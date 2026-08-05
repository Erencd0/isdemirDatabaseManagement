/*
  Simple, reusable popup (modal) component.
  It puts a semi transparent black layer over the background and opens a white card
  in the middle.

  - open: visible when true
  - onClose: called when the background or the X is clicked
  - title: the heading in the top bar
  - maxWidth: the width of the card (e.g. "max-w-lg", "max-w-3xl")

  Modals can be nested (a second modal from inside a modal) because each one stacks
  on top with fixed + z-50 and stopPropagation keeps them from closing each other.
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
