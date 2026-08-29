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
Switched to Routes API client (fetching alternate roads, checking traffic on hand-picked route)
Not used within the app yet. Next: a screen to actually pick a road.

## Known limitations / redesign considerations
- Replacing Distance Matrix entirely since it is deprecated and limited in functionality 

## Improvement backlog
- Replace raw hour/minute text fields in AddEditRouteScreen with a real time picker
- CheckNowActionReceivers "no API key" handling is awkward when zero routes are enabled
- Add a Quick Settings Tile as an alternative to the home screen widget
- General UI/visual polish needed