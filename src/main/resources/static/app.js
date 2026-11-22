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

function applyLanguage() {
  return fetch('/api/config')
    .then(function (r) { return r.ok ? r.json() : null; })
    .then(function (c) {
      if (!c) { return null; }
      var chip = document.getElementById('language');
      if (chip) { chip.textContent = c.languageName; }
      var label = document.getElementById('codeLabel');
      if (label) { label.textContent = 'Your ' + c.languageName + ' code'; }
      return c;
    })
    .catch(function () { return null; });
}

function friendlyError(status) {
  if (status === 400) { return 'The request was not accepted. Check that the code box is not empty and try again.'; }
  return 'The feedback model did not answer. If you run this yourself, check that Ollama is running, then try again.';
}
