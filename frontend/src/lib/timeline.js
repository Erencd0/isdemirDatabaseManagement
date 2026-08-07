// The pure logic behind the Gantt chart: filtering, turning heats into bars and working out
// the time axis. Deliberately kept out of the chart component - the chart only draws what
// these return, so a new filter or a new axis rule never touches the drawing code.

export const HOUR = 3600_000
export const DAY = 24 * HOUR

// Which production day a casting belongs to: the day it was CHARGED on. A heat that starts
// at 23:40 and taps after midnight stays on the day it started, which is the day the shift
// that ran it would file it under.
// The backend sends "2026-08-06T00:25:00", and <input type="date"> speaks "2026-08-06", so
// the two meet without any parsing.
export const dayOf = (heat) => String(heat.hurdaSarjBaslamaZamani ?? '').slice(0, 10)

// Every production day present in the data, oldest first
export const datesOf = (heats) => [...new Set(heats.map(dayOf).filter(Boolean))].sort()

// ---- The week ----
// The week board folds seven days onto ONE time-of-day axis: every day is drawn against the
// same 00:00 -> 24:00 window and the days stack downwards. That is what keeps a week on
// screen without an axis that scrolls sideways forever, and it lines the days up so a gap
// that repeats every day (a shift change, an idle converter) reads as a column.

const pad = (n) => String(n).padStart(2, '0')

// A Date -> "YYYY-MM-DD" in LOCAL time. Not toISOString(), which is UTC and would file a
// local midnight under the previous day everywhere east of Greenwich.
export const toDateStr = (d) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

// Parses "YYYY-MM-DD" at LOCAL midnight. The bare form would be read as UTC.
const atMidnight = (dateStr) => new Date(`${dateStr}T00:00:00`)

// The Monday of the week a day falls in. Weeks start on Monday, the way the shift plan is
// written; getDay() counts from Sunday, hence the shift by 6.
export function weekOf(dateStr) {
  const d = atMidnight(dateStr)
  if (isNaN(d)) return ''
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7))
  return toDateStr(d)
}

// The seven days of the week a day falls in, Monday first. Days with no castings are still
// returned - an empty lane is information, it says the converter stood still.
export function weekDates(dateStr) {
  const monday = weekOf(dateStr)
  if (!monday) return []
  return Array.from({ length: 7 }, (_, i) => {
    const d = atMidnight(monday)
    d.setDate(d.getDate() + i)
    return toDateStr(d)
  })
}

// Midnight -> midnight of one production day. The end is reached by date arithmetic rather
// than by adding 24h, so the window still covers the whole day if one ever runs short or
// long. (Turkey has no DST today, but nothing here has to know that.)
export function dayWindow(dateStr) {
  const start = atMidnight(dateStr)
  const end = atMidnight(dateStr)
  end.setDate(end.getDate() + 1)
  return { from: start.getTime(), to: end.getTime() }
}

// Where a moment sits inside its day, as a percentage of the day's width. The week board is
// laid out in percent rather than pixels: a day always fills exactly the width it is given,
// so there is no zoom to hold and no horizontal scroll to get lost in.
// A casting charged before midnight and tapped after it runs past 100 and is clipped by the
// lane; it stays whole on the day it was charged, which is the day dayOf() files it under.
export const dayPercent = (time, { from, to }) => ((time - from) / (to - from)) * 100

// The marks on the week axis: every `stepHours` from midnight. 24:00 is left out - it is the
// right edge of the lane, and a mark there would sit on top of the next day's 00:00.
export function dayHours(stepHours = 3) {
  const out = []
  for (let h = 0; h < 24; h += stepHours) out.push(h)
  return out
}

export const formatHour = (h) => `${pad(h)}:00`

// A week zoomed in shows finer marks, the way the day axis subdivides as it is zoomed into.
// This one goes by the zoom multiplier rather than by pixels-per-hour: the week board is laid
// out in percent, so it never measures a pixel and has none to hand. Every step still divides
// 24, so the marks land on whole hours.
export const weekTickStep = (zoom) => (zoom >= 4 ? 1 : zoom >= 2 ? 2 : 3)

// Steps a day forward or back by whole weeks, staying on the same weekday.
export function shiftWeeks(dateStr, weeks) {
  const d = atMidnight(dateStr)
  if (isNaN(d)) return ''
  d.setDate(d.getDate() + weeks * 7)
  return toDateStr(d)
}

// The filters. Adding a new one (steel grade, status...) is one entry here plus one control
// in the filter panel; the chart does not change.
export const FILTERS = {
  konverter: (heat, value) => value === 'all' || heat.konverterNo === Number(value),
  tarih: (heat, value) => value === 'all' || dayOf(heat) === value,
  // The week view filters by the week a casting was charged in rather than by the day. The
  // control is still a single date - any day picked stands for the week it falls in.
  hafta: (heat, value) => value === 'all' || weekOf(dayOf(heat)) === weekOf(value),
}

export function applyFilters(heats, values) {
  return heats.filter((heat) =>
    Object.entries(values).every(([name, value]) => FILTERS[name](heat, value)),
  )
}

// A casting can only be drawn when it has BOTH ends of its interval. Those two columns are
// nullable, and a record that is still being filled in has no casting time yet.
export function toBars(heats) {
  return heats
    .filter((h) => h.hurdaSarjBaslamaZamani && h.dokumZamani)
    .map((h) => ({
      ...h,
      start: new Date(h.hurdaSarjBaslamaZamani),
      end: new Date(h.dokumZamani),
    }))
    .filter((b) => !isNaN(b.start) && !isNaN(b.end) && b.end >= b.start)
}

// A few minutes of air on both ends so the first and last milestone do not sit flush
// against the edge of the chart.
const AXIS_PADDING = 5 * 60_000

// The castings decide the axis: it runs from the earliest scrap charging to the latest
// casting time. It is NOT rounded out to whole hours - the chart shows the production
// window that actually happened, not a clock.
export function axisRange(bars) {
  const first = Math.min(...bars.map((b) => b.start.getTime()))
  const last = Math.max(...bars.map((b) => b.end.getTime()))
  return { from: first - AXIS_PADDING, to: last + AXIS_PADDING }
}

// The intervals the axis is allowed to mark, in minutes. Only steps that divide the day
// evenly, so the marks always land on times a person would actually say out loud.
const TICK_LADDER = [5, 10, 15, 30, 60, 120, 180, 360]

// How finely the axis is marked at a given zoom: the smallest interval from the ladder that
// still leaves room to write the label. Zoom in and the axis goes from hours to half hours
// to quarters; zoom out and it steps back up to two and three hour marks.
export function tickInterval(hourPx, minLabelPx = 95) {
  const needed = (minLabelPx / hourPx) * 60
  return TICK_LADDER.find((minutes) => minutes >= needed) ?? TICK_LADDER[TICK_LADDER.length - 1]
}

// The marks inside the axis, every intervalMin minutes. They are counted from midnight, not
// from where the window happens to begin, so a 30 minute step lands on :00 and :30 rather
// than on :07 and :37.
export function timeTicks(from, to, intervalMin) {
  const step = intervalMin * 60_000
  const midnight = new Date(from)
  midnight.setHours(0, 0, 0, 0)

  let t = midnight.getTime()
  while (t < from) t += step

  const ticks = []
  for (; t <= to; t += step) ticks.push(t)
  return ticks
}

// ---- Chart geometry ----
// Kept here rather than in Tailwind height classes because the hover guides have to know
// exactly where the hovered bar sits; split across two places they would silently drift.

// How wide one hour of production is drawn. This is the zoom: the chart keeps its scale and
// scrolls horizontally rather than squeezing a whole day into the screen, and the user moves
// this value to trade detail against overview. HOUR_PX is what the readout calls 100%.
export const HOUR_PX = 165
export const MIN_ZOOM = 0.41 // how far out the readout may go
export const MAX_ZOOM = 2.55

export const MIN_HOUR_PX = Math.round(HOUR_PX * MIN_ZOOM)
export const MAX_HOUR_PX = Math.round(HOUR_PX * MAX_ZOOM)

// The week view passes a floor of HOUR_PX: at 100% a day already fills the width it is
// given, so zooming out past that would only shrink the board away from the edges.
export const clampZoom = (px, floor = MIN_HOUR_PX) =>
  Math.min(Math.max(px, floor), MAX_HOUR_PX)

// What the readout shows, as a whole percent
export const zoomPercent = (hourPx) => Math.round((hourPx / HOUR_PX) * 100)

export const AXIS_H = 36 // the hour strip above the rows; does not grow with the rows
export const ROW_H = 92 // the height a converter channel opens at
export const BAR_H = 48 // the matching casting bar

// Dragging the handle changes the row height; the bars keep their share of it, so the
// channels never end up as hairlines in a tall row or as a solid block in a short one.
export const BAR_RATIO = BAR_H / ROW_H

// How far the grip may be dragged, and the ceiling it runs into first.
// MAX_BOARD_H covers the axis plus the channels - the drawn chart. The grip strip and the
// horizontal scrollbar sit outside it.
export const MIN_ROW_H = 44
export const MAX_ROW_H = 220
export const MAX_BOARD_H = 450

// The tallest a channel may be drawn, given how many are on screen. With all three
// converters showing, the board height is what runs out first; filtered down to one, the
// per channel ceiling does. Either way the board never passes MAX_BOARD_H.
export const maxRowHeight = (rowCount) =>
  Math.max(
    MIN_ROW_H,
    Math.min(MAX_ROW_H, Math.floor((MAX_BOARD_H - AXIS_H) / Math.max(rowCount, 1))),
  )

export const clampRow = (px, max = MAX_ROW_H) => Math.min(Math.max(px, MIN_ROW_H), max)

// ---- Week board geometry ----
// Twenty one lanes have to fit on one page, so the lanes are compact and fixed: there is no
// zoom and no resize grip in the week view, because a day already fits the width it is given.
export const LANE_H = 26
export const LANE_BAR_H = Math.round(LANE_H * BAR_RATIO)

// The lanes grow with the zoom as well as the day does. Without this, zooming only stretches
// the castings sideways - longer boxes, no closer - because the height is what a bar is read
// as much as its length. Capped, or twenty one lanes at full zoom would run several screens
// deep and the point of having the week in one place would be gone.
export const WEEK_MAX_LANE_H = 64
export const weekLaneHeight = (zoom) =>
  Math.min(Math.max(Math.round(LANE_H * zoom), LANE_H), WEEK_MAX_LANE_H)
export const weekBarHeight = (zoom) => Math.round(weekLaneHeight(zoom) * BAR_RATIO)

// The zoom at which the casting numbers come out, as a whole percent off the readout so the
// threshold is the one the user can see themselves.
// Below it the numbers fit on the long castings and not on the short ones, and a row where
// half the bars are labelled reads worse than a row where none of them are. All or nothing.
export const WEEK_NO_ZOOM = 121
export const showsCastingNo = (zoom) => Math.round(zoom * 100) >= WEEK_NO_ZOOM
// The two label columns to the left of the lanes: the day, then the converter. The axis
// header is inset by their sum so the hour marks line up with the lanes underneath.
export const WEEK_DAY_W = 68
export const WEEK_KV_W = 46
export const WEEK_INSET = WEEK_DAY_W + WEEK_KV_W

// The drawn height of the board, for a given channel height and count
export const boardHeight = (rowCount, rowH) => AXIS_H + rowCount * rowH

// Where the TOP EDGE of a bar sits, measured from the top of the chart. Bars are centred in
// their row. The heights are passed in because the user can resize the rows.
export const barTop = (rowIndex, rowH = ROW_H, barH = BAR_H) =>
  AXIS_H + rowIndex * rowH + (rowH - barH) / 2

// The vertical guide drawn on hover: it starts just under the time label on the axis and
// stops exactly on the top edge of the hovered bar, so it reads as coming out of it.
export function guideLine(rowIndex, rowH = ROW_H, barH = BAR_H) {
  const top = AXIS_H - 6
  return { top, height: barTop(rowIndex, rowH, barH) - top }
}

// What the shift adds up to. Feeds the readings above the chart and the per converter
// figures on the rail; every one of them is derived from the bars already on screen, so a
// filter changes them with the chart.
export function stats(bars) {
  const durations = bars.map((b) => b.end - b.start)
  const total = durations.reduce((sum, d) => sum + d, 0)
  return {
    count: bars.length,
    total,
    average: durations.length ? total / durations.length : 0,
    longest: durations.length ? Math.max(...durations) : 0,
    shortest: durations.length ? Math.min(...durations) : 0,
  }
}

export function formatDuration(ms) {
  const minutes = Math.round(ms / 60000)
  const hours = Math.floor(minutes / 60)
  return hours ? `${hours} sa ${minutes % 60} dk` : `${minutes} dk`
}
