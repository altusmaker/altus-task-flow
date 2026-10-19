# Altus Theme Kit

This folder is the portable visual foundation for another Spring/Thymeleaf project. Copy it together with these existing assets:

- `static/images/altus-logo-square.png`
- `static/fonts/Vazir.ttf` and `static/css/vazir.css`
- `static/css/jalali-reminder.css` and `static/js/jalali-picker.js` when Jalali date/time selection is required

Add these resources after Bootstrap:

```html
<link rel="stylesheet" th:href="@{/css/vazir.css}">
<link rel="stylesheet" th:href="@{/theme/altus-theme.css}">
<link rel="stylesheet" th:href="@{/css/jalali-reminder.css}">
<script th:src="@{/js/jalali-picker.js}"></script>
<script th:src="@{/theme/altus-theme.js}"></script>
```

Apply `altus-app` to `body`. Use `altus-panel`, `altus-btn-primary`, and `altus-input` for common UI components. The theme exposes both reusable `--altus-*` tokens and compatibility aliases (`--primary`, `--surface`, and so on).

Add this first inside the body for the splash screen:

```html
<div class="altus-loader" role="status" aria-label="Loading">
  <div class="altus-loader__content">
    <img class="altus-loader__logo" th:src="@{/images/altus-logo-square.png}" alt="Altus">
    <div class="altus-loader__bar" aria-hidden="true"></div>
  </div>
</div>
```

Use `AltusTheme.toggle()` from any button to switch modes. The preference is stored locally. Create a Jalali date/time input with `JalaliDatePicker.create({ input, valueInput })`; use the `jalali-date-input` class and a positioned parent, as in this project.
