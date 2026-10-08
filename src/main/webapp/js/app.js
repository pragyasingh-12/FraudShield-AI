/* FraudShield AI - small progressive-enhancement helpers (no framework). */
(function () {
  'use strict';

  // Confirm destructive actions: <form data-confirm="Delete this?">
  document.querySelectorAll('form[data-confirm]').forEach(function (form) {
    form.addEventListener('submit', function (e) {
      if (!window.confirm(form.getAttribute('data-confirm'))) e.preventDefault();
    });
  });

  // Auto-submit selects marked data-autosubmit (alert status dropdown)
  document.querySelectorAll('select[data-autosubmit]').forEach(function (sel) {
    sel.addEventListener('change', function () { sel.form.submit(); });
  });

  // Transaction form helpers: fill device/location from the chosen customer, and demo presets.
  var form = document.getElementById('txn-form');
  if (!form) return;
  var user = form.querySelector('[name=userId]');
  var device = form.querySelector('[name=deviceId]');
  var location = form.querySelector('[name=location]');
  var receiver = form.querySelector('[name=receiver]');
  var amount = form.querySelector('[name=amount]');
  var time = form.querySelector('[name=time]');
  var type = form.querySelector('[name=type]');

  function hint(attr) {
    var opt = user.options[user.selectedIndex];
    return opt ? (opt.getAttribute(attr) || '') : '';
  }
  function setTime(hour, minute) {
    var base = (time.value || '').split('T')[0];
    if (!base) { var n = new Date(); base = n.toISOString().slice(0, 10); }
    time.value = base + 'T' + ('0' + hour).slice(-2) + ':' + ('0' + minute).slice(-2);
  }
  user.addEventListener('change', function () {
    device.value = hint('data-device');
    location.value = hint('data-location');
  });

  var presets = {
    normal: function () {
      amount.value = Math.round(parseFloat(hint('data-avg') || '2000'));
      device.value = hint('data-device'); location.value = hint('data-location');
      type.value = 'UPI'; receiver.value = hint('data-receiver') || 'swiggy@icici'; setTime(13, 15);
    },
    suspicious: function () {
      amount.value = Math.round(parseFloat(hint('data-avg') || '2000') * 25);
      device.value = 'DEV-UNKNOWN-7Z'; location.value = 'Kolkata';
      type.value = 'UPI'; receiver.value = 'quick.cash@paytm'; setTime(3, 20);
    }
  };
  document.querySelectorAll('[data-preset]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      if (user.selectedIndex < 0) return;
      presets[btn.getAttribute('data-preset')]();
    });
  });
})();
