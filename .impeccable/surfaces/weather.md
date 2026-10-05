# Weather screen

Mode: Operate. Native Android / Material 3. Build directly in Kotlin, as requested.

Concept seed: `728491e4`, scope `direction`, mode `operate`, assigned grounded candidate 6. Run completed before UI implementation.

FORM: A daily observation ledger translated into native Material 3: aligned readings, a selectable hourly timeline, and chronological daily rows. The reusable mechanic is selection of a temporal reading with its detailed measurements revealed inline.

## Direction contract

THESIS: A compact weather station: location, conditions, hours, then seven aligned daily readings on a continuous screen.

OWN-WORLD: Cool pale blue surfaces, deep blue ink, cobalt controls; warm sun icons. Dark mode uses ink-blue surfaces. Material type and controls; units stay beside readings.

STORY: Choose a city or current location, read conditions, scan today's hours and the week, add the same readings to the home screen.

FIRST VIEWPORT: City and update control at top, temperature and weather symbol, wind/UV/humidity strip, then a horizontally scrolling hourly timeline.

SIGNATURE: Hour selection reveals wind, rain probability and UV inline without navigation. The current hour is labelled «Сейчас» and selected by default; the selection follows the user's choice. Weather glyphs share one stroke system.

REACH: Wide screens arrange current conditions and forecasts in two columns; narrow screens stack. Loading preserves existing data; offline and outdated snapshots identify their age. Material sheets and system Back.

## Design evaluation

Grounded candidates: forecast bulletin; transit timetable; hiking weather log; home weather station; coastal instrument panel; daily observation ledger; daylight planner. Assigned candidate 6: daily observation ledger, adapted to native Material 3.

Catalog challengers were assessed against audience identification and forecast clarity. Emission-line rail: declined, retain precise temporal alignment. Vertical video feed: declined, retain visible weather context. Boarding pass and gate board: competitive on chronological clarity, retain aligned daily readings. Developer console: declined, retain separation of actions from data. Acetate manual: declined, retain structured sections. Crouwel grid: declined, retain disciplined alignment. Physical materials and custom navigation are omitted where native Android affordances take precedence.
