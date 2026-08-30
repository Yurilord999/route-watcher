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
9. Route picker - chose a specific road on the map (from up to 3 suggestions)

## Next up
Wire RoutesApiClient.checkTrafficOnRoute into TrafficCheckReceiver and CheckNowActionReceiver.
Fully retire DistanceMatrixClient, then, tackle the ViewModel refactor

## Known limitations / redesign considerations
- Replacing Distance Matrix entirely since it is deprecated and limited in functionality
- Route picker only offers Google's suggested alternatives (no custom waypoint/detour editing)
- Considering a ViewModel once app state outgrows RouteWatcherApp.kt (added 2 TODOs: "bandaid")

## Improvement backlog
- Replace raw hour/minute text fields in AddEditRouteScreen with a real time picker
- CheckNowActionReceivers "no API key" handling is awkward when zero routes are enabled
- Add a Quick Settings Tile as an alternative to the home screen widget
- General UI/visual polish needed