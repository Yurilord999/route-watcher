package com.routewatcher.app.ui

// Pure helper for turning a map picked route into flat string format
// (for RouteEntity matching decode)

fun encodeWaypoints(waypoints: List<Pair<Double, Double>>): String =
    waypoints.joinToString(";") { "${it.first},${it.second}" }