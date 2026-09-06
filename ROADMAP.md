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
12. Custom waypoints - adding/editing/removing route detours in custom mode
13. Onboarding - first launch guide screen & API guide in settings
14. Expandable route list UI - day of the week icons, instant check, mini map view
15. Day of the week scheduling - Repeating alarms + quick delete/route check button
16. Minimap - an expandable, interactive map for each route. Shows traffic congestion.

## Next up
"Open in Google Maps" button for routes.
Notification & Widget pointers towards route details when jams are detected.
Add/Edit form UI for day of week scheduling + Route default day bugfix 

## Known limitations / redesign considerations
- Polyline has no native drag support. Editing route through dragging is out of scope for now. 
- Googles routing quality for nearby/short-distance custom stops is very bad.
  Probably limited routes API behavior. 
  Potential future workarounds: distance sanity checking a returned route before showing it?
- Add/edit form has considerable downsides: no route autocomplete, no visual feedback etc.
  Combining it with Places SDK for one-screen solution (visual route picking & autocomplete)  

## Improvement backlog
- Replace raw hour/minute text fields in AddEditRouteScreen with a real time picker
- Add a Quick Settings Tile as an alternative to the home screen widget
- Improved visibility (in app/widget ): on glance results
- Cap API requests per day/month as a safeguard within Googles free tier
- Marker drag is super clunky, takes too long. Needs a custom touch
  listener/hit testing layer (Maps SDK has no drag timing setting)
- Route list fold/unfold smooth sliding transition
- Long origin/destination address truncation required
- Bug: Collapse button for expanded routes is hidden bellow the screen, while scrolling is disabled
  due to minimap.
- General UI/visual polish needed