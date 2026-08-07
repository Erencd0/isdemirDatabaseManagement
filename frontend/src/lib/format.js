// Shared display helpers. Used by the dashboard and the timeline page.

// Makes a datetime-local / ISO value readable ("2026-08-02T14:30" -> "02.08.2026 14:30")
export function formatDate(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (isNaN(d)) return value
  return d.toLocaleString('tr-TR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
