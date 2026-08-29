# route-watcher - Roadmap

## Status
Working prototype: add/edit/delete routes, schedule
automatic traffic checks, manual check via widget, notifications.

## Progress
1. Scaffold - empty compose app, it boots and runs
2. Localization pattern - values/ & values-de/, English fallback
3. UI shell - 3 screens (RouteList, AddEditRoute, Settings)
4. Local storage - working CRUD
5. Settings & API key - EncryptedSharedPreferences
6. First traffic check - Distance Matrix API
7. Alarm scheduling - AlarmManager.setAlarmClock(), enable/disable, notifications
8. Widget - home-screen "Check now" button

## Next up
Decide whether Distance Matrix route selection limitation
needs addressing before adding more features on top of it

## Known limitations / redesign considerations
- Distance Matrix API has no way to specify or pin which physical route
  a check runs against. Google can pick any different road between calls.
  Might have to switch to the Directions API (supports route alternatives + pinning via waypoints)

## Improvement backlog
- Replace raw hour/minute text fields in AddEditRouteScreen with a real time picker
- CheckNowActionReceivers "no API key" handling is awkward when zero routes are enabled
- Add a Quick Settings Tile as an alternative to the home screen widget
- General UI/visual polish needed