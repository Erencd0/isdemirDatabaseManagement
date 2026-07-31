/*
  Tekrar kullanilabilir buton componenti.
  Renkler src/index.css icindeki @theme "brand" paletinden gelir; oradaki
  renkleri degistirince butonlar da otomatik degisir.

  Kullanim:
    <Button>Giris</Button>                     // varsayilan: primary + md
    <Button variant="outline">Iptal</Button>
    <Button size="lg" fullWidth>Kaydet</Button>
*/

const base =
  'inline-flex items-center justify-center font-semibold rounded-lg transition-colors ' +
  'focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-2 ' +
  'disabled:opacity-60 disabled:cursor-not-allowed'

// Renk varyantlari - hepsi brand paletini kullanir
const variants = {
  primary: 'bg-brand-700 hover:bg-brand-800 text-white',
  secondary: 'bg-brand-50 hover:bg-brand-100 text-brand-700',
  outline: 'border border-brand-700 text-brand-700 hover:bg-brand-50',
}

// Boyutlar - genelde md kullanilir, ileride tek yerden degistirilebilir
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
