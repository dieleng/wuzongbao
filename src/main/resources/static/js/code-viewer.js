/**
 * 项目详情页源码浏览器:左侧文件树 + 右侧代码预览(带语法高亮)
 */
(function () {
    var box = document.getElementById('codeViewer');
    if (!box) return;
    var pid = box.dataset.id;
    var treeEl = document.getElementById('codeTree');
    var pathEl = document.getElementById('codePath');
    var contentEl = document.getElementById('codeContent');

    function escapeHtml(s) {
        return String(s).replace(/[&<>"']/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
        });
    }

    // 递归渲染文件树,目录用 details/summary 折叠
    function render(nodes, container) {
        var ul = document.createElement('ul');
        nodes.forEach(function (n) {
            var li = document.createElement('li');
            if (n.dir) {
                li.className = 'c-dir';
                li.innerHTML = '<details open><summary>' + escapeHtml(n.name) + '</summary></details>';
                render(n.children || [], li.querySelector('details'));
            } else {
                li.className = 'c-file';
                li.innerHTML = '<a href="javascript:void(0)" data-path="' + escapeHtml(n.path) + '">'
                    + escapeHtml(n.name) + '</a>';
            }
            ul.appendChild(li);
        });
        container.appendChild(ul);
    }

    function openFile(path) {
        pathEl.textContent = path;
        contentEl.textContent = '加载中…';
        contentEl.className = '';

        treeEl.querySelectorAll('a.active').forEach(function (a) { a.classList.remove('active'); });
        var links = treeEl.querySelectorAll('a[data-path]');
        for (var i = 0; i < links.length; i++) {
            if (links[i].dataset.path === path) { links[i].classList.add('active'); break; }
        }

        fetch('/api/projects/' + pid + '/code/file?path=' + encodeURIComponent(path))
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (!data.ok) {
                    contentEl.textContent = data.message || '加载失败';
                    return;
                }
                contentEl.removeAttribute('data-highlighted');
                contentEl.textContent = data.content;
                if (window.hljs) {
                    try { hljs.highlightElement(contentEl); } catch (e) { /* 高亮失败则显示纯文本 */ }
                }
            })
            .catch(function () { contentEl.textContent = '加载失败'; });
    }

    fetch('/api/projects/' + pid + '/code')
        .then(function (r) { return r.json(); })
        .then(function (nodes) {
            if (!Array.isArray(nodes) || nodes.length === 0) {
                treeEl.textContent = '没有源码文件';
                return;
            }
            treeEl.innerHTML = '';
            render(nodes, treeEl);
            treeEl.addEventListener('click', function (e) {
                var a = e.target.closest('a[data-path]');
                if (a) openFile(a.dataset.path);
            });
            // 默认展开第一个文件
            var first = treeEl.querySelector('a[data-path]');
            if (first) openFile(first.dataset.path);
        })
        .catch(function () { treeEl.textContent = '加载失败'; });
})();
