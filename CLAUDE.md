# Gantt Chart Timeline Feature Roadmap

## Goal

Implement a dedicated Gantt Chart page that allows users to visualize casting operations on a timeline.

Each casting should be displayed according to its production duration, allowing users to quickly identify:

- Which converter was used
- When the casting started
- When the casting finished
- The overall production timeline

The timeline should provide a clear overview of converter utilization throughout the day.

---

# Page Structure

Create a new page for the Gantt Chart instead of placing it on the main dashboard.

**Suggested Route**

```
/timeline
```

## Reason

A Gantt Chart requires significant horizontal space. Keeping it on a dedicated page provides:

- Better readability
- Cleaner dashboard layout
- Easier future expansion
- Better responsiveness
- Space for filters and additional timeline controls

---

# Timeline Layout

The page should contain three main sections.

## 1. Filter Panel

Located at the top of the page.

Initially support filtering by converter.

Filters:

- All Converters
- KV1
- KV2
- KV3

Changing the selected converter should immediately refresh the chart.

The filter panel should be designed so additional filters can easily be added in the future (such as date range, steel grade, status, etc.).

---

## 2. Gantt Timeline

The chart should contain one row for each converter.

Example:

```
Time -------------------------------------------------------------->

KV1 | ███████████████████
KV2 |        █████████████████
KV3 |                  ███████████
```

Each casting must appear only on the row of its assigned converter.

---

## 3. Legend

A small legend should explain:

- Converter colors
- Milestone indicators

This helps users understand the chart quickly.

---

# Timeline Duration

Each Gantt bar represents one casting.

The duration of a bar is determined by:

**Start**

Scrap Charging Start Time

**End**

Casting Time

Therefore,

```
Bar Duration = Scrap Charging Start Time → Casting Time
```

This represents the complete production interval for a casting.

---

# Important Milestones

Each Gantt bar should contain two important process markers.

## Milestone 1

Scrap Charging Start

## Milestone 2

Casting Time

These milestones should be represented by vertical indicator lines placed on the timeline bar.

Example:

```
████│────────────────────│████

    ^                    ^

 Scrap Start         Casting
```

These markers make important production stages immediately visible.

---

# Chart Data

Each timeline item should include at minimum:

- Casting Number
- Converter
- Scrap Charging Start Time
- Casting Time
- Start Timestamp
- End Timestamp

The data structure should remain flexible for future additions.

Possible future fields:

- Heat Number
- Steel Grade
- Operator
- Production Status

---

# Converter Colors

Use a consistent color for each converter.

Suggested mapping:

- KV1 → Blue
- KV2 → Green
- KV3 → Orange

Milestone indicators should use a dark color such as black or dark gray to remain clearly visible.

---

# Hover Information

When the user hovers over a Gantt bar, display a tooltip containing:

- Casting Number
- Converter
- Scrap Charging Start Time
- Casting Time
- Total Production Duration

This information should appear without navigating away from the chart.

---

# Filtering Behavior

The filter panel should initially support:

- All Converters
- KV1
- KV2
- KV3

Example:

All

```
KV1 ███████
KV2 █████████████
KV3 ██████
```

KV1

```
KV1 ███████
```

KV2

```
KV2 █████████████
```

KV3

```
KV3 ██████
```

The filtering logic should be implemented in a reusable way to simplify adding more filters later.

---

# UI Guidelines

The page should follow the existing project design.

Recommended layout:

```
-------------------------------------------------------
                Casting Timeline
-------------------------------------------------------

[ Converter ▼ ]

-------------------------------------------------------

                 Gantt Chart

-------------------------------------------------------

Legend
```

The page should remain clean and easy to understand.

Avoid excessive colors or unnecessary controls.

---

# Future Improvements

The architecture should remain flexible so new features can be added without major refactoring.

Possible future enhancements include:

- Date range filtering
- Search by Casting Number
- Filter by Production Status
- Filter by Steel Grade
- Zoom In / Zoom Out
- Horizontal scrolling
- Daily / Weekly / Monthly timeline views
- Real-time timeline updates
- Export to PDF
- Export to Excel
- Responsive mobile layout
- Clicking a bar to open detailed casting information

---

# Implementation Notes

- Create the Gantt Chart as a separate page (`/timeline`).
- Keep the filtering logic independent from the chart component.
- Build reusable timeline components to support future enhancements.
- Use a scalable architecture so additional converters or timeline events can be added easily.
- Prioritize readability and maintainability over visual complexity.