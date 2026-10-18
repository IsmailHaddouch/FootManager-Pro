/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Identité visuelle football : bleu marine foncé, blanc, vert vif
        navy: {
          DEFAULT: '#0b1f3a',
          700: '#12305a',
          600: '#1a417a',
        },
        pitch: {
          DEFAULT: '#22c55e',
          600: '#16a34a',
        },
      },
    },
  },
  plugins: [],
}
