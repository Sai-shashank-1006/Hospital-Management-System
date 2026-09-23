/*
 * The application's only JavaScript.
 *
 * Everything works without it: forms submit, pages render, data saves. This file
 * adds two conveniences on top, and is written as progressive enhancement so a
 * blocked or failed script never costs the user a feature they cannot reach.
 *
 * Behaviour is attached from data attributes rather than inline handlers, because
 * the Content-Security-Policy sets script-src 'self' with no 'unsafe-inline' --
 * an inline onclick would simply not run.
 */
(function () {
  'use strict';

  /* Confirmation before a destructive submit. Without this the form still
     submits; the server is what actually authorises the action. */
  function wireConfirms(root) {
    root.querySelectorAll('form[data-confirm]').forEach(function (form) {
      if (form.dataset.confirmWired) {
        return;
      }
      form.dataset.confirmWired = '1';
      form.addEventListener('submit', function (event) {
        if (!window.confirm(form.getAttribute('data-confirm'))) {
          event.preventDefault();
        }
      });
    });
  }

  /* Repeating rows on the prescription and invoice forms.
     The server already renders spare blank rows, so adding more is a
     convenience for long prescriptions, not a requirement. */
  function wireRowAdders(root) {
    root.querySelectorAll('[data-add-row]').forEach(function (button) {
      if (button.dataset.addRowWired) {
        return;
      }
      button.dataset.addRowWired = '1';

      button.addEventListener('click', function () {
        var body = document.getElementById(button.getAttribute('data-add-row'));
        if (!body) {
          return;
        }

        var rows = body.querySelectorAll('tr');
        if (rows.length === 0) {
          return;
        }

        var clone = rows[rows.length - 1].cloneNode(true);
        clone.querySelectorAll('input, textarea').forEach(function (field) {
          field.value = '';
        });
        body.appendChild(clone);
      });
    });
  }

  function init() {
    wireConfirms(document);
    wireRowAdders(document);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
