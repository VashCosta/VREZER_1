/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        themeBlack: '#050505',
        themeRed: '#C1121F',
        themeWhite: '#FFFFFF',
        themeGold: '#D4AF37',
        brand: {
          50: '#fff1f2',
          100: '#ffe4e6',
          500: '#C1121F',
          600: '#A10E1A',
          700: '#780B13',
          900: '#4A060B',
        },
        dark: {
          bg: '#050505',
          card: '#0F0F0F',
          border: 'rgba(193, 18, 31, 0.3)',
          hover: '#171717'
        },
        gold: {
          50: '#fffbeb',
          100: '#fef3c7',
          500: '#D4AF37',
          600: '#B89628',
          700: '#91741C',
        }
      },
      fontFamily: {
        sans: ['Inter', 'Plus Jakarta Sans', 'system-ui', 'sans-serif'],
        mono: ['Fira Code', 'monospace']
      },
      boxShadow: {
        'red-glow': '0 0 25px -5px rgba(193, 18, 31, 0.5)',
        'red-glow-lg': '0 0 45px -5px rgba(193, 18, 31, 0.6)',
        'gold-glow': '0 0 25px -5px rgba(212, 175, 55, 0.5)',
        'red-inner': 'inset 0 0 20px rgba(193, 18, 31, 0.25)',
      },
      animation: {
        'pulse-slow': 'pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'float': 'float 6s ease-in-out infinite',
        'scan': 'scan 3s linear infinite',
        'laser-pulse': 'laserPulse 2s infinite alternate',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        scan: {
          '0%': { top: '0%' },
          '100%': { top: '100%' },
        },
        laserPulse: {
          '0%': { opacity: '0.4', filter: 'drop-shadow(0 0 5px #C1121F)' },
          '100%': { opacity: '1', filter: 'drop-shadow(0 0 20px #C1121F)' }
        }
      }
    },
  },
  plugins: [],
}
