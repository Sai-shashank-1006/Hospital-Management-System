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

  /* Navigation dropdowns.
     CSS already opens these on :hover and :focus-within, so they work with this
     script blocked. What JS adds is a click/tap toggle, Escape to close, and
     closing when focus or the pointer leaves -- and it keeps aria-expanded
     truthful, which CSS alone cannot do. */
  function wireDropdowns(root) {
    var items = root.querySelectorAll('.nav-item');

    function closeAll(except) {
      items.forEach(function (item) {
        if (item === except) {
          return;
        }
        item.classList.remove('open');
        var toggle = item.querySelector('.nav-toggle');
        if (toggle) {
          toggle.setAttribute('aria-expanded', 'false');
        }
      });
    }

    items.forEach(function (item) {
      var toggle = item.querySelector('.nav-toggle');
      if (!toggle || toggle.dataset.ddWired) {
        return;
      }
      toggle.dataset.ddWired = '1';

      toggle.addEventListener('click', function (event) {
        event.preventDefault();
        var willOpen = !item.classList.contains('open');
        closeAll(item);
        item.classList.toggle('open', willOpen);
        toggle.setAttribute('aria-expanded', willOpen ? 'true' : 'false');
      });
    });

    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') {
        closeAll(null);
      }
    });

    document.addEventListener('click', function (event) {
      if (!event.target.closest('.nav-item')) {
        closeAll(null);
      }
    });

    // Leaving the bar entirely closes whatever was pinned open by a click.
    var shell = root.querySelector('.navshell');
    if (shell) {
      shell.addEventListener('mouseleave', function () {
        closeAll(null);
      });
    }
  }

  function init() {
    wireConfirms(document);
    wireRowAdders(document);
    wireDropdowns(document);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
