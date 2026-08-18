// API 基础配置
// 开发环境：http://localhost:8080/api
// 生产环境：将下面的 URL 替换为 Railway 生成的域名
const API_BASE = window.location.hostname === 'localhost'
    ? 'http://localhost:8080/api'
    : `${window.location.protocol}//${window.location.host}/api`;

// ===== 公共工具 =====
// 这两个函数原先只定义在 main.js / admin.js 里，
// 而 post.html 并未引入它们 -> 详情页直接 ReferenceError。
// 统一放到 api.js（所有页面都引了这个文件）。

function formatDate(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return '';
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function escapeHtml(text) {
    if (text === null || text === undefined) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// ===== 通用请求方法 =====
async function request(url, options = {}) {
    const token = localStorage.getItem('token');
    const headers = {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
        ...options.headers
    };

    const res = await fetch(`${API_BASE}${url}`, { ...options, headers });

    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        // token 过期/未登录：清掉本地凭证并跳登录页。
        // 登录、注册接口本身的 401 是「密码错误」，不能跳转，要把消息透出去。
        const isAuthEndpoint = url.startsWith('/auth/');
        if (res.status === 401 && !isAuthEndpoint) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            const back = encodeURIComponent(location.pathname.split('/').pop() + location.search);
            location.href = `login.html?return=${back}`;
        }
        throw new Error(err.message || `HTTP ${res.status}`);
    }

    if (res.status === 204) return null;
    return res.json();
}

const api = {
    // Auth
    login: (data) => request('/auth/login', { method: 'POST', body: JSON.stringify(data) }),
    register: (data) => request('/auth/register', { method: 'POST', body: JSON.stringify(data) }),

    // Articles
    getArticles: (page = 0, size = 10, categoryId, tagId) => {
        let url = `/articles?page=${page}&size=${size}`;
        if (categoryId) url += `&categoryId=${categoryId}`;
        if (tagId) url += `&tagId=${tagId}`;
        return request(url);
    },
    getArticle: (id) => request(`/articles/${id}`),
    createArticle: (data) => request('/articles', { method: 'POST', body: JSON.stringify(data) }),
    updateArticle: (id, data) => request(`/articles/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    deleteArticle: (id) => request(`/articles/${id}`, { method: 'DELETE' }),
    // 不带 status：草稿和已发布都要，后台才能编辑草稿
    getMyArticles: (page = 0, size = 20) => request(`/articles/my?page=${page}&size=${size}`),

    // Comments
    getComments: (articleId) => request(`/articles/${articleId}/comments`),
    addComment: (articleId, content, parentId) => {
        const body = { content };
        if (parentId) body.parentId = parentId;
        return request(`/articles/${articleId}/comments`, { method: 'POST', body: JSON.stringify(body) });
    },
    deleteComment: (articleId, commentId) => request(`/articles/${articleId}/comments/${commentId}`, { method: 'DELETE' }),

    // Metadata
    getCategories: () => request('/categories'),
    getTags: () => request('/tags')
};

// ===== Auth helpers =====
function isLoggedIn() { return !!localStorage.getItem('token'); }

function getCurrentUser() {
    try {
        return JSON.parse(localStorage.getItem('user') || 'null');
    } catch (e) {
        return null;
    }
}

function updateNavAuth() {
    const authLink = document.getElementById('authLink');
    const userDisplay = document.getElementById('userDisplay');
    const logoutLink = document.getElementById('logoutLink');

    // 逐个判空：admin.html 只有 userDisplay，
    // 原先要求三个元素同时存在，否则整个函数 early return，用户名一直空着。
    if (isLoggedIn()) {
        const user = getCurrentUser();
        if (authLink) authLink.style.display = 'none';
        if (userDisplay) {
            userDisplay.style.display = 'inline';
            userDisplay.textContent = user?.nickname || user?.username || '';
        }
        if (logoutLink) logoutLink.style.display = 'inline';
    } else {
        if (authLink) authLink.style.display = 'inline';
        if (userDisplay) userDisplay.style.display = 'none';
        if (logoutLink) logoutLink.style.display = 'none';
    }
}

function handleAuthClick() {
    window.location.href = 'login.html';
}

function goToAdmin() {
    if (!isLoggedIn()) {
        window.location.href = 'login.html?return=admin.html';
        return;
    }
    window.location.href = 'admin.html';
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = 'index.html';
}
