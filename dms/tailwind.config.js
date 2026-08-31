module.exports = {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        darkBg: 'hsl(222, 47%, 11%)',
        cardBg: 'rgba(30, 41, 59, 0.7)',
        neonCyan: '#06b6d4',
        neonPurple: '#8b5cf6',
      }
    },
  },
  plugins: [],
}
