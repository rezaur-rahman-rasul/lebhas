/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        canvas: 'rgb(var(--color-canvas) / <alpha-value>)',
        surface: 'rgb(var(--color-surface) / <alpha-value>)',
        'surface-elevated': 'rgb(var(--color-surface-elevated) / <alpha-value>)',
        panel: 'rgb(var(--color-panel) / <alpha-value>)',
        'panel-strong': 'rgb(var(--color-panel-strong) / <alpha-value>)',
        ink: 'rgb(var(--color-ink) / <alpha-value>)',
        muted: 'rgb(var(--color-muted) / <alpha-value>)',
        border: 'rgb(var(--color-border) / <alpha-value>)',
        'border-strong': 'rgb(var(--color-border-strong) / <alpha-value>)',
        icon: 'rgb(var(--color-icon) / <alpha-value>)',
        input: 'rgb(var(--color-input) / <alpha-value>)',
        dropdown: 'rgb(var(--color-dropdown) / <alpha-value>)',
        brand: {
          50: '#eefdf7',
          100: '#d6faec',
          500: '#16a679',
          600: '#0b8f68',
          700: '#067052',
          900: '#063f31',
        },
        accent: {
          500: '#2563eb',
          600: '#1d4ed8',
        },
        alert: {
          500: '#dc2626',
          600: '#b91c1c',
        },
      },
      boxShadow: {
        soft: '0 18px 60px rgba(15, 23, 42, 0.08)',
        panel: '0 28px 90px rgba(2, 6, 23, 0.22)',
      },
      fontFamily: {
        sans: ['Inter', 'Noto Sans Bengali', 'system-ui', 'sans-serif'],
      },
    },
  },
};
