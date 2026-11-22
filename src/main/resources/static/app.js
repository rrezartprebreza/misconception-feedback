/* Shared helpers for the two pages. No dependencies. */
function attachEditor(textarea, gutter) {
  function update() {
    var lines = textarea.value.split('\n').length;
    var text = '';
    for (var i = 1; i <= lines; i++) { text += i + (i < lines ? '\n' : ''); }
    gutter.textContent = text;
    gutter.scrollTop = textarea.scrollTop;
  }
  textarea.addEventListener('input', update);
  textarea.addEventListener('scroll', function () { gutter.scrollTop = textarea.scrollTop; });
  update();
}

/* Language selection. The page's language comes from ?lang=, else the server's default. */
var pageLanguage = null;      // {id, name} once known
var languageOptions = [];

function languageParam() {
  var m = /[?&]lang=([A-Za-z]+)/.exec(window.location.search);
  return m ? m[1].toLowerCase() : null;
}

function withLanguage(path) {
  if (!pageLanguage) { return path; }
  return path + (path.indexOf('?') >= 0 ? '&' : '?') + 'language=' + encodeURIComponent(pageLanguage.id);
}

var FILE_NAMES = { java: 'Answer.java', python: 'answer.py', csharp: 'Answer.cs' };

function setLanguageLabels() {
  var label = document.getElementById('codeLabel');
  if (label && pageLanguage) { label.textContent = 'Your ' + pageLanguage.name + ' code'; }
  var file = document.getElementById('fileName');
  if (file && pageLanguage) { file.textContent = FILE_NAMES[pageLanguage.id] || 'answer'; }
  document.documentElement.setAttribute('data-language', pageLanguage ? pageLanguage.id : '');
}

/* A three-way switch (Java | Python | C#). Arrow keys move between the options. */
function buildSwitcher(onPick) {
  var group = document.createElement('div');
  group.id = 'language';
  group.className = 'langswitch';
  group.setAttribute('role', 'radiogroup');
  group.setAttribute('aria-label', 'Programming language');
  function paint() {
    Array.prototype.forEach.call(group.children, function (b) {
      var on = b.getAttribute('data-id') === pageLanguage.id;
      b.setAttribute('aria-checked', String(on));
      b.tabIndex = on ? 0 : -1;
    });
  }
  languageOptions.forEach(function (l, i) {
    var b = document.createElement('button');
    b.type = 'button';
    b.setAttribute('role', 'radio');
    b.setAttribute('data-id', l.id);
    b.textContent = l.name;
    b.addEventListener('click', function () {
      if (l.id === pageLanguage.id) { return; }
      pageLanguage = l;
      paint();
      onPick(l);
    });
    b.addEventListener('keydown', function (e) {
      var step = e.key === 'ArrowRight' || e.key === 'ArrowDown' ? 1 : e.key === 'ArrowLeft' || e.key === 'ArrowUp' ? -1 : 0;
      if (!step) { return; }
      e.preventDefault();
      var next = group.children[(i + step + languageOptions.length) % languageOptions.length];
      next.focus();
      next.click();
    });
    group.appendChild(b);
  });
  paint();
  return group;
}

/*
 * Reads /api/config, picks the language and shows it in #language. With switcher = true the chip becomes
 * a three-way switch; onChange(language) runs after the person picks another one (not on the first call).
 */
function applyLanguage(switcher, onChange) {
  return fetch('/api/config')
    .then(function (r) { return r.ok ? r.json() : null; })
    .then(function (c) {
      if (!c) { return null; }
      languageOptions = c.languages || [];
      var wanted = languageParam() || c.languageId;
      var found = languageOptions.filter(function (l) { return l.id === wanted; })[0];
      if (!found) { found = languageOptions.filter(function (l) { return l.id === c.languageId; })[0]; }
      pageLanguage = found || { id: c.languageId, name: c.languageName };
      var chip = document.getElementById('language');
      if (chip && switcher && languageOptions.length > 1) {
        chip.parentNode.replaceChild(buildSwitcher(function (l) {
          if (window.history && history.replaceState) { history.replaceState(null, '', '?lang=' + l.id); }
          setLanguageLabels();
          if (onChange) { onChange(l); }
        }), chip);
      } else if (chip) {
        chip.textContent = pageLanguage.name;
      }
      setLanguageLabels();
      return pageLanguage;
    })
    .catch(function () { return null; });
}

function friendlyError(status) {
  if (status === 400) { return 'The request was not accepted. Check that the code box is not empty and try again.'; }
  return 'The feedback model did not answer. If you run this yourself, check that Ollama is running, then try again.';
}
