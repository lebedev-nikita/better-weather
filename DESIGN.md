---
name: Better Weather
description: Native weather readings with inline hourly detail.
colors:
  primary: "#235BCD"
  on-primary: "#FFFFFF"
  primary-container: "#DAE7FF"
  on-primary-container: "#173C78"
  secondary-container: "#E4EDFA"
  on-secondary-container: "#243E5D"
  sun: "#805400"
  surface: "#FFFFFF"
  ink: "#152A46"
  surface-variant: "#EAF0FA"
  secondary-ink: "#445D79"
  divider: "#CDD9E9"
  dark-primary: "#A9C7FF"
  dark-on-primary: "#10346D"
  dark-primary-container: "#294974"
  dark-on-primary-container: "#E0ECFF"
  dark-secondary-container: "#253A52"
  dark-on-secondary-container: "#DDEBFB"
  dark-sun: "#FFCF78"
  dark-surface: "#101C2C"
  dark-ink: "#E5EFFF"
  dark-surface-variant: "#1C2D43"
  dark-secondary-ink: "#B7CAE1"
  dark-divider: "#354A64"
  widget-surface: "#FFFFFF"
  dark-widget-surface: "#192A40"
typography:
  display:
    fontSize: "88sp"
    fontWeight: 300
    lineHeight: "92sp"
    letterSpacing: "-3sp"
  title-large:
    fontWeight: 600
  title-medium:
    fontWeight: 600
rounded:
  reading: "12dp"
  panel: "16dp"
spacing:
  compact: "4dp"
  small: "8dp"
  medium: "12dp"
  inset: "16dp"
  page: "20dp"
  section: "24dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
  button-text:
    textColor: "{colors.primary}"
  outlined-search:
    textColor: "{colors.ink}"
  top-app-bar:
    backgroundColor: "{colors.surface}"
  metric-panel:
    backgroundColor: "{colors.surface-variant}"
    rounded: "{rounded.panel}"
    padding: "16dp"
  hour-selected:
    backgroundColor: "{colors.primary-container}"
    rounded: "{rounded.reading}"
    width: "76dp"
    padding: "14dp 6dp"
  hour-detail:
    backgroundColor: "{colors.secondary-container}"
    rounded: "{rounded.reading}"
    padding: "14dp"
---

# Design System: Better Weather

## Overview

**Creative North Star: "The Daily Observation Ledger"**

The implemented interface presents weather as aligned readings: a large current temperature, grouped measurements, a selectable hourly sequence, and chronological daily rows. Pale blue panels and blue ink give the data a quiet hierarchy; the warm accent identifies clear daytime weather.

This is a description of the shipped native implementation, not a user-approved brand identity. Material 3 supplies Android controls, interaction states, and platform typography. The descriptive name and all visual defaults were implementation decisions. The source files below remain authoritative; native measurements use dp and sp.

**Key Characteristics:**
- Readings and their units stay together.
- Selection reveals detail inline.
- Light and dark schemes use paired semantic roles.
- Tonal panels group information without decorative shadows.

## Colors

A white application and widget background in light mode supports cool blue controls and a restrained warm weather accent. The dark scheme follows the system setting.

### Primary
- **Cobalt / pale blue:** `primary` colors actions, measurement icons and rain probabilities; `primary-container` marks the selected hour; native filled buttons use `on-primary`.

### Secondary
- **Detail blue:** `secondary-container` holds the selected hour's details, with supporting text in `on-secondary-container`.

### Tertiary
- **Sun amber:** `sun` maps to Material `tertiary`, reserved for semantic accents. Weather illustrations keep their upstream colors: amber sun, pale blue clouds, blue rain, and a cool moon; they are not tinted by the theme.

### Neutral
- **White field / blue ink:** `surface` and `ink` map to Material `surface` and `onSurface`; `surface-variant` groups current measurements. `secondary-ink` supplies metadata and labels; `divider` separates daily and city-result rows.
- **Widget field:** `widget-surface` is the RemoteViews background. Its text and accent share the application scheme's corresponding values.
- **Dark counterparts:** each `dark-*` entry is the corresponding system-dark role. The app selects these through `isSystemInDarkTheme()`; widget resources select them through `values-night`. Wallpaper-derived Dynamic Color is not implemented. Error colors retain Material defaults rather than defining a custom error palette.

**The Role Pair Rule.** Resolve application colors through `MaterialTheme.colorScheme`; resolve widget colors through resource qualifiers. Keep the corresponding foreground and background roles together when adding states.

## Typography

**Display Font:** Android's default sans serif, used as a numerical readout.
**Body Font:** the same platform family through Material 3 `Typography()`; no font asset or branded display family is configured.

The type ramp comes from Material roles. Only the temperature display and three semibold overrides depart from the library defaults; there is no custom ratio-based scale.

### Hierarchy
- **Display:** `displayLarge` uses the frontmatter values for current temperature only. Its lighter weight separates the reading from semibold titles.
- **Headline:** `headlineSmall` introduces search and empty states. `headlineLarge` has a semibold override in the theme but is not used on the shipped screen, so it is not a reusable token here.
- **Title:** `titleLarge` labels the city and forecast sections; `titleMedium` labels conditions and readings. Both are semibold. `titleSmall` identifies each day using the Material default.
- **Body:** `bodyMedium` carries descriptions and guidance; `bodySmall` carries dates, age, unit context and detailed readings. Sizes and line heights remain Material defaults.
- **Label:** `labelMedium` identifies metrics and hour labels; `labelSmall` carries precipitation percentages; `labelLarge` identifies the selected hour detail.
- **Widget:** RemoteViews uses a light sans serif temperature (24–40sp, sized to fit its space), bold city (16sp), readings (14sp), condition (12sp), and measurement labels/age (11sp). These are widget-local adaptations, not replacements for the application type roles.

**The Reading Role Rule.** Use the existing Material role for a new datum or label; reserve the oversized light display for the current temperature. Keep text in sp so Android font scaling applies.

## Layout

The application is a single vertically scrolling surface beneath a Material top app bar. `Scaffold` supplies content insets; the app bar owns the status-bar inset. Page insets are 20dp horizontally, with 24dp between major stacked sections and at the bottom. Local component spacing remains denser than section spacing.

At available content width **840dp**, current conditions form the left column, and hourly/week forecasts form the right. Column weights are 0.9:1.3 with a 36dp gap. Below that threshold the sections stack. This is a content layout breakpoint, not a navigation breakpoint; the app has one main destination and no bottom navigation.

Hours scroll horizontally with an 8dp gap. Measurement cells use a wrapping `FlowRow` with at most three items per row. Day rows align their dates on the left, condition and precipitation in the middle, and high/low readings on the right.

Search uses a fully expanded Material bottom sheet with IME padding. Results scroll within a maximum 340dp list. A reserved 4dp progress-indicator slot in the screen and search prevents the loading indicator itself from moving adjacent content. Error or status messages can still increase the surrounding area's height.

The launcher widget resizes in both directions, declares a 240 × 150dp minimum, and targets a 4 × 2 launcher cell area. Launcher sizing remains host-controlled. City and update time share the header with a 48dp refresh target. The body places artwork on the left, temperature/conditions in the middle, and wind/UV readings aligned at the right. Wind direction is part of its label so the speed and unit stay together.

## Elevation & Depth

Custom data panels are flat `Surface` containers: grouping comes from tonal fills and whitespace, with no custom shadow or elevation parameters. Daily rows and search results use Material dividers. The top app bar, sheet, buttons, field and snackbar retain Material's built-in depth and interaction behavior; the app defines no bespoke elevation or animation vocabulary.

**The Tonal Group Rule.** Use a tonal container for an actual measurement group or selected detail, and a divider for repeated chronological rows. Preserve Material elevation for native overlays.

## Shapes

Rounded readings and status/detail containers share the small corner (12dp); measurement groups share the panel corner (16dp). The launcher widget has its own larger corner (24dp). Buttons, the outlined field and the modal sheet retain Material shapes. Controls and metric symbols use Material outlined vectors; the widget refresh icon is an Android vector drawable. Weather conditions use bundled, ready-made Weather Icons illustrations from Makin-Things / amCharts. These retain their original colors and are rendered as transparent lossless WebP assets: 112dp for current conditions, 40dp for hours and days, 48dp in the widget. Day/night artwork follows forecast `isDay`; unknown conditions display «—» instead of a misleading cloud. Source SVGs, conversion and licensing are in [third_party/weather-icons](third_party/weather-icons/README.md).

## Components

### Buttons

Native Material `Button`, `TextButton` and `IconButton` provide interaction states. Filled buttons initiate city choice in the empty state; text buttons provide location, retry and attribution actions. Refresh is an icon button in the app bar. Busy state disables conflicting location/city/refresh actions. Default component paddings and shapes are inherited; selected text actions remove horizontal content padding. No custom hover or focus treatment is configured.

### Cards / Containers

The metric panel uses `surfaceVariant`, and inline hour detail uses `secondaryContainer`. Apply the documented small/panel corners and existing internal spacing. Error feedback uses a small-corner Material `errorContainer` with `onErrorContainer` and an inline retry action.

### Inputs / Fields

City search uses a full-width, single-line Material `OutlinedTextField`, with a persistent city label, example placeholder, search leading icon and conditional clear trailing action. Focus, cursor and stroke behavior come from Material. Search errors appear below the field with retry; they are not represented as a custom field-error style.

### Navigation

The top app bar pairs the selected city with a dropdown affordance and refresh. Selecting the city opens the search sheet; city choice or foreground geolocation dismisses it before loading. The Material sheet handles dismissal. The widget body opens the app while its separate refresh control updates data. There are no tabs, chips, navigation rail or navigation bar in the shipped system.

### Hourly reading and inline detail

Each hour is a 76dp-wide rounded selectable surface containing time, a weather illustration, temperature and precipitation. Selection uses `primaryContainer`; unselected hours use `surface`. The accessible description includes time, temperature, rain probability and selection. Current time is labelled «Сейчас»; initial selection chooses the current hour, falling back to the first available reading. User choice persists for the selected place and local day. The selected hour's wind, precipitation and UV appear directly below the timeline.

### Daily reading

Flat rows retain the same three-column reading order and separate days with `outlineVariant`. The left column expands, while right-aligned high/low temperatures stay together. Labels use «Сегодня», «Завтра», then localized weekday names; dates remain visible below them.

### Launcher widget

A compact RemoteViews panel presents city, refresh, temperature, condition with the same weather illustration as the app, wind, UV and data age. The refresh target is 48dp square. Its background and foreground adapt through day/night resources; it does not embed Compose or a separate forecast navigation system.

## Do's and Don'ts

### Do:
- **Do** use Material semantic roles and the paired light/dark widget resources.
- **Do** keep units, condition descriptions and data age beside their readings.
- **Do** reveal selected-hour measurements inline and preserve the selected place/day state.
- **Do** reuse the native Material controls, system insets and IME-aware search sheet.
- **Do** retain downloaded readings during refresh and identify offline or old data with visible text.

### Don't:
- **Don't** replace Material type roles with a new per-screen size scale.
- **Don't** add shadows to flat reading panels or replace chronological rows with decorative cards.
- **Don't** hardcode light colors into new Compose components.
- **Don't** turn implementation defaults or the descriptive North Star into a claim of approved branding.

Not canonized: the widget status text's literal «↻» is a text cue, not part of the vector icon system; unspecified Material defaults and the unused headline override are not promoted to custom brand tokens.

Source evidence: [WeatherTheme.kt](app/src/main/java/dev/betterweather/ui/WeatherTheme.kt), [WeatherScreen.kt](app/src/main/java/dev/betterweather/ui/WeatherScreen.kt), [WeatherIcon.kt](app/src/main/java/dev/betterweather/ui/WeatherIcon.kt), [widget layout](app/src/main/res/layout/weather_widget.xml), [widget colors](app/src/main/res/values/colors.xml), [night colors](app/src/main/res/values-night/colors.xml), [widget background](app/src/main/res/drawable/widget_background.xml), [widget sizing](app/src/main/res/xml/weather_widget_info.xml). Surface direction remains in [.impeccable/surfaces/weather.md](.impeccable/surfaces/weather.md).
