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

function setLanguageLabels() {
  var label = document.getElementById('codeLabel');
  if (label && pageLanguage) { label.textContent = 'Your ' + pageLanguage.name + ' code'; }
  document.documentElement.setAttribute('data-language', pageLanguage ? pageLanguage.id : '');
}

/*
 * Reads /api/config, picks the language and shows it in #language. With switcher = true the chip becomes
 * a drop-down; onChange(language) runs after the person picks another one (not on the first call).
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
        var select = document.createElement('select');
        select.id = 'language';
        select.className = 'chip';
        select.setAttribute('aria-label', 'Programming language');
        languageOptions.forEach(function (l) {
          var o = document.createElement('option');
          o.value = l.id;
          o.textContent = l.name;
          if (l.id === pageLanguage.id) { o.selected = true; }
          select.appendChild(o);
        });
        select.addEventListener('change', function () {
          pageLanguage = languageOptions.filter(function (l) { return l.id === select.value; })[0];
          if (window.history && history.replaceState) {
            history.replaceState(null, '', '?lang=' + pageLanguage.id);
          }
          setLanguageLabels();
          if (onChange) { onChange(pageLanguage); }
        });
        chip.parentNode.replaceChild(select, chip);
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
