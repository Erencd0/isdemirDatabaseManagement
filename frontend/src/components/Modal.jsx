/*
  Basit, tekrar kullanilabilir popup (modal) componenti.
  Arka plana yari saydam siyah bir katman koyar, ortada beyaz kart acar.

  - open: true ise gorunur
  - onClose: arka plana ya da X'e tiklaninca cagrilir
  - title: ust bardaki baslik
  - maxWidth: kartin genisligi (orn. "max-w-lg", "max-w-3xl")

  Ic ice acilabilir (bir modalin icinden ikinci modal) cunku her biri
  fixed + z-50 ile en uste biner ve stopPropagation ile birbirini kapatmaz.
*/
function Modal({ open, onClose, title, children, maxWidth = 'max-w-lg' }) {
  if (!open) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onClick={onClose}
    >
      <div
        className={`w-full ${maxWidth} max-h-[85vh] overflow-y-auto bg-white rounded-2xl shadow-2xl`}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-gray-200 px-6 py-4">
          <h2 className="text-lg font-bold text-brand-700">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Kapat"
            className="text-2xl leading-none text-gray-400 hover:text-gray-700"
          >
            &times;
          </button>
        </div>
        <div className="px-6 py-4">{children}</div>
      </div>
    </div>
  )
}

export default Modal
