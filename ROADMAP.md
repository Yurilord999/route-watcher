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
10. Real traffic checks - scheduled and manual checks query Routes API now
11. ViewModel refactor - list, settings, add/edit etc. live in RouteViewModel now
    (manual ViewModelFactory, no DI library)

## Next up
Custom Waypoints

## Known limitations / redesign considerations
- Route picker only offers Google's suggested alternatives (no custom waypoint/detour editing)
- Googles routing quality for nearby/short-distance custom stops is very bad.
  Probably limited routes API behavior. 
  Potential future workarounds: distance sanity checking a returned route before showing it?

## Improvement backlog
- Replace raw hour/minute text fields in AddEditRouteScreen with a real time picker
- CheckNowActionReceivers "no API key" handling is awkward when zero routes are enabled
- Add a Quick Settings Tile as an alternative to the home screen widget
- Day of the week scheduling (+snooze button?)
- Improved visibility (in app / widget ): on glance results
- API key tutorial for dummies
- Cap API requests per day / month as a safeguard within Googles free tier 
- No API key set = every function silently fails. Onboarding required.
- No routes found screen (no API key set) is a dead end. Cancel/back button required
- Marker drag is super clunky, takes too long
- General UI/visual polish needed