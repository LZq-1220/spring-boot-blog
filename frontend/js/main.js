// 首页逻辑
// formatDate / escapeHtml 由 api.js 提供，不再在此重复定义
let currentPage = 0, totalPages = 0;
let currentFilter = { type: null, id: null };
let categories = [], tags = [];

document.addEventListener('DOMContentLoaded', () => {
    updateNavAuth();
    loadCategories();
    loadTags();
    loadArticles();

    // 「全部」链接原本用内联 onclick，这里统一改成事件监听
    const allLink = document.getElementById('allCategoriesLink');
    if (allLink) {
        allLink.addEventListener('click', () => filterArticles('category', null));
    }
});

async function loadCategories() {
    try {
        categories = await api.getCategories();
        const list = document.getElementById('categoryList');
        categories.forEach(c => {
            const li = document.createElement('li');
            const a = document.createElement('a');
            a.href = 'javascript:void(0)';
            // textContent 而非 innerHTML 拼接：分类名不再有注入风险
            a.textContent = c.name;
            a.dataset.categoryId = c.id;
            a.addEventListener('click', () => filterArticles('category', c.id));
            li.appendChild(a);
            list.appendChild(li);
        });
    } catch (e) { console.error('加载分类失败', e); }
}

async function loadTags() {
    try {
        tags = await api.getTags();
        const container = document.getElementById('tagList');
        tags.forEach(t => {
            const span = document.createElement('span');
            span.className = 'tag-pill';
            span.textContent = t.name;
            span.dataset.tagId = t.id;
            span.addEventListener('click', () => filterArticles('tag', t.id));
            container.appendChild(span);
        });
    } catch (e) { console.error('加载标签失败', e); }
}

async function loadArticles(page = 0) {
    currentPage = page;
    const list = document.getElementById('articleList');
    list.innerHTML = '<div class="loading">加载中...</div>';

    try {
        const data = await api.getArticles(
            page, 10,
            currentFilter.type === 'category' ? currentFilter.id : null,
            currentFilter.type === 'tag' ? currentFilter.id : null
        );

        totalPages = data.totalPages;
        list.innerHTML = '';

        if (data.content.length === 0) {
            list.innerHTML = '<div class="loading">暂无文章</div>';
        }

        data.content.forEach(article => {
            const card = document.createElement('div');
            card.className = 'article-card';
            card.onclick = () => window.location.href = `post.html?id=${article.id}`;
            card.innerHTML = `
                <h2>${escapeHtml(article.title)}</h2>
                ${article.summary ? `<p class="summary">${escapeHtml(article.summary)}</p>` : ''}
                <div class="meta">
                    <span>👤 ${escapeHtml(article.authorNickname)}</span>
                    <span>📅 ${formatDate(article.createdAt)}</span>
                    <span>👁 ${article.viewCount}</span>
                    <span>💬 ${article.commentCount}</span>
                    ${article.categoryName ? `<span>📂 ${escapeHtml(article.categoryName)}</span>` : ''}
                </div>
            `;
            list.appendChild(card);
        });

        renderPagination();
    } catch (e) {
        list.innerHTML = `<div class="loading">加载失败: ${escapeHtml(e.message)}</div>`;
    }
}

function filterArticles(type, id) {
    currentFilter = { type, id };
    currentPage = 0;
    loadArticles(0);
    updateFilterHighlight(type, id);
}

/** 高亮改为按 dataset 匹配，不再去解析 onclick 属性字符串 */
function updateFilterHighlight(type, id) {
    document.querySelectorAll('.category-list a').forEach(a => a.classList.remove('active'));
    document.querySelectorAll('.tag-pill').forEach(t => t.classList.remove('active'));

    if (type === 'category' && id === null) {
        document.getElementById('allCategoriesLink')?.classList.add('active');
    } else if (type === 'category') {
        document.querySelector(`.category-list a[data-category-id="${id}"]`)?.classList.add('active');
    } else if (type === 'tag') {
        document.querySelector(`.tag-pill[data-tag-id="${id}"]`)?.classList.add('active');
    }
}

/** 只渲染当前页附近的按钮，原先按总页数全量渲染，页多了会铺满屏 */
function renderPagination() {
    const container = document.getElementById('pagination');
    container.innerHTML = '';
    if (totalPages <= 1) return;

    const addButton = (label, targetPage, opts = {}) => {
        const btn = document.createElement('button');
        btn.textContent = label;
        if (opts.disabled) btn.disabled = true;
        if (opts.active) btn.className = 'active';
        if (!opts.disabled && targetPage !== null) {
            btn.addEventListener('click', () => loadArticles(targetPage));
        }
        container.appendChild(btn);
    };

    addButton('上一页', currentPage - 1, { disabled: currentPage === 0 });

    const WINDOW = 2;
    let start = Math.max(0, currentPage - WINDOW);
    let end = Math.min(totalPages - 1, currentPage + WINDOW);

    if (start > 0) {
        addButton('1', 0);
        if (start > 1) addButton('...', null, { disabled: true });
    }

    for (let i = start; i <= end; i++) {
        addButton(String(i + 1), i, { active: i === currentPage });
    }

    if (end < totalPages - 1) {
        if (end < totalPages - 2) addButton('...', null, { disabled: true });
        addButton(String(totalPages), totalPages - 1);
    }

    addButton('下一页', currentPage + 1, { disabled: currentPage >= totalPages - 1 });
}
