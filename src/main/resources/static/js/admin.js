/**
 * 后台通用脚本:登录 / 登出 / 删除 / 保存 / 图片上传
 */

function showMsg(text, isErr) {
    const el = document.getElementById('saveMsg');
    if (!el) return;
    el.textContent = text;
    el.className = 'save-msg ' + (isErr ? 'err' : 'ok');
}

async function request(url, options) {
    const resp = await fetch(url, options);
    if (resp.status === 401) {
        location.href = '/admin/login';
        return null;
    }
    return resp;
}

/* ---------- 登录页 ---------- */
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const errEl = document.getElementById('loginErr');
        errEl.textContent = '';
        const resp = await fetch('/admin/api/login', {
            method: 'POST',
            body: new FormData(loginForm)
        });
        const data = await resp.json();
        if (resp.ok && data.ok) {
            location.href = '/admin';
        } else {
            errEl.textContent = data.message || '登录失败';
        }
    });
}

/* ---------- 仪表盘:登出 / 删除 ---------- */
const logoutBtn = document.getElementById('logoutBtn');
if (logoutBtn) {
    logoutBtn.addEventListener('click', async (e) => {
        e.preventDefault();
        await request('/admin/api/logout', { method: 'POST' });
        location.href = '/admin/login';
    });
}

document.querySelectorAll('.js-delete').forEach(btn => {
    btn.addEventListener('click', async (e) => {
        e.preventDefault();
        const { id, title } = btn.dataset;
        if (!confirm(`确定删除「${title}」吗?此操作不可恢复。`)) return;
        const resp = await request('/admin/api/projects/' + id, { method: 'DELETE' });
        if (resp) location.reload();
    });
});

/* ---------- 编辑页:上传 / 保存 ---------- */
const saveBtn = document.getElementById('saveBtn');
if (saveBtn) {
    const fileInput = document.getElementById('f-file');
    const coverInput = document.getElementById('f-cover');
    const preview = document.getElementById('coverPreview');

    // 选图后立即上传
    fileInput.addEventListener('change', async () => {
        const file = fileInput.files[0];
        if (!file) return;
        showMsg('上传中…');
        const fd = new FormData();
        fd.append('file', file);
        const resp = await request('/admin/api/upload', { method: 'POST', body: fd });
        if (!resp) return;
        const data = await resp.json();
        if (resp.ok && data.ok) {
            coverInput.value = data.url;
            preview.src = data.url;
            preview.style.display = 'block';
            showMsg('封面上传成功');
        } else {
            showMsg(data.message || '上传失败', true);
        }
    });

    // 保存
    saveBtn.addEventListener('click', async () => {
        const id = saveBtn.dataset.id || null;
        const payload = {
            title: document.getElementById('f-title').value.trim(),
            summary: document.getElementById('f-summary').value.trim(),
            tags: document.getElementById('f-tags').value.trim(),
            coverUrl: coverInput.value.trim(),
            content: document.getElementById('f-content').value,
            demoUrl: document.getElementById('f-demo').value.trim(),
            repoUrl: document.getElementById('f-repo').value.trim(),
            published: document.getElementById('f-published').checked,
            sortWeight: parseInt(document.getElementById('f-sort').value || '0', 10) || 0
        };
        if (!payload.title) {
            showMsg('标题不能为空', true);
            return;
        }
        saveBtn.disabled = true;
        showMsg('保存中…');
        const resp = await request(
            id ? '/admin/api/projects/' + id : '/admin/api/projects',
            {
                method: id ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            }
        );
        saveBtn.disabled = false;
        if (!resp) return;
        const data = await resp.json();
        if (resp.ok && data.ok) {
            location.href = '/admin';
        } else {
            showMsg(data.message || '保存失败', true);
        }
    });
}

/* ---------- 编辑页:体验包 / 源码包 zip 上传 ---------- */
function bindZipAsset(kind) {
    const input = document.getElementById(kind === 'demo' ? 'f-demo-zip' : 'f-code-zip');
    if (!input) return;
    const delBtn = document.getElementById(kind === 'demo' ? 'delDemoBtn' : 'delCodeBtn');
    const id = saveBtn ? saveBtn.dataset.id : null;

    input.addEventListener('change', async () => {
        const file = input.files[0];
        if (!file) return;
        if (!id) {
            showMsg('请先保存项目,再上传体验包', true);
            input.value = '';
            return;
        }
        showMsg('上传解压中,大文件请稍候…');
        const fd = new FormData();
        fd.append('file', file);
        const resp = await request('/admin/api/projects/' + id + '/' + kind, { method: 'POST', body: fd });
        if (!resp) return;
        const data = await resp.json().catch(() => ({}));
        if (resp.ok && data.ok) {
            showMsg(kind === 'demo' ? '体验包上传成功' : '源码包上传成功');
            setTimeout(() => location.reload(), 600);
        } else {
            showMsg(data.message || '上传失败', true);
        }
        input.value = '';
    });

    if (delBtn) {
        delBtn.addEventListener('click', async () => {
            if (!confirm('确定删除该' + (kind === 'demo' ? '体验包' : '源码包') + '吗?')) return;
            const resp = await request('/admin/api/projects/' + id + '/' + kind, { method: 'DELETE' });
            if (resp) location.reload();
        });
    }
}
bindZipAsset('demo');
bindZipAsset('code');
