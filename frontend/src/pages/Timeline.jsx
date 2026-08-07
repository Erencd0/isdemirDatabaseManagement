import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import Button from '../components/Button.jsx'
import { apiFetch, hasSession, logoutRequest, SessionError } from '../api/client.js'
import { formatDate } from '../lib/format.js'
import {
  applyFilters,
  axisRange,
  AXIS_H,
  BAR_RATIO,
  clampRow,
  clampZoom,
  datesOf,
  dayHours,
  dayOf,
  dayPercent,
  dayWindow,
  formatDuration,
  formatHour,
  guideLine,
  HOUR,
  HOUR_PX,
  MAX_HOUR_PX,
  maxRowHeight,
  MIN_HOUR_PX,
  MIN_ROW_H,
  ROW_H,
  shiftWeeks,
  showsCastingNo,
  stats,
  tickInterval,
  timeTicks,
  toBars,
  WEEK_DAY_W,
  WEEK_INSET,
  WEEK_KV_W,
  weekBarHeight,
  weekDates,
  weekLaneHeight,
  weekOf,
  weekTickStep,
  zoomPercent,
} from '../lib/timeline.js'

// Gantt chart of the castings: one channel per converter, one bar per casting.
// A bar spans the whole production interval: hurda sarj baslama -> dokum zamani.
//
// The data comes from GET /api/dokum/zaman-cizelgesi, which returns EVERY converter and not
// just the ones this user holds a kv role for - the point of the page is comparing the three
// converters against each other. (The dashboard list stays restricted, and so do all writes.)
//
// Filtering / bars / axis / figures live in lib/timeline.js; this file only draws them.

const ZOOM_STEP = 1.25 // per button press; the wheel uses a finer step

// The zoom limits (HOUR_PX / MIN_HOUR_PX / MAX_HOUR_PX) and the drag limits (MIN_ROW_H /
// MAX_ROW_H / MAX_BOARD_H) live in lib/timeline.js with the rest of the geometry.

// The converter rail on the left. It stays pinned while the board scrolls sideways.
const LABEL_W = 132

// The row / bar / axis heights come from lib/timeline.js, together with the geometry the
// hover guides need.

// The colour of each converter, used by the bars, the rail and the legend alike.
const CONVERTERS = [
  { no: 1, label: 'KV1', bar: 'bg-kv1', dot: 'bg-kv1' },
  { no: 2, label: 'KV2', bar: 'bg-kv2', dot: 'bg-kv2' },
  { no: 3, label: 'KV3', bar: 'bg-kv3', dot: 'bg-kv3' },
]

const formatTime = (d) => d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })

const EYEBROW = 'text-[10px] font-semibold uppercase tracking-[0.18em]'

// A square button for the control groups: zoom in / out, and stepping between weeks.
function IconButton({ onClick, disabled, label, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      className="px-3 py-2 text-base leading-none font-semibold text-gray-600 hover:bg-gray-50 hover:text-gray-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 disabled:cursor-not-allowed disabled:text-gray-300 disabled:hover:bg-transparent"
    >
      {children}
    </button>
  )
}

// One reading above the board. The number carries the weight, the label stays quiet.
function Reading({ label, value, unit }) {
  return (
    // The rules only appear once the readings sit in one row; wrapped into two columns they
    // would leave a divider hanging at the start of the second row.
    <div className="sm:border-l-2 sm:border-hairline sm:pl-4 sm:first:border-l-0 sm:first:pl-0">
      <p className={EYEBROW + ' text-gray-400'}>{label}</p>
      <p className="tnum mt-1 font-mono text-2xl font-semibold tracking-tight text-gray-900">
        {value}
        {unit && <span className="ml-1 text-sm font-normal text-gray-400">{unit}</span>}
      </p>
    </div>
  )
}

// The week board: seven days stacked downwards, every one of them drawn against the SAME
// 00:00 -> 24:00 axis, with the converters as lanes inside each day.
//
// Folding the week this way is what avoids an axis 168 hours long that scrolls sideways
// forever. It costs the ability to see a casting cross midnight as one bar (it is clipped at
// the edge of its charging day) and buys the thing a flat week cannot show: because the days
// are lined up on time-of-day, a gap that repeats - a shift change, a converter that always
// idles in the afternoon - reads as a column straight down the week.
//
// Laid out in percent, not pixels, which is what makes the zoom simple: the board is drawn
// zoom x 100% wide inside a scroll box, so at 100% a day is exactly one screen with nothing
// to scroll, and every step above that stretches the SAME percentages wider. The axis takes
// the hint and subdivides as it goes. The label columns are pinned so they survive the scroll.
function WeekBoard({ dates, bars, rows, hover, onHover, onLeave, zoom, scrollRef }) {
  const hours = dayHours(weekTickStep(zoom))
  // Zooming moves in on both axes: the day stretches sideways AND the lanes get taller, so
  // the castings actually come closer instead of just growing longer.
  const laneH = weekLaneHeight(zoom)
  const barH = weekBarHeight(zoom)
  // The casting numbers are all out or all away, decided by the zoom rather than by each
  // bar's own width - see showsCastingNo. Their size still follows the bar they sit in.
  const showNo = showsCastingNo(zoom)
  const noSize = Math.min(Math.max(Math.round(barH * 0.5), 9), 13)

  return (
    // Capped and scrolling in BOTH directions: zoomed right in the board runs well past a
    // screen, and it has to take that scroll itself rather than pushing the page around.
    <div ref={scrollRef} className="gantt max-h-[75vh] cursor-grab overflow-auto">
      <div className="relative" style={{ width: `${zoom * 100}%` }}>
      {/* The one time-of-day axis every day below is read against. Pinned to the top, so
          scrolling down through the week does not leave the days without their hours. */}
      <div className="sticky top-0 z-30 flex bg-panel-raised">
        <div
          className="sticky left-0 z-20 shrink-0 bg-panel-raised"
          style={{ width: WEEK_INSET }}
        />
        <div className="relative flex-1" style={{ height: AXIS_H }}>
          {hours.map((h) => (
            <div
              key={h}
              className="absolute bottom-1 border-l border-panel-line pl-1.5"
              style={{ left: `${(h / 24) * 100}%` }}
            >
              <span className="tnum font-mono text-[11px] font-semibold text-gray-600">
                {formatHour(h)}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Hover guides, the week view's answer to the day board's pair of lines.
          Here they run the FULL height of the board rather than stopping at the hovered
          bar - which is the fold paying off: every day shares one time-of-day axis, so a
          line at 08:15 marks that same moment on all seven days at once and you can read
          straight down it to see what the other days were doing.
          After the axis in DOM order so the labels sit over it, before the days so the
          castings paint over the lines instead of being crossed out by them.
          pointer-events-none, or it would swallow the hover it exists for. */}
      {hover && (
        <div
          // Above the pinned axis (z-30), or the time labels would be hidden behind it.
          // The lines themselves start below the axis, so only the labels overlap it.
          className="pointer-events-none absolute top-0 bottom-0 z-40"
          style={{ left: WEEK_INSET, right: 0 }}
        >
          {[hover.start, hover.end].map((time, i) => {
            // A casting tapped after midnight sits past the right edge of its own day, so
            // the guide is pinned to the edge rather than stretching the board wider.
            const at = Math.min(
              dayPercent(time.getTime(), dayWindow(dayOf(hover))),
              100,
            )
            return (
              <div key={i}>
                <div
                  className="absolute w-px bg-panel-muted"
                  style={{ left: `${at}%`, top: AXIS_H, bottom: 0 }}
                />
                <div
                  className="tnum absolute top-1 -translate-x-1/2 rounded bg-gray-950 px-1.5 py-0.5 font-mono text-[10px] font-semibold text-white"
                  // Held off the edges, so the first and last casting of a day do not push
                  // their own label out of the board
                  style={{ left: `${Math.min(Math.max(at, 2), 98)}%` }}
                >
                  {formatTime(time)}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {dates.map((date) => {
        const win = dayWindow(date)
        const day = new Date(win.from)
        const dayBars = bars.filter((b) => dayOf(b) === date)
        const figures = stats(dayBars)
        // Saturday and Sunday are tinted, so the week reads at a glance without a label.
        // A solid tone rather than a translucent one: the pinned label columns carry the
        // same surface, and a see-through one would let the bars slide underneath it.
        const surface =
          day.getDay() === 0 || day.getDay() === 6 ? 'bg-panel-raised' : 'bg-panel'

        return (
          // The rule between two DAYS is deliberately heavier than the ones between the
          // lanes inside a day. Zoomed in the two used to be near enough the same weight,
          // and a scrolled board turned into an undifferentiated stack of rows.
          <div key={date} className={'flex border-t-2 border-gray-300 ' + surface}>
            {/* The day, and what it came to. The stats hang here because it is where the
                eye lands before it reads across the lanes. */}
            <div
              // Centred rather than top aligned: zoomed in, a day block is several times
              // taller than its label and the text would otherwise float at the top of it.
              className={
                'sticky left-0 z-20 flex shrink-0 flex-col justify-center self-stretch border-r-2 border-gray-300 px-3 py-2 ' +
                surface
              }
              style={{ width: WEEK_DAY_W }}
            >
              <p className="text-[11px] font-semibold uppercase tracking-wide text-gray-500">
                {day.toLocaleDateString('tr-TR', { weekday: 'short' })}
              </p>
              <p className="tnum font-mono text-sm font-bold text-panel-ink">
                {day.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })}
              </p>
              <p className="tnum mt-0.5 font-mono text-[10px] leading-tight text-panel-muted">
                {figures.count > 0 ? (
                  <>
                    {figures.count} döküm
                    <br />
                    {formatDuration(figures.total)}
                  </>
                ) : (
                  '—'
                )}
              </p>
            </div>

            {/* One lane per converter, in the same order every day so the week reads
                downwards as well as across */}
            <div className="min-w-0 flex-1">
              {rows.map((c) => {
                const mine = dayBars.filter((b) => b.konverterNo === c.no)
                return (
                  <div key={c.no} className="flex border-t border-panel-line/50 first:border-t-0">
                    <div
                      className={
                        'sticky z-20 flex shrink-0 items-center gap-1.5 px-2 ' + surface
                      }
                      style={{ width: WEEK_KV_W, height: laneH, left: WEEK_DAY_W }}
                    >
                      <span className={'h-2 w-2 shrink-0 rounded-full ' + c.dot} />
                      <span className="font-mono text-[10px] font-bold text-panel-muted">
                        {c.label}
                      </span>
                    </div>

                    {/* overflow-hidden is what clips a casting that runs past midnight into
                        the next day; the tooltip still reports its true times. */}
                    <div
                      className="relative min-w-0 flex-1 overflow-hidden"
                      style={{ height: laneH }}
                    >
                      {hours.map((h) => (
                        <div
                          key={h}
                          className="absolute top-0 bottom-0 w-px bg-panel-line/70"
                          style={{ left: `${(h / 24) * 100}%` }}
                        />
                      ))}

                      {mine.map((b) => {
                        const left = dayPercent(b.start.getTime(), win)
                        const width = dayPercent(b.end.getTime(), win) - left
                        return (
                          <div
                            key={b.dokumId}
                            onMouseEnter={(e) => onHover(b, e)}
                            onMouseMove={(e) => onHover(b, e)}
                            onMouseLeave={onLeave}
                            className={
                              'gbar absolute top-1/2 flex -translate-y-1/2 items-center overflow-hidden rounded-[2px] transition-opacity ' +
                              c.bar
                            }
                            style={{
                              left: `${left}%`,
                              width: `${width}%`,
                              minWidth: 3,
                              height: barH,
                            }}
                          >
                            {/* The two milestones: scrap charging start and casting time */}
                            <span className="absolute -top-0.5 -bottom-0.5 left-0 z-10 w-0.5 bg-panel-ink" />
                            <span className="absolute -top-0.5 -bottom-0.5 right-0 z-10 w-0.5 bg-panel-ink" />
                            {showNo && (
                              <span
                                className="tnum w-full truncate px-1.5 text-center font-mono leading-none font-semibold text-white"
                                style={{ fontSize: noSize }}
                              >
                                {b.dokumNo}
                              </span>
                            )}
                          </div>
                        )
                      })}
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        )
      })}
      </div>
    </div>
  )
}

function Timeline() {
  const navigate = useNavigate()

  const username = localStorage.getItem('kullanici_adi')
  const selectedRole = localStorage.getItem('secili_rol')

  const [heats, setHeats] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filters, setFilters] = useState({ konverter: 'all', tarih: 'all' })
  // 'gun' draws one day on a scrollable hour axis; 'hafta' folds seven days onto a shared
  // time-of-day axis. Both read the same date - in the week view it stands for the week the
  // picked day falls in, so switching between the two never loses the user's place.
  const [mode, setMode] = useState('gun')
  const week = mode === 'hafta'
  // The board opens on the newest production day rather than on every day at once, which
  // would draw weeks of castings side by side. Only until the user picks for themselves -
  // after that a refresh must not drag them back to today.
  const dayChosen = useRef(false)
  // The hovered bar. The tooltip is drawn position:fixed at the cursor, so the horizontal
  // scroll container cannot clip it.
  const [hover, setHover] = useState(null)
  // Where the cursor is, and the tooltip node itself. Both refs rather than state: the
  // cursor moves sixty times a second and a render per move dragged the whole board -
  // every casting on it - through React. Only crossing INTO another bar costs a render now.
  const cursor = useRef({ x: 0, y: 0 })
  const tip = useRef(null)

  // Zoom: how wide an hour is drawn. Mouse wheel over the board, or the -/+ buttons.
  const [hourPx, setHourPx] = useState(HOUR_PX)
  // Height: how tall a converter channel is. Dragged with the grip under the board.
  const [rowH, setRowH] = useState(ROW_H)
  const scrollRef = useRef(null)
  // Where to put the scroll position back after a zoom, so the moment under the cursor
  // stays under the cursor instead of the board jumping somewhere else in the day.
  const anchor = useRef(null)
  // True while the board is being dragged. A ref, not state: it is read by event handlers
  // many times a second and none of those reads should cost a render.
  const dragging = useRef(false)

  // Put the tooltip where the cursor is, by hand. Held off the right edge so it does not
  // hang off the window.
  const placeTip = () => {
    if (!tip.current) return
    tip.current.style.left = Math.min(cursor.current.x + 16, window.innerWidth - 260) + 'px'
    tip.current.style.top = cursor.current.y + 16 + 'px'
  }

  // Every casting on both boards reports its hover through here, so the guard against
  // lighting up mid drag is written once. Moving WITHIN a bar only nudges the tooltip;
  // the state - and with it the render - changes only when a different casting is entered.
  const showHover = (bar, e) => {
    if (dragging.current) return
    cursor.current = { x: e.clientX, y: e.clientY }
    placeTip()
    if (bar.dokumId !== hover?.dokumId) setHover(bar)
  }

  // The tooltip is only in the DOM once hover has rendered it, so the first placement has
  // to happen after that - before the browser paints, or it flashes at the last position.
  useLayoutEffect(placeTip, [hover])

  // Both boards scroll behind pinned label columns, they are just pinned at different
  // widths. The anchor maths works off whichever is on screen.
  const inset = week ? WEEK_INSET : LABEL_W
  // The week board is laid out in percent and sits at exactly its container's width at 100%,
  // so zooming out below that has nothing to give. The day board keeps its usual floor.
  const zoomFloor = week ? HOUR_PX : MIN_HOUR_PX

  // factor > 1 zooms in. pointerX is measured from the left edge of the scroll box; without
  // one the middle of what is on screen is held instead.
  const zoom = (factor, pointerX) => {
    const el = scrollRef.current
    if (el) {
      // Read the anchor off the DOM rather than off render state, so this stays correct no
      // matter how many wheel events arrive before React re-renders.
      const board = el.scrollWidth - inset
      const at = pointerX ?? el.clientWidth / 2
      const ratio = board > 0 ? (el.scrollLeft + at - inset) / board : 0
      anchor.current = { ratio: Math.min(Math.max(ratio, 0), 1), at }
    }
    setHourPx((px) => clampZoom(px * factor, zoomFloor))
  }

  // Restore the anchor once the new width is on screen but before the browser paints it,
  // otherwise the board visibly jumps.
  useLayoutEffect(() => {
    const el = scrollRef.current
    if (!el || !anchor.current) return
    const { ratio, at } = anchor.current
    anchor.current = null
    el.scrollLeft = Math.max(ratio * (el.scrollWidth - inset) + inset - at, 0)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hourPx])

  // Grab the board and drag it, on either view. Attached by hand to the same ref the wheel
  // uses, so the day board and the week board both get it without either one knowing.
  //
  // Both axes move the BOARD's own scroll, never the window - dragging a chart must not
  // drag the page out from under it. The week board is capped in height for that reason, so
  // that zoomed in it scrolls inside itself instead of running off down the page.
  // screenX/Y rather than clientX/Y: scrolling underneath a drag changes what clientY means
  // mid gesture, and the drag would chase its own tail.
  useEffect(() => {
    const el = scrollRef.current
    if (!el) return
    let from = null

    const down = (e) => {
      // Left button, real mouse only. Touch and pen already pan the board natively and
      // fighting the browser for them buys nothing.
      if (e.pointerType !== 'mouse' || e.button !== 0) return
      e.preventDefault() // or the drag selects the casting numbers instead of panning
      from = { x: e.screenX, y: e.screenY, left: el.scrollLeft, top: el.scrollTop }
      el.setPointerCapture(e.pointerId)
    }

    const move = (e) => {
      if (!from) return
      const dx = e.screenX - from.x
      const dy = e.screenY - from.y
      // A few pixels of slack before it counts as a drag, so a plain click on a casting
      // does not blink its tooltip away.
      if (!dragging.current) {
        if (Math.abs(dx) + Math.abs(dy) < 4) return
        dragging.current = true
        setHover(null) // panning across the board must not light up everything it passes
        el.style.cursor = 'grabbing'
        el.style.userSelect = 'none'
      }
      el.scrollLeft = from.left - dx
      // A no-op on the day board, which never overflows vertically
      el.scrollTop = from.top - dy
    }

    const up = (e) => {
      from = null
      dragging.current = false
      el.style.cursor = ''
      el.style.userSelect = ''
      if (el.hasPointerCapture?.(e.pointerId)) el.releasePointerCapture(e.pointerId)
    }

    el.addEventListener('pointerdown', down)
    el.addEventListener('pointermove', move)
    el.addEventListener('pointerup', up)
    el.addEventListener('pointercancel', up)
    return () => {
      el.removeEventListener('pointerdown', down)
      el.removeEventListener('pointermove', move)
      el.removeEventListener('pointerup', up)
      el.removeEventListener('pointercancel', up)
    }
    // Same deps as the wheel below: anything that unmounts the board needs a fresh bind.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, error, heats.length, filters, mode])

  // The wheel over the board zooms - plain, no modifier needed. Shift is left alone so the
  // browser's own sideways scroll still pans the day.
  // Registered by hand because it has to be non passive: React's own onWheel cannot
  // preventDefault, and without that the page scrolls (or the browser zooms) instead.
  useEffect(() => {
    const el = scrollRef.current
    if (!el) return
    const onWheel = (e) => {
      if (e.shiftKey) return // panning, not zooming
      e.preventDefault()
      zoom(e.deltaY < 0 ? 1.1 : 1 / 1.1, e.clientX - el.getBoundingClientRect().left)
    }
    el.addEventListener('wheel', onWheel, { passive: false })
    return () => el.removeEventListener('wheel', onWheel)
    // mode and filters are in here because anything that unmounts the board - switching
    // view, or a selection that empties it - builds a fresh node on the way back, and the
    // old listener is not on that one.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, error, heats.length, filters, mode])

  // Dragging the grip under the board makes the channels taller or shorter. Pointer capture
  // keeps the drag alive when the cursor leaves the grip, so no window level listeners.
  const drag = useRef(null)

  const startResize = (e) => {
    e.preventDefault()
    e.currentTarget.setPointerCapture(e.pointerId)
    // Starts from the height on screen, not the one in state: if the board is already at its
    // ceiling the grip must not jump on the first pixel of the drag.
    drag.current = { y: e.clientY, rowH: rowHeight }
  }

  const onResize = (e) => {
    if (!drag.current) return
    // The drag moves the BOTTOM of the board, so the travel is shared between the channels
    const perRow = (e.clientY - drag.current.y) / Math.max(rows.length, 1)
    setRowH(clampRow(drag.current.rowH + perRow, maxRowH))
  }

  const endResize = (e) => {
    drag.current = null
    if (e.currentTarget.hasPointerCapture?.(e.pointerId)) {
      e.currentTarget.releasePointerCapture(e.pointerId)
    }
  }

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await apiFetch('/api/dokum/zaman-cizelgesi')
      if (!res.ok) {
        setError('Dökümler alınamadı')
        return
      }
      setHeats(await res.json())
    } catch (e) {
      if (e instanceof SessionError) return
      setError('Sunucuya bağlanılamadı')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const days = datesOf(heats)

  useEffect(() => {
    if (dayChosen.current || days.length === 0) return
    dayChosen.current = true
    setFilters((f) => ({ ...f, tarih: days[days.length - 1] }))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [heats])

  if (!hasSession()) {
    return <Navigate to="/login" replace />
  }

  const logout = async () => {
    await logoutRequest()
    navigate('/login', { replace: true })
  }

  // What the zoom means on the week board: a multiplier on the fitted width, so 100% is
  // exactly one screen and anything above it scrolls. The floor keeps it from going under.
  const weekZoom = hourPx / HOUR_PX

  // Until the picker has settled on the newest production day, stand in for it by hand -
  // otherwise switching to the week view before the data lands draws an empty board.
  const activeDate = filters.tarih === 'all' ? (days[days.length - 1] ?? 'all') : filters.tarih
  // Same date control, different question asked of it: one day, or the week it falls in.
  const filtered = applyFilters(
    heats,
    week ? { konverter: filters.konverter, hafta: activeDate } : filters,
  )
  const bars = toBars(filtered)
  const weekDays = week ? weekDates(activeDate) : []

  // Stepping between weeks. The arrows stop at the weeks the data actually covers, so there
  // is no walking off into empty months - "YYYY-MM-DD" compares correctly as a string.
  const canStep = (delta) => {
    if (days.length === 0 || activeDate === 'all') return false
    const target = weekOf(shiftWeeks(activeDate, delta))
    return delta < 0
      ? target >= weekOf(days[0])
      : target <= weekOf(days[days.length - 1])
  }

  const stepWeek = (delta) => {
    const next = shiftWeeks(activeDate, delta)
    if (next) setFilters((f) => ({ ...f, tarih: next }))
  }
  // Castings that cannot be placed on the timeline yet (no start and/or no casting time)
  const incomplete = filtered.length - bars.length

  const rows = CONVERTERS.filter(
    (c) => filters.konverter === 'all' || c.no === Number(filters.konverter),
  )

  // The window the castings actually cover (the axis itself has a few minutes of padding)
  const windowStart = bars.length > 0 ? new Date(Math.min(...bars.map((b) => b.start))) : null
  const windowEnd = bars.length > 0 ? new Date(Math.max(...bars.map((b) => b.end))) : null

  const { from, to } = bars.length > 0 ? axisRange(bars) : { from: 0, to: 0 }
  const width = ((to - from) / HOUR) * hourPx
  const x = (time) => ((time - from) / HOUR) * hourPx

  // How finely the axis is marked follows the zoom: three hour marks when the whole day is
  // on screen, down to quarter hours when zoomed right in.
  const ticks = bars.length > 0 ? timeTicks(from, to, tickInterval(hourPx)) : []
  // The board is never drawn taller than MAX_BOARD_H, however far the grip is dragged. With
  // three channels on screen that ceiling is what stops the drag; filtered down to one, the
  // per channel limit is. The dragged height is kept in state either way, so narrowing the
  // filter and widening it again gives back the height the user chose rather than the
  // clamped one.
  const maxRowH = maxRowHeight(rows.length)
  const rowHeight = clampRow(rowH, maxRowH)
  const barH = Math.round(rowHeight * BAR_RATIO)
  const shift = stats(bars)

  return (
    <div className="min-h-screen bg-shell">
      <header className="bg-brand-700 text-white shadow">
        <div className="mx-auto flex max-w-[1400px] items-center justify-between px-8 py-4">
          <h1 className="text-xl font-bold">İsdemir Döküm Paneli</h1>
          <div className="flex items-center gap-4">
            <div className="text-right leading-tight">
              <p className="font-semibold">{username}</p>
              <p className="text-sm text-brand-100">Rol: {selectedRole}</p>
            </div>
            <Button variant="secondary" size="sm" onClick={() => navigate('/dashboard')}>
              Panele Dön
            </Button>
            <Button variant="secondary" size="sm" onClick={logout}>
              Çıkış Yap
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-[1400px] px-8 py-8">
        {/* Title block: what this page is, and which slice of time it is showing */}
        <div className="flex flex-wrap items-end justify-between gap-4 border-b border-hairline pb-6">
          <div>
            <p className={EYEBROW + ' text-brand-700'}>Konverter İzleme</p>
            <h2 className="mt-1 text-3xl font-bold tracking-tight text-gray-900">
              Döküm Zaman Çizelgesi
            </h2>
          </div>
          {/* In the day view the window the castings actually cover; in the week view the
              week being shown, which is fixed whether or not every day of it ran. */}
          {week && weekDays.length > 0 ? (
            <p className="tnum font-mono text-sm text-gray-500">
              {new Date(dayWindow(weekDays[0]).from).toLocaleDateString('tr-TR', {
                day: '2-digit',
                month: '2-digit',
              })}{' '}
              —{' '}
              {new Date(dayWindow(weekDays[6]).from).toLocaleDateString('tr-TR', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
              })}
            </p>
          ) : (
            windowStart && (
              <p className="tnum font-mono text-sm text-gray-500">
                {formatDate(windowStart)} — {formatTime(windowEnd)}
              </p>
            )
          )}
        </div>

        {/* The shift, in four numbers. They follow the filter, like the chart does. */}
        {bars.length > 0 && (
          <div className="grid grid-cols-2 gap-6 border-b border-hairline py-6 sm:grid-cols-4">
            <Reading label="Döküm" value={shift.count} />
            <Reading label="Ortalama Çevrim" value={formatDuration(shift.average)} />
            <Reading label="En Uzun Çevrim" value={formatDuration(shift.longest)} />
            <Reading label="En Kısa Çevrim" value={formatDuration(shift.shortest)} />
          </div>
        )}

        {/* Filter rail */}
        <div className="flex flex-wrap items-center justify-between gap-4 py-6">
          <div className="flex flex-wrap items-center gap-x-6 gap-y-3">
            <div className="flex items-center gap-3">
              <span className={EYEBROW + ' text-gray-400'}>Konverter</span>
              <div className="flex overflow-hidden rounded-lg border border-hairline bg-white">
                {[
                  { value: 'all', label: 'Tümü' },
                  ...CONVERTERS.map((c) => ({ value: String(c.no), label: c.label })),
                ].map((option) => {
                  const active = filters.konverter === option.value
                  return (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => setFilters((f) => ({ ...f, konverter: option.value }))}
                      className={
                        'border-l border-hairline px-4 py-2 text-sm font-semibold transition-colors first:border-l-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 ' +
                        (active
                          ? 'bg-brand-700 text-white'
                          : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900')
                      }
                    >
                      {option.label}
                    </button>
                  )
                })}
              </div>
            </div>

            <div className="flex items-center gap-3">
              <span className={EYEBROW + ' text-gray-400'}>{week ? 'Hafta' : 'Tarih'}</span>
              {/* One control for both views. In the week view the arrows walk a week at a
                  time and the picker still jumps anywhere - the day picked stands for the
                  week it falls in, and the range being shown is spelt out in the heading. */}
              <div className="flex items-center overflow-hidden rounded-lg border border-hairline bg-white">
                {week && (
                  <IconButton
                    onClick={() => stepWeek(-1)}
                    disabled={!canStep(-1)}
                    label="Önceki hafta"
                  >
                    ‹
                  </IconButton>
                )}
                <input
                  type="date"
                  value={filters.tarih === 'all' ? '' : filters.tarih}
                  min={days[0]}
                  max={days[days.length - 1]}
                  // Clearing the field is ignored: with no "all days" option there would be
                  // no way back from an empty date, and the board would try to draw every day
                  // at once. The day only changes when a day is actually picked.
                  onChange={(e) =>
                    e.target.value && setFilters((f) => ({ ...f, tarih: e.target.value }))
                  }
                  aria-label={week ? 'Üretim haftası' : 'Üretim günü'}
                  className={
                    'tnum bg-transparent px-3 py-2 font-mono text-sm text-gray-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-500 ' +
                    (week ? 'border-x border-hairline' : '')
                  }
                />
                {week && (
                  <IconButton
                    onClick={() => stepWeek(1)}
                    disabled={!canStep(1)}
                    label="Sonraki hafta"
                  >
                    ›
                  </IconButton>
                )}
              </div>
            </div>

            <div className="flex items-center gap-3">
              <span className={EYEBROW + ' text-gray-400'}>Görünüm</span>
              <div className="flex overflow-hidden rounded-lg border border-hairline bg-white">
                {[
                  { value: 'gun', label: 'Gün' },
                  { value: 'hafta', label: 'Hafta' },
                ].map((option) => {
                  const active = mode === option.value
                  return (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => {
                        setMode(option.value)
                        // The two views have different zoom floors. Carried across unclamped,
                        // a day board zoomed right out would open the week board narrower
                        // than the space it has.
                        setHourPx((px) =>
                          clampZoom(px, option.value === 'hafta' ? HOUR_PX : MIN_HOUR_PX),
                        )
                      }}
                      className={
                        'border-l border-hairline px-4 py-2 text-sm font-semibold transition-colors first:border-l-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 ' +
                        (active
                          ? 'bg-brand-700 text-white'
                          : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900')
                      }
                    >
                      {option.label}
                    </button>
                  )
                })}
              </div>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <span className="hidden text-xs text-gray-400 lg:inline">
              Sürükle: kaydır · Tekerlek: yakınlaştır
            </span>
            {/* One zoom for both views, and 100% means "the view as it opens" in each: a
                readable hour on the day board, exactly one screen per day on the week board.
                The week floor is higher, which is the only thing that differs. */}
            <div className="flex items-center overflow-hidden rounded-lg border border-hairline bg-white">
              <IconButton
                onClick={() => zoom(1 / ZOOM_STEP)}
                disabled={hourPx <= zoomFloor}
                label="Uzaklaştır"
              >
                −
              </IconButton>
              <button
                type="button"
                onClick={() => zoom(HOUR_PX / hourPx)}
                title="Varsayılan görünüme dön"
                className="tnum border-x border-hairline px-3 py-2 font-mono text-xs font-semibold text-gray-600 hover:bg-gray-50 hover:text-gray-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
              >
                {zoomPercent(hourPx)}%
              </button>
              <IconButton
                onClick={() => zoom(ZOOM_STEP)}
                disabled={hourPx >= MAX_HOUR_PX}
                label="Yakınlaştır"
              >
                +
              </IconButton>
            </div>
            <Button variant="outline" onClick={load} disabled={loading}>
              {loading ? 'Yükleniyor...' : 'Yenile'}
            </Button>
          </div>
        </div>

        {/* The board */}
        <section className="overflow-hidden rounded-xl bg-panel shadow-sm ring-1 ring-hairline">
          {error && (
            <p className="px-6 py-5 text-sm text-red-600">{error}</p>
          )}
          {!error && loading && (
            <p className="px-6 py-5 text-sm text-panel-muted">Yükleniyor...</p>
          )}
          {!error && !loading && bars.length === 0 && (
            <p className="px-6 py-5 text-sm text-panel-muted">
              Bu seçimde çizelgeye alınacak döküm yok.
            </p>
          )}

          {!error && !loading && bars.length > 0 && week && (
            <WeekBoard
              dates={weekDays}
              bars={bars}
              rows={rows}
              hover={hover}
              onHover={showHover}
              onLeave={() => setHover(null)}
              zoom={weekZoom}
              scrollRef={scrollRef}
            />
          )}

          {!error && !loading && bars.length > 0 && !week && (
            <div ref={scrollRef} className="gantt cursor-grab overflow-x-auto">
              <div className="relative" style={{ width: LABEL_W + width }}>
                {/* Hour axis */}
                <div className="flex">
                  <div
                    className="sticky left-0 z-20 shrink-0 bg-panel-raised"
                    style={{ width: LABEL_W, height: AXIS_H }}
                  />
                  <div className="relative" style={{ width, height: AXIS_H }}>
                    {ticks.map((t) => {
                      const d = new Date(t)
                      // On the hour reads as the main mark; the marks between it are quieter
                      const onTheHour = d.getMinutes() === 0
                      return (
                        <div
                          key={t}
                          className="absolute bottom-1 border-l border-panel-line pl-1.5"
                          style={{ left: x(t) }}
                        >
                          <span
                            className={
                              'tnum font-mono text-[11px] ' +
                              (onTheHour ? 'font-semibold text-gray-600' : 'text-panel-muted')
                            }
                          >
                            {/* At midnight the date is shown, so a multi day board stays readable */}
                            {d.getHours() === 0 && onTheHour
                              ? d.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })
                              : formatTime(d)}
                          </span>
                        </div>
                      )
                    })}
                  </div>
                </div>

                {/* Hover guides: two lines rising from the edges of the hovered casting up
                    to the axis, each one labelled with the time it marks.
                    Rendered BEFORE the rows on purpose: both are positioned elements, so
                    DOM order decides what paints on top and the castings stay over the
                    lines instead of being crossed out by them. (A z-index here would undo
                    that.) pointer-events-none, or it would swallow the hover it exists for. */}
                {hover && (
                  <div
                    className="pointer-events-none absolute top-0"
                    style={{ left: LABEL_W, width }}
                  >
                    {[hover.start, hover.end].map((time, i) => {
                      const at = x(time.getTime())
                      const line = guideLine(
                        rows.findIndex((c) => c.no === hover.konverterNo),
                        rowHeight,
                        barH,
                      )
                      return (
                        <div key={i}>
                          <div
                            className="absolute w-px bg-panel-muted"
                            style={{ left: at, ...line }}
                          />
                          <div
                            className="tnum absolute top-0 rounded bg-gray-950 px-1.5 py-0.5 font-mono text-[10px] font-semibold text-white"
                            // Kept inside the board, so the first and last casting of the
                            // day do not push their label off the edge
                            style={{ left: Math.min(Math.max(at - 22, 0), width - 44) }}
                          >
                            {formatTime(time)}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}

                {/* One channel per converter */}
                {rows.map((c) => {
                  const mine = bars.filter((b) => b.konverterNo === c.no)
                  const own = stats(mine)
                  return (
                    <div key={c.no} className="flex">
                      <div
                        className="sticky left-0 z-20 flex shrink-0 flex-col justify-center gap-0.5 overflow-hidden border-t border-panel-line bg-panel-raised px-4"
                        style={{ width: LABEL_W, height: rowHeight }}
                      >
                        <div className="flex items-center gap-2">
                          <span className={'h-2.5 w-2.5 rounded-full ' + c.dot} />
                          <span className="font-mono text-sm font-bold text-panel-ink">
                            {c.label}
                          </span>
                        </div>
                        <p className="tnum pl-[18px] font-mono text-[11px] text-panel-muted">
                          {own.count} döküm · {formatDuration(own.total)}
                        </p>
                      </div>
                      <div
                        className="relative border-t border-panel-line"
                        style={{ width, height: rowHeight }}
                      >
                        {ticks.map((t) => (
                          <div
                            key={t}
                            className={
                              'absolute top-0 bottom-0 w-px ' +
                              (new Date(t).getMinutes() === 0 ? 'bg-panel-line' : 'bg-panel-line/50')
                            }
                            style={{ left: x(t) }}
                          />
                        ))}

                        {mine.map((b) => {
                          const left = x(b.start.getTime())
                          // A very short casting would otherwise be invisible
                          const barWidth = Math.max(x(b.end.getTime()) - left, 6)
                          return (
                            <div
                              key={b.dokumId}
                              onMouseEnter={(e) => showHover(b, e)}
                              onMouseMove={(e) => showHover(b, e)}
                              onMouseLeave={() => setHover(null)}
                              className={
                                'gbar absolute top-1/2 flex -translate-y-1/2 items-center overflow-hidden rounded-[3px] transition-opacity ' +
                                c.bar
                              }
                              style={{ left, width: barWidth, height: barH }}
                            >
                              {/* The two milestones: scrap charging start and casting time */}
                              <span className="absolute -top-1 -bottom-1 left-0 w-0.5 bg-panel-ink" />
                              <span className="absolute -top-1 -bottom-1 right-0 w-0.5 bg-panel-ink" />
                              <span className="tnum truncate px-2.5 font-mono text-xs font-semibold text-white">
                                {b.dokumNo}
                              </span>
                            </div>
                          )
                        })}
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>
          )}

          {/* The grip: drag it down to make the channels taller, up to shrink them.
              Pointer capture keeps the drag going once the cursor leaves the grip.
              Day view only - the week board's lanes are fixed so twenty one of them stay on
              one page. */}
          {!error && !loading && bars.length > 0 && !week && (
            <div
              role="separator"
              aria-orientation="horizontal"
              aria-label="Çizelge yüksekliğini ayarla"
              aria-valuenow={rowHeight}
              aria-valuemin={MIN_ROW_H}
              aria-valuemax={maxRowH}
              tabIndex={0}
              onPointerDown={startResize}
              onPointerMove={onResize}
              onPointerUp={endResize}
              onPointerCancel={endResize}
              onDoubleClick={() => setRowH(ROW_H)}
              onKeyDown={(e) => {
                if (e.key !== 'ArrowUp' && e.key !== 'ArrowDown') return
                e.preventDefault()
                setRowH(clampRow(rowHeight + (e.key === 'ArrowDown' ? 8 : -8), maxRowH))
              }}
              title="Sürükleyerek yüksekliği değiştirin, çift tıkla sıfırlayın"
              className="group flex cursor-ns-resize touch-none items-center justify-center border-t border-panel-line py-2 hover:bg-gray-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-500"
            >
              <span className="h-1 w-12 rounded-full bg-gray-300 transition-colors group-hover:bg-gray-400" />
            </div>
          )}
        </section>

        {/* Legend + the castings that could not be drawn */}
        <div className="flex flex-wrap items-center justify-between gap-4 pt-5">
          <div className="flex flex-wrap items-center gap-5 text-sm text-gray-600">
            {CONVERTERS.map((c) => (
              <span key={c.no} className="flex items-center gap-2">
                <span className={'h-3 w-6 rounded-[2px] ' + c.dot} />
                <span className="font-mono text-xs font-semibold">{c.label}</span>
              </span>
            ))}
            <span className="flex items-center gap-2 border-l border-hairline pl-5">
              <span className="h-4 w-0.5 bg-panel-ink" />
              <span className="text-xs">Hurda şarj başlama / Döküm zamanı</span>
            </span>
          </div>
          {incomplete > 0 && (
            <p className="text-xs text-gray-400">
              {incomplete} döküm zaman bilgisi eksik, çizelgeye alınmadı.
            </p>
          )}
        </div>
      </main>

      {/* Hover detail. position:fixed, so the scroll container cannot clip it. Deliberately
          dark: it floats over the board and has to separate from it at a glance. */}
      {hover && (
        <div
          ref={tip}
          className="pointer-events-none fixed z-50 rounded-lg bg-gray-900 px-3 py-2.5 text-xs text-white shadow-xl"
        >
          <p className="tnum font-mono text-sm font-bold">{hover.dokumNo}</p>
          <p className={EYEBROW + ' mt-0.5 text-gray-400'}>KV{hover.konverterNo}</p>
          <dl className="mt-2 grid grid-cols-[auto_auto] gap-x-4 gap-y-1">
            <dt className="text-gray-400">Hurda şarj</dt>
            <dd className="tnum font-mono">{formatDate(hover.hurdaSarjBaslamaZamani)}</dd>
            <dt className="text-gray-400">Döküm</dt>
            <dd className="tnum font-mono">{formatDate(hover.dokumZamani)}</dd>
            <dt className="text-gray-400">Süre</dt>
            <dd className="tnum font-mono font-semibold">
              {formatDuration(hover.end - hover.start)}
            </dd>
          </dl>
        </div>
      )}
    </div>
  )
}

export default Timeline
