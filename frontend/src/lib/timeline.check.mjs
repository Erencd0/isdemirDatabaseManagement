// Checks the timeline logic. No test framework, plain node:
//   node src/lib/timeline.check.mjs
//
// The geometry has to hold across the whole range the user can drag the rows to, not just at
// the height the board opens at.
import assert from 'node:assert/strict'
import {
  applyFilters,
  axisRange,
  AXIS_H,
  BAR_H,
  BAR_RATIO,
  barTop,
  boardHeight,
  clampRow,
  clampZoom,
  datesOf,
  dayHours,
  dayOf,
  dayPercent,
  dayWindow,
  formatDuration,
  formatHour,
  LANE_BAR_H,
  LANE_H,
  guideLine,
  HOUR,
  HOUR_PX,
  MAX_BOARD_H,
  MAX_HOUR_PX,
  MAX_ROW_H,
  maxRowHeight,
  MIN_HOUR_PX,
  MIN_ROW_H,
  ROW_H,
  stats,
  tickInterval,
  shiftWeeks,
  showsCastingNo,
  timeTicks,
  toBars,
  toDateStr,
  weekBarHeight,
  weekDates,
  weekLaneHeight,
  WEEK_MAX_LANE_H,
  WEEK_NO_ZOOM,
  weekOf,
  weekTickStep,
  zoomPercent,
} from './timeline.js'

const heats = [
  // KV1, 08:15 -> 09:50
  { dokumId: 1, dokumNo: 6100001, konverterNo: 1, hurdaSarjBaslamaZamani: '2026-08-06T08:15', dokumZamani: '2026-08-06T09:50' },
  // KV2, 09:00 -> 10:30 (runs in parallel with KV1)
  { dokumId: 2, dokumNo: 6200001, konverterNo: 2, hurdaSarjBaslamaZamani: '2026-08-06T09:00', dokumZamani: '2026-08-06T10:30' },
  // KV3, still in production: no casting time yet -> must not be drawn
  { dokumId: 3, dokumNo: 6300001, konverterNo: 3, hurdaSarjBaslamaZamani: '2026-08-06T10:00', dokumZamani: null },
  // KV1, second heat of the day
  { dokumId: 4, dokumNo: 6100002, konverterNo: 1, hurdaSarjBaslamaZamani: '2026-08-06T10:05', dokumZamani: '2026-08-06T11:40' },
]

// --- filtering by day ---
const twoDays = [
  ...heats,
  // the day before: same converters, must not show up when 06.08 is picked
  { dokumId: 5, dokumNo: 6100009, konverterNo: 1, hurdaSarjBaslamaZamani: '2026-08-05T09:00', dokumZamani: '2026-08-05T10:00' },
  // starts late on the 5th and taps after midnight - it belongs to the day it was CHARGED
  { dokumId: 6, dokumNo: 6200009, konverterNo: 2, hurdaSarjBaslamaZamani: '2026-08-05T23:40', dokumZamani: '2026-08-06T00:35' },
]
assert.deepEqual(datesOf(twoDays), ['2026-08-05', '2026-08-06'])
assert.equal(dayOf(twoDays[5]), '2026-08-05', 'a heat that taps after midnight stays on its charging day')
assert.equal(applyFilters(twoDays, { tarih: '2026-08-06' }).length, 4)
assert.equal(applyFilters(twoDays, { tarih: '2026-08-05' }).length, 2)
assert.equal(applyFilters(twoDays, { tarih: '2026-08-04' }).length, 0) // a day with no castings
assert.equal(applyFilters(twoDays, { tarih: 'all' }).length, 6)
// a heat with no charging time has no day, so it cannot belong to the one being viewed
assert.equal(dayOf({ hurdaSarjBaslamaZamani: null }), '')
assert.ok(!datesOf([{ hurdaSarjBaslamaZamani: null }]).length)
// the two filters compose - neither one cancels the other
assert.equal(applyFilters(twoDays, { tarih: '2026-08-05', konverter: '2' }).length, 1)
assert.equal(applyFilters(twoDays, { tarih: '2026-08-05', konverter: '3' }).length, 0)
assert.equal(applyFilters(twoDays, { tarih: 'all', konverter: '1' }).length, 3)

// --- filtering by converter ---
assert.equal(applyFilters(heats, { konverter: 'all' }).length, 4)
assert.deepEqual(
  applyFilters(heats, { konverter: '1' }).map((h) => h.dokumNo),
  [6100001, 6100002],
)
// the select gives a string, the entity holds a number
assert.equal(applyFilters(heats, { konverter: 2 }).length, 1)
assert.equal(applyFilters(heats, { konverter: '3' }).length, 1)

// --- bars: the unfinished heat is dropped ---
const bars = toBars(heats)
assert.equal(bars.length, 3)
assert.ok(!bars.some((b) => b.dokumNo === 6300001))
assert.equal(heats.length - bars.length, 1) // what the "eksik" note counts

// --- axis: decided by the castings, NOT rounded out to whole hours ---
const PADDING = 5 * 60000
const { from, to } = axisRange(bars)
// earliest scrap charging is 08:15, latest casting 11:40
assert.equal(new Date(from + PADDING).getHours(), 8)
assert.equal(new Date(from + PADDING).getMinutes(), 15)
assert.equal(new Date(to - PADDING).getHours(), 11)
assert.equal(new Date(to - PADDING).getMinutes(), 40)

// --- hour marks: only the whole hours inside the window ---
const ticks = timeTicks(from, to, 60)
assert.deepEqual(
  ticks.map((t) => new Date(t).getHours()),
  [9, 10, 11],
) // 08:10 is past 08:00, and 12:00 is past the end
assert.ok(ticks.every((t) => new Date(t).getMinutes() === 0))
assert.ok(ticks.every((t) => t >= from && t <= to))

// --- pixel mapping: a bar never lands outside the axis, at any zoom the user can reach ---
for (const scale of [MIN_HOUR_PX, HOUR_PX, MAX_HOUR_PX]) {
  const x = (t) => ((t - from) / HOUR) * scale
  const width = ((to - from) / HOUR) * scale
  for (const b of bars) {
    assert.ok(x(b.start.getTime()) >= 0, `${b.dokumNo} starts before the axis at ${scale}`)
    assert.ok(x(b.end.getTime()) <= width, `${b.dokumNo} ends past the axis at ${scale}`)
    assert.ok(x(b.end.getTime()) > x(b.start.getTime()), `${b.dokumNo} has no width at ${scale}`)
  }
  // the earliest casting sits exactly at the padding, not at 0 and not on an hour mark
  assert.equal(x(bars[0].start.getTime()), (PADDING / HOUR) * scale)
}

// --- a lone casting gets an axis its own length, no invented hours ---
const short = toBars([
  { dokumId: 9, dokumNo: 6100003, konverterNo: 1, hurdaSarjBaslamaZamani: '2026-08-06T08:10', dokumZamani: '2026-08-06T08:25' },
])
const tiny = axisRange(short)
assert.equal(tiny.to - tiny.from, 15 * 60000 + 2 * PADDING)

// --- hover guides: the line must land exactly on the top edge of the hovered bar ---
// checked at the height the board opens at AND at both ends of the drag range, because the
// user can resize the rows while a guide is on screen
for (const rowH of [MIN_ROW_H, ROW_H, MAX_ROW_H]) {
  const barH = Math.round(rowH * BAR_RATIO)
  for (const row of [0, 1, 2]) {
    const line = guideLine(row, rowH, barH)
    assert.equal(
      line.top + line.height,
      barTop(row, rowH, barH),
      `guide misses the bar in row ${row} at rowH ${rowH}`,
    )
    assert.ok(line.top < AXIS_H, 'the guide has to reach up into the axis')
    assert.ok(line.height > 0, `guide has no length in row ${row} at rowH ${rowH}`)
    // the bar sits inside its own row, never spilling into the one above or below
    assert.ok(barTop(row, rowH, barH) >= AXIS_H + row * rowH, `bar rides up out of row ${row}`)
    assert.ok(
      barTop(row, rowH, barH) + barH <= AXIS_H + (row + 1) * rowH,
      `bar overflows row ${row} at rowH ${rowH}`,
    )
  }
  // a lower row means a longer line, never a shorter one
  assert.ok(guideLine(2, rowH, barH).height > guideLine(1, rowH, barH).height)
  assert.ok(guideLine(1, rowH, barH).height > guideLine(0, rowH, barH).height)
}
// the defaults still describe the board as it opens
assert.equal(barTop(0), barTop(0, ROW_H, BAR_H))
assert.equal(Math.round(ROW_H * BAR_RATIO), BAR_H)

// --- the board never grows past its ceiling, however far the grip is dragged ---
for (const rowCount of [1, 2, 3]) {
  const max = maxRowHeight(rowCount)
  assert.ok(
    boardHeight(rowCount, max) <= MAX_BOARD_H,
    `${rowCount} channels at ${max}px make a ${boardHeight(rowCount, max)}px board`,
  )
  // dragging past the ceiling, or above it, is pinned to the ceiling
  assert.equal(clampRow(9999, max), max)
  assert.equal(clampRow(MIN_ROW_H - 50, max), MIN_ROW_H)
  assert.ok(boardHeight(rowCount, clampRow(9999, max)) <= MAX_BOARD_H)
  // and the channels stay usable rather than being squeezed to nothing
  assert.ok(max >= MIN_ROW_H)
}
// With every converter on screen the ceiling is what stops the drag, and it is nearly
// reached rather than left far short. Derived from MAX_BOARD_H, so retuning that constant
// does not send anyone editing numbers in here.
assert.ok(boardHeight(3, maxRowHeight(3)) <= MAX_BOARD_H)
assert.ok(
  boardHeight(3, maxRowHeight(3)) > MAX_BOARD_H - 3,
  'three channels should reach the ceiling, give or take the rounding of a whole pixel',
)
// filtered down to one converter the per channel limit stops it first
assert.equal(maxRowHeight(1), MAX_ROW_H)
assert.ok(boardHeight(1, MAX_ROW_H) < MAX_BOARD_H)
// filtered down to one converter the per channel limit stops it first
assert.equal(maxRowHeight(1), MAX_ROW_H)
assert.ok(boardHeight(1, MAX_ROW_H) < MAX_BOARD_H)

// --- the figures above the board follow the bars, not the raw rows ---
const shift = stats(bars)
assert.equal(shift.count, 3) // the unfinished heat is not counted
assert.equal(shift.longest, 95 * 60000) // 08:15 -> 09:50 and 10:05 -> 11:40
assert.equal(shift.shortest, 90 * 60000) // 09:00 -> 10:30
assert.equal(shift.total, (95 + 90 + 95) * 60000)
assert.equal(shift.average, shift.total / 3)
assert.ok(shift.shortest <= shift.average && shift.average <= shift.longest)
// filtering the chart has to filter the figures with it
assert.equal(stats(toBars(applyFilters(heats, { konverter: '2' }))).count, 1)
// an empty selection must not produce NaN readings
assert.deepEqual(stats([]), { count: 0, total: 0, average: 0, longest: 0, shortest: 0 })

// --- zoom: the readout never goes below the floor or above the ceiling ---
assert.equal(zoomPercent(HOUR_PX), 100)
assert.equal(zoomPercent(MIN_HOUR_PX), 41, 'zoomed all the way out must read 41%')
assert.equal(zoomPercent(MAX_HOUR_PX), 255)
// zooming out past the floor is pinned to it, not allowed through
assert.equal(clampZoom(1), MIN_HOUR_PX)
assert.equal(clampZoom(MIN_HOUR_PX - 20), MIN_HOUR_PX)
assert.equal(clampZoom(99999), MAX_HOUR_PX)
assert.equal(clampZoom(HOUR_PX), HOUR_PX)
// repeated wheel-out steps settle on the floor and stay there
let out = HOUR_PX
for (let i = 0; i < 50; i++) out = clampZoom(out / 1.1)
assert.equal(out, MIN_HOUR_PX)
assert.ok(zoomPercent(out) >= 41, 'zoom escaped below 41%')

// --- the axis subdivides as it is zoomed into ---
assert.equal(tickInterval(MIN_HOUR_PX), 120) // furthest out -> two hour marks
assert.equal(tickInterval(HOUR_PX), 60) // the scale the board opens at -> hours
assert.equal(tickInterval(250), 30) // zoomed in -> half hours (13:30, 14:00, 14:30)
assert.equal(tickInterval(MAX_HOUR_PX), 15) // right in -> quarter hours
// finer intervals never appear as you zoom OUT, only as you zoom in. Walked across the range
// the user can actually reach, so the floor moving cannot leave this checking dead scales.
let previous = 0
for (let px = MAX_HOUR_PX; px >= MIN_HOUR_PX; px -= 1) {
  const step = tickInterval(px)
  assert.ok(step >= previous, `axis got finer while zooming out at ${px}px/hour`)
  previous = step
  // whatever the zoom, two marks are never closer than a label needs
  assert.ok((step / 60) * px >= 95, `marks collide at ${px}px/hour`)
  // and the step always divides an hour or a day, so marks land on sayable times
  assert.ok(step <= 60 ? 60 % step === 0 : 1440 % step === 0, `${step} min is not a round step`)
}

// --- the marks are counted from midnight, so half hours land on :00 and :30 ---
const noon = new Date('2026-08-06T12:07').getTime()
const evening = new Date('2026-08-06T15:20').getTime()
const half = timeTicks(noon, evening, 30)
// 12:30 .. 15:00; 15:30 would fall outside the window
assert.deepEqual(
  half.map((t) => new Date(t).getMinutes()),
  [30, 0, 30, 0, 30, 0],
)
assert.ok(half.every((t) => t >= noon && t <= evening))
assert.equal(new Date(half[0]).getHours(), 12) // first mark after 12:07 is 12:30
// a three hour step still starts from midnight, not from where the window opens
assert.deepEqual(
  timeTicks(noon, evening, 180).map((t) => new Date(t).getHours()),
  [15],
)
// nothing is emitted for a window shorter than one step
assert.deepEqual(timeTicks(noon, noon + 60000, 30), [])

// --- durations ---
assert.equal(formatDuration(95 * 60000), '1 sa 35 dk')
assert.equal(formatDuration(40 * 60000), '40 dk')
assert.equal(formatDuration(2 * HOUR), '2 sa 0 dk')

// --- the week: which Monday a day belongs to ---
// 06.08.2026 is a Thursday, so its week runs 03.08 (Mon) -> 09.08 (Sun)
assert.equal(weekOf('2026-08-06'), '2026-08-03')
assert.equal(weekOf('2026-08-03'), '2026-08-03', 'a Monday is the start of its own week')
assert.equal(weekOf('2026-08-09'), '2026-08-03', 'Sunday closes the week, it does not open one')
assert.equal(weekOf('2026-08-10'), '2026-08-10', 'the next Monday starts a new week')
// every day of the same week has to land on the same Monday
const sameWeek = ['2026-08-03', '2026-08-04', '2026-08-05', '2026-08-06', '2026-08-07', '2026-08-08', '2026-08-09']
assert.equal(new Set(sameWeek.map(weekOf)).size, 1)
// a week that straddles a month and a year boundary: 01.01.2026 is a Thursday
assert.equal(weekOf('2026-01-01'), '2025-12-29')
assert.equal(weekOf('2026-03-02'), '2026-03-02')
assert.equal(weekOf('2026-03-01'), '2026-02-23', 'a Sunday belongs to the month it started in')
// nothing to file when there is no day
assert.equal(weekOf(''), '')
assert.equal(weekOf('all'), '')

// --- the seven days drawn, whether or not they ran ---
const drawn = weekDates('2026-08-06')
assert.equal(drawn.length, 7)
assert.deepEqual(drawn, sameWeek)
// any day of the week gives the same seven
assert.deepEqual(weekDates('2026-08-09'), drawn)
assert.deepEqual(weekDates('2026-08-03'), drawn)
// consecutive, with no day skipped or repeated
for (let i = 1; i < drawn.length; i++) {
  assert.equal(dayWindow(drawn[i]).from - dayWindow(drawn[i - 1]).from, 24 * 3600_000)
}
// the fold crosses a month end without losing a day
assert.deepEqual(weekDates('2026-01-01'), [
  '2025-12-29', '2025-12-30', '2025-12-31', '2026-01-01', '2026-01-02', '2026-01-03', '2026-01-04',
])
assert.deepEqual(weekDates('all'), [], 'no week picked, nothing to draw')

// --- the day window: local midnight to local midnight, never UTC ---
for (const date of drawn) {
  const win = dayWindow(date)
  const start = new Date(win.from)
  assert.equal(start.getHours(), 0)
  assert.equal(start.getMinutes(), 0)
  assert.equal(toDateStr(start), date, `${date} did not round-trip through its own window`)
  // the window closes on the NEXT midnight, so it covers the whole day and nothing more
  assert.equal(new Date(win.to).getHours(), 0)
  assert.equal(toDateStr(new Date(win.to - 1)), date, `${date} stops short of its own end`)
}

// --- where a casting sits inside its day, in percent ---
const day = dayWindow('2026-08-06')
const at = (time) => dayPercent(new Date(`2026-08-06T${time}`).getTime(), day)
assert.equal(at('00:00'), 0)
assert.equal(at('06:00'), 25)
assert.equal(at('12:00'), 50)
assert.equal(at('18:00'), 75)
// every casting charged on the day opens inside the lane, and closes to the right of where
// it opened - checked across the whole week's worth of bars, not just one
const weekHeats = [
  ...twoDays,
  { dokumId: 7, dokumNo: 6300007, konverterNo: 3, hurdaSarjBaslamaZamani: '2026-08-03T00:05', dokumZamani: '2026-08-03T01:30' },
  { dokumId: 8, dokumNo: 6100008, konverterNo: 1, hurdaSarjBaslamaZamani: '2026-08-09T22:10', dokumZamani: '2026-08-09T23:55' },
]
for (const b of toBars(weekHeats)) {
  const win = dayWindow(dayOf(b))
  const left = dayPercent(b.start.getTime(), win)
  const right = dayPercent(b.end.getTime(), win)
  assert.ok(left >= 0 && left < 100, `${b.dokumNo} opens outside its own lane at ${left}%`)
  assert.ok(right > left, `${b.dokumNo} has no width`)
}
// the one that taps after midnight stays on its charging day and runs off the right edge,
// where the lane clips it
const crosses = toBars(weekHeats).find((b) => b.dokumNo === 6200009)
assert.equal(dayOf(crosses), '2026-08-05')
assert.ok(dayPercent(crosses.end.getTime(), dayWindow('2026-08-05')) > 100)

// --- filtering by week ---
assert.equal(applyFilters(weekHeats, { hafta: '2026-08-06' }).length, weekHeats.length)
// a day in a different week pulls none of these in
assert.equal(applyFilters(weekHeats, { hafta: '2026-08-10' }).length, 0)
assert.equal(applyFilters(weekHeats, { hafta: '2026-07-27' }).length, 0)
assert.equal(applyFilters(weekHeats, { hafta: 'all' }).length, weekHeats.length)
// it composes with the converter filter, like the day filter does
assert.equal(applyFilters(weekHeats, { hafta: '2026-08-06', konverter: '3' }).length, 2)
// a heat with no charging time has no week either
assert.equal(applyFilters([{ hurdaSarjBaslamaZamani: null }], { hafta: '2026-08-06' }).length, 0)
// the week holds every day the day filter would find on its own
for (const date of drawn) {
  const ofDay = applyFilters(weekHeats, { tarih: date })
  const ofWeek = applyFilters(weekHeats, { hafta: date })
  assert.ok(ofDay.every((h) => ofWeek.includes(h)), `${date} is not inside its own week`)
}

// --- the week axis ---
const hours = dayHours(3)
assert.deepEqual(hours, [0, 3, 6, 9, 12, 15, 18, 21])
// 24:00 is the right edge of the lane, not a mark: it would sit on the next day's 00:00
assert.ok(!hours.includes(24))
assert.ok(hours.every((h) => (h / 24) * 100 < 100))
assert.equal(formatHour(0), '00:00')
assert.equal(formatHour(9), '09:00')
assert.deepEqual(dayHours(6), [0, 6, 12, 18])

// --- the week lanes: 21 of them have to stay on one page ---
assert.ok(LANE_BAR_H < LANE_H, 'the bar has to leave the lane some air')
assert.ok(LANE_BAR_H > 0)
assert.equal(LANE_BAR_H, Math.round(LANE_H * BAR_RATIO), 'the bar keeps its share of the lane')
assert.ok(LANE_H < ROW_H, 'week lanes are the compact ones')
// seven days of three converters, plus the axis, inside a page worth of height
assert.ok(AXIS_H + 7 * 3 * LANE_H < 700, 'the week board does not fit on one screen')

// --- stepping between weeks ---
assert.equal(shiftWeeks('2026-08-06', 1), '2026-08-13')
assert.equal(shiftWeeks('2026-08-06', -1), '2026-07-30')
assert.equal(shiftWeeks('2026-08-06', 0), '2026-08-06')
// a step always lands on the same weekday, and always one whole week away
for (const delta of [-3, -1, 1, 2, 5]) {
  const landed = shiftWeeks('2026-08-06', delta)
  assert.equal(new Date(`${landed}T00:00:00`).getDay(), new Date('2026-08-06T00:00:00').getDay())
  assert.equal(weekOf(landed), shiftWeeks(weekOf('2026-08-06'), delta))
}
// stepping over a month end, and over a year end, keeps the weeks whole
assert.equal(shiftWeeks('2026-01-01', -1), '2025-12-25')
assert.deepEqual(weekDates(shiftWeeks('2026-08-06', 1))[0], '2026-08-10')
// forward then back is where it started
assert.equal(shiftWeeks(shiftWeeks('2026-08-06', 4), -4), '2026-08-06')
assert.equal(shiftWeeks('all', 1), '', 'nothing to step from without a day')
// the seven days a step lands on follow straight on from the seven before it
const before = weekDates('2026-08-06')
const after = weekDates(shiftWeeks('2026-08-06', 1))
assert.equal(dayWindow(after[0]).from - dayWindow(before[6]).from, 24 * 3600_000)

// --- the week axis subdivides as it is zoomed into ---
assert.equal(weekTickStep(1), 3) // as it opens: 00:00, 03:00 ... 21:00
assert.equal(weekTickStep(2), 2)
assert.equal(weekTickStep(4), 1) // right in: every hour
// finer marks never appear while zooming OUT, only while zooming in
let coarsest = 0
for (let z = MAX_HOUR_PX / HOUR_PX; z >= 1; z -= 0.01) {
  const step = weekTickStep(z)
  assert.ok(step >= coarsest, `the week axis got finer while zooming out at ${z}`)
  coarsest = step
  // every step has to divide the day, or the marks stop landing on whole hours
  assert.equal(24 % step, 0, `${step}h does not divide a day`)
  assert.ok(dayHours(step).length <= 24)
}

// --- zooming the week moves in on BOTH axes ---
// At 100% the lanes are the compact ones the board opens at
assert.equal(weekLaneHeight(1), LANE_H)
assert.equal(weekBarHeight(1), LANE_BAR_H)
// zooming in makes them taller, so a casting comes closer instead of only growing longer
assert.ok(weekLaneHeight(2) > weekLaneHeight(1))
assert.ok(weekBarHeight(2) > weekBarHeight(1))
// never shorter than it opens at, however far out the zoom is pushed
for (const z of [0, 0.1, 0.41, 0.99, 1]) {
  assert.equal(weekLaneHeight(z), LANE_H, `the lane shrank below its floor at ${z}`)
}
// taller is monotonic, and it stops at the cap rather than running off
let previousLane = 0
for (let z = 1; z <= MAX_HOUR_PX / HOUR_PX; z += 0.01) {
  const h = weekLaneHeight(z)
  assert.ok(h >= previousLane, `the lane shrank while zooming in at ${z}`)
  assert.ok(h <= WEEK_MAX_LANE_H, `the lane passed its cap at ${z}`)
  previousLane = h
  // the bar keeps its share of whatever height the lane is at, and stays inside it
  assert.equal(weekBarHeight(z), Math.round(h * BAR_RATIO))
  assert.ok(weekBarHeight(z) < h, `the bar fills the whole lane at ${z}`)
  assert.ok(weekBarHeight(z) > 0)
}
// the cap is actually reached inside the zoom range, not left unreachable
assert.equal(weekLaneHeight(MAX_HOUR_PX / HOUR_PX), WEEK_MAX_LANE_H)
// and the board stays navigable at full zoom: seven days of three, plus the axis
assert.ok(
  AXIS_H + 7 * 3 * WEEK_MAX_LANE_H < 1500,
  'zoomed right in, the week board is too deep to scroll through',
)

// --- the casting numbers are all out or all away, never half and half ---
assert.ok(!showsCastingNo(1), 'the numbers must stay away at the zoom the board opens at')
assert.ok(!showsCastingNo(1.1), 'one wheel step in is still too tight for them')
assert.ok(showsCastingNo(1.21), 'two wheel steps in is where they come out')
assert.ok(showsCastingNo(1.25), 'one button press in is past the threshold too')
// the two wheel steps that reach the threshold have to actually land on it, floating point
// and all - 1.1 * 1.1 must not come out at 120%
assert.equal(Math.round((HOUR_PX * 1.1 * 1.1) / HOUR_PX * 100), WEEK_NO_ZOOM)
assert.ok(showsCastingNo((HOUR_PX * 1.1 * 1.1) / HOUR_PX))
// it goes one way only: once out, zooming further in never takes them away again
let shown = false
for (let z = 1; z <= MAX_HOUR_PX / HOUR_PX; z += 0.005) {
  const now = showsCastingNo(z)
  assert.ok(!(shown && !now), `the numbers disappeared again while zooming in at ${z}`)
  shown = now
}
assert.ok(shown, 'the numbers never appear anywhere in the zoom range')
// the threshold is the number on the readout, so what the user reads is what they get
assert.ok(showsCastingNo(WEEK_NO_ZOOM / 100))
assert.ok(!showsCastingNo((WEEK_NO_ZOOM - 1) / 100))

// --- the week zoom floor: 100% is already a full screen per day ---
assert.equal(clampZoom(HOUR_PX, HOUR_PX), HOUR_PX)
assert.equal(clampZoom(MIN_HOUR_PX, HOUR_PX), HOUR_PX, 'the week board must not shrink below fit')
assert.equal(clampZoom(1, HOUR_PX), HOUR_PX)
assert.equal(clampZoom(MAX_HOUR_PX * 5, HOUR_PX), MAX_HOUR_PX, 'the ceiling still holds')
// carried in from a zoomed out day board, the week opens at exactly 100%
assert.equal(zoomPercent(clampZoom(MIN_HOUR_PX, HOUR_PX)), 100)
// and the day board keeps its own, lower floor
assert.equal(clampZoom(1), MIN_HOUR_PX)
assert.equal(clampZoom(1, MIN_HOUR_PX), MIN_HOUR_PX)
// wheeling out repeatedly settles on whichever floor is in force and stays there
for (const floor of [MIN_HOUR_PX, HOUR_PX]) {
  let px = MAX_HOUR_PX
  for (let i = 0; i < 60; i++) px = clampZoom(px / 1.1, floor)
  assert.equal(px, floor)
}

console.log('timeline: all checks passed')
