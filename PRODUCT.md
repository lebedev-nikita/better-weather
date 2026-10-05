# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Kotlin is required by the user. Jetpack Compose, Material 3, and WorkManager are implementation choices.

## Product Purpose

Weather at a glance on the home screen, with hourly forecasts for the current day and a forecast for seven days in the application.

## Capabilities and Constraints

- Widget: temperature, wind, UV index.
- Application: today's hourly forecast and the coming week.
- Use free weather APIs.
- Keep testing focused; the user explicitly asks not to overdo tests.
- Repository name: better-weather.

## Assumptions and Open Decisions

- Assumed personal, noncommercial use; Open-Meteo's free endpoint has this restriction.
- Russian UI and Celsius / meters per second are inferred from the request.
- The user confirmed city search and optional foreground geolocation.
- The user confirmed building the working interface directly in code.
- Tbilisi is an editable first-launch default inferred from the workspace timezone, not a detected location.
- Primary use is checking the forecast before going out; no account is needed.

## Product Principles

- Show current weather and forecasts without nested navigation.
- Retain downloaded data and make its age visible when offline.
- Request location only after an explicit user action.
