/*
  Reusable button component.
  The colors come from the "brand" @theme palette in src/index.css; changing the
  colors there changes the buttons automatically.

  Usage:
    <Button>Giris</Button>                     // default: primary + md
    <Button variant="outline">Iptal</Button>
    <Button size="lg" fullWidth>Kaydet</Button>
*/

const base =
  'inline-flex items-center justify-center font-semibold rounded-lg transition-colors ' +
  'focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2 ' +
  'disabled:opacity-60 disabled:cursor-not-allowed'

// Color variants - all of them use the brand palette
const variants = {
  primary: 'bg-brand-700 hover:bg-brand-800 text-white',
  secondary: 'bg-brand-50 hover:bg-brand-100 text-brand-700',
  outline: 'border border-brand-700 text-brand-700 hover:bg-brand-50',
}

// Sizes - md is the usual one, changeable from this single place later
const sizes = {
  sm: 'text-sm px-3 py-1.5',
  md: 'px-4 py-2.5',
  lg: 'text-lg px-6 py-3',
}

function Button({
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  className = '',
  type = 'button',
  children,
  ...props
}) {
  const classes = [
    base,
    variants[variant] ?? variants.primary,
    sizes[size] ?? sizes.md,
    fullWidth ? 'w-full' : '',
    className,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <button type={type} className={classes} {...props}>
      {children}
    </button>
  )
}

export default Button
