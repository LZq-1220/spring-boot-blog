// 登录/注册页面逻辑

function switchTab(tab) {
    document.querySelectorAll('.auth-tab').forEach(t => t.classList.remove('active'));
    document.getElementById('loginForm').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('registerForm').style.display = tab === 'register' ? 'block' : 'none';
    document.querySelector(`.auth-tab:nth-child(${tab === 'login' ? 1 : 2})`).classList.add('active');
}

async function handleLogin(e) {
    e.preventDefault();
    const errorDiv = document.getElementById('loginError');
    errorDiv.style.display = 'none';

    try {
        const res = await api.login({
            username: document.getElementById('loginUsername').value,
            password: document.getElementById('loginPassword').value
        });
        localStorage.setItem('token', res.token);
        localStorage.setItem('user', JSON.stringify({ username: res.username, nickname: res.nickname, role: res.role }));
        const returnUrl = new URLSearchParams(window.location.search).get('return') || 'index.html';
        window.location.href = returnUrl;
    } catch (e) {
        errorDiv.textContent = e.message;
        errorDiv.style.display = 'block';
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const errorDiv = document.getElementById('registerError');
    errorDiv.style.display = 'none';

    try {
        const res = await api.register({
            username: document.getElementById('regUsername').value,
            email: document.getElementById('regEmail').value,
            nickname: document.getElementById('regNickname').value || undefined,
            password: document.getElementById('regPassword').value
        });
        localStorage.setItem('token', res.token);
        localStorage.setItem('user', JSON.stringify({ username: res.username, nickname: res.nickname, role: res.role }));
        window.location.href = 'index.html';
    } catch (e) {
        errorDiv.textContent = e.message;
        errorDiv.style.display = 'block';
    }
}
