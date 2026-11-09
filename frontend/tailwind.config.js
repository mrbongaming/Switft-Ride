/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        ink: '#18243b',
        muted: '#718098',
        brand: '#2463eb',
        canvas: '#f5f7fb',
      },
      fontFamily: {
        sans: ['DM Sans', 'sans-serif'],
        display: ['Manrope', 'sans-serif'],
      },
      boxShadow: {
        card: '0 8px 35px rgba(29, 48, 89, 0.045)',
      },
    },
  },
  plugins: [],
};
