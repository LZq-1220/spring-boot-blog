// 文章详情与评论页面逻辑
// formatDate / escapeHtml 由 api.js 提供
const urlParams = new URLSearchParams(window.location.search);
const articleId = urlParams.get('id');

document.addEventListener('DOMContentLoaded', () => {
    updateNavAuth();
    if (!articleId) { window.location.href = 'index.html'; return; }
    loadArticle();
    loadComments();
    initCommentForm();

    document.getElementById('submitCommentBtn')?.addEventListener('click', submitComment);
    document.getElementById('cancelReplyBtn')?.addEventListener('click', cancelReply);
});

async function loadArticle() {
    const container = document.getElementById('articleContent');
    try {
        const article = await api.getArticle(articleId);
        container.innerHTML = `
            <div class="article-detail">
                <h1>${escapeHtml(article.title)}</h1>
                <div class="meta">
                    <span>👤 ${escapeHtml(article.authorNickname)}</span>
                    <span>📅 ${formatDate(article.createdAt)}</span>
                    <span>👁 ${article.viewCount} 次阅读</span>
                    ${article.categoryName ? `<span>📂 ${escapeHtml(article.categoryName)}</span>` : ''}
                    ${article.tags && article.tags.length ? `<span>🏷 ${article.tags.map(t => escapeHtml(t)).join(', ')}</span>` : ''}
                </div>
                <div class="content">${simpleMarkdown(article.content)}</div>
            </div>
        `;
        document.title = `${article.title} - 个人博客`;
    } catch (e) {
        container.innerHTML = `<div class="loading">加载失败: ${escapeHtml(e.message)}</div>`;
    }
}

async function loadComments() {
    const container = document.getElementById('commentList');
    try {
        const comments = await api.getComments(articleId);
        // 计入回复数，原先只数根评论，和列表页显示的总数不一致
        const total = comments.reduce((sum, c) => sum + 1 + (c.replies ? c.replies.length : 0), 0);
        document.getElementById('commentCount').textContent = total;
        container.innerHTML = '';
        comments.forEach(c => renderComment(c, container));
    } catch (e) {
        container.innerHTML = `<div class="loading">评论加载失败: ${escapeHtml(e.message)}</div>`;
    }
}

function renderComment(comment, container, isReply = false) {
    const div = document.createElement('div');
    div.className = isReply ? 'reply' : 'comment';
    div.innerHTML = `
        <div class="comment-header">
            <span class="comment-author">${escapeHtml(comment.nickname)}</span>
            <span class="comment-time">${formatDate(comment.createdAt)}</span>
        </div>
        <div class="comment-body">${escapeHtml(comment.content)}</div>
        <span class="comment-reply">回复</span>
    `;

    // 原先是 onclick="replyTo(${id}, '${nickname}', event)"：
    // escapeHtml 不转义单引号，昵称含 ' 就语法错误（按钮失效），
    // 更糟的是可以借此执行任意脚本。改成闭包 + addEventListener，不再拼字符串。
    const replyBtn = div.querySelector('.comment-reply');
    replyBtn.addEventListener('click', (event) => replyTo(comment.id, comment.nickname, event));

    container.appendChild(div);

    if (comment.replies && comment.replies.length > 0) {
        comment.replies.forEach(r => renderComment(r, container, true));
    }
}

function replyTo(commentId, nickname, event) {
    event.stopPropagation();
    if (!isLoggedIn()) {
        window.location.href = `login.html?return=post.html%3Fid%3D${encodeURIComponent(articleId)}`;
        return;
    }
    document.getElementById('replyParentId').value = commentId;
    const hint = document.getElementById('replyHint');
    hint.textContent = `回复 @${nickname}`;   // textContent，不走 HTML 解析
    hint.style.display = 'inline';
    document.getElementById('cancelReplyBtn').style.display = 'inline';
    document.getElementById('commentInput').focus();
}

function cancelReply() {
    document.getElementById('replyParentId').value = '';
    document.getElementById('replyHint').style.display = 'none';
    document.getElementById('cancelReplyBtn').style.display = 'none';
}

function initCommentForm() {
    const formContainer = document.getElementById('commentFormContainer');
    formContainer.style.display = 'block';
    if (!isLoggedIn()) {
        document.querySelector('.comment-form').style.display = 'none';
        document.getElementById('loginPrompt').style.display = 'block';
    }
}

async function submitComment() {
    const input = document.getElementById('commentInput');
    const content = input.value.trim();
    if (!content) return;

    const parentId = document.getElementById('replyParentId').value || null;
    try {
        await api.addComment(articleId, content, parentId ? parseInt(parentId) : null);
        input.value = '';
        cancelReply();
        loadComments();
    } catch (e) {
        alert('评论失败: ' + e.message);
    }
}

// Simple Markdown to HTML converter for blog content
function simpleMarkdown(text) {
    if (!text) return '';
    return text
        .split('\n\n')
        .map(block => {
            block = block.trim();
            if (!block) return '';
            if (block.startsWith('### ')) return `<h3>${escapeHtml(block.slice(4))}</h3>`;
            if (block.startsWith('## ')) return `<h2>${escapeHtml(block.slice(3))}</h2>`;
            if (block.startsWith('# ')) return `<h2>${escapeHtml(block.slice(2))}</h2>`;
            if (block.startsWith('```')) {
                const code = block.replace(/^```\w*\n?/, '').replace(/```$/, '');
                return `<pre><code>${escapeHtml(code.trim())}</code></pre>`;
            }
            return `<p>${escapeHtml(block)}</p>`;
        })
        .join('\n');
}
