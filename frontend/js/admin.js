// 管理后台逻辑
// formatDate / escapeHtml 由 api.js 提供，不再在此重复定义
document.addEventListener('DOMContentLoaded', () => {
    if (!isLoggedIn()) { window.location.href = 'login.html?return=admin.html'; return; }
    updateNavAuth();
    loadMyArticles();
    loadCategories();
    loadTags();

    document.getElementById('newArticleBtn')?.addEventListener('click', newArticle);
});

async function loadMyArticles() {
    const container = document.getElementById('adminArticleList');
    try {
        const data = await api.getMyArticles(0, 50);
        container.innerHTML = '';

        if (data.content.length === 0) {
            container.innerHTML = '<div class="loading">暂无文章，点击上方按钮开始写作</div>';
            return;
        }

        data.content.forEach(article => {
            const row = document.createElement('div');
            row.className = 'admin-article-row';
            row.innerHTML = `
                <div class="info">
                    <div class="title">${escapeHtml(article.title)}</div>
                    <div style="font-size:0.85rem;color:var(--text-secondary)">
                        ${formatDate(article.createdAt)} · ${article.viewCount} 阅读 · ${article.commentCount} 评论
                        ${article.categoryName ? ' · ' + escapeHtml(article.categoryName) : ''}
                    </div>
                </div>
                <span class="status status-${escapeHtml(article.status)}">${article.status === 'PUBLISHED' ? '已发布' : '草稿'}</span>
                <div class="actions">
                    <button class="btn-edit">编辑</button>
                    <button class="btn-danger">删除</button>
                </div>
            `;
            row.querySelector('.btn-edit').addEventListener('click', () => editArticle(article.id));
            row.querySelector('.btn-danger').addEventListener('click', () => deleteArticleConfirm(article.id));
            container.appendChild(row);
        });
    } catch (e) {
        container.innerHTML = `<div class="loading">加载失败: ${escapeHtml(e.message)}</div>`;
    }
}

async function loadCategories() {
    try {
        const cats = await api.getCategories();
        const select = document.getElementById('articleCategory');
        select.innerHTML = '';
        // createElement + textContent，原先是 innerHTML 拼接分类名
        select.appendChild(new Option('无分类', ''));
        cats.forEach(c => select.appendChild(new Option(c.name, c.id)));
    } catch (e) {
        console.error('加载分类失败', e);
    }
}

async function loadTags() {
    try {
        const tags = await api.getTags();
        const container = document.getElementById('tagCheckboxes');
        container.innerHTML = '';
        tags.forEach(t => {
            const label = document.createElement('label');
            const cb = document.createElement('input');
            cb.type = 'checkbox';
            cb.value = t.id;
            label.appendChild(cb);
            // textContent 而非 innerHTML：标签名不再有注入风险
            label.appendChild(document.createTextNode(' ' + t.name));
            container.appendChild(label);
        });
    } catch (e) {
        console.error('加载标签失败', e);
    }
}

function newArticle() {
    document.getElementById('editorTitle').textContent = '写文章';
    document.getElementById('articleId').value = '';
    document.getElementById('articleTitle').value = '';
    document.getElementById('articleCategory').value = '';
    document.querySelectorAll('#tagCheckboxes input[type=checkbox]').forEach(cb => cb.checked = false);
    document.getElementById('articleSummary').value = '';
    document.getElementById('articleContent').value = '';
    document.getElementById('articleStatus').value = 'DRAFT';
    document.getElementById('editorError').style.display = 'none';
    document.getElementById('editorModal').style.display = 'flex';
}

async function editArticle(id) {
    try {
        const article = await api.getArticle(id);
        document.getElementById('editorTitle').textContent = '编辑文章';
        document.getElementById('articleId').value = article.id;
        document.getElementById('articleTitle').value = article.title;
        document.getElementById('articleCategory').value = article.categoryId || '';
        // 按 tagIds 匹配。原先拿 checkbox 的 nextSibling 文本去比标签名，
        // 既依赖 DOM 结构又会被重名/空格影响。
        const tagIds = (article.tagIds || []).map(String);
        document.querySelectorAll('#tagCheckboxes input[type=checkbox]').forEach(cb => {
            cb.checked = tagIds.includes(cb.value);
        });
        document.getElementById('articleSummary').value = article.summary || '';
        document.getElementById('articleContent').value = article.content || '';
        document.getElementById('articleStatus').value = article.status;
        document.getElementById('editorError').style.display = 'none';
        document.getElementById('editorModal').style.display = 'flex';
    } catch (e) {
        alert('加载文章失败: ' + e.message);
    }
}

function closeEditor() {
    document.getElementById('editorModal').style.display = 'none';
}

async function saveArticle(e) {
    e.preventDefault();
    const errorDiv = document.getElementById('editorError');
    errorDiv.style.display = 'none';

    const articleId = document.getElementById('articleId').value;
    const tagIds = Array.from(document.querySelectorAll('#tagCheckboxes input[type=checkbox]:checked'))
        .map(cb => parseInt(cb.value));

    const data = {
        title: document.getElementById('articleTitle').value.trim(),
        summary: document.getElementById('articleSummary').value.trim() || null,
        content: document.getElementById('articleContent').value.trim(),
        status: document.getElementById('articleStatus').value,
        categoryId: document.getElementById('articleCategory').value
            ? parseInt(document.getElementById('articleCategory').value)
            : null,
        tagIds: tagIds
    };

    try {
        if (articleId) {
            await api.updateArticle(parseInt(articleId), data);
        } else {
            await api.createArticle(data);
        }
        closeEditor();
        loadMyArticles();
    } catch (err) {
        errorDiv.textContent = err.message;
        errorDiv.style.display = 'block';
    }
}

async function deleteArticleConfirm(id) {
    if (!confirm('确定要删除这篇文章吗？此操作不可撤销。')) return;
    try {
        await api.deleteArticle(id);
        loadMyArticles();
    } catch (e) {
        alert('删除失败: ' + e.message);
    }
}
