package com.routewatcher.app.network

// Places Autocomplete billing mode toggle

// USE_LEGACY = true (Legacy Places billing):
//   An Autocomplete session is billed as a whole and the entire session becomes free once it's
//   terminated by any fetchPlace() call. A free "ID Refresh" lookup right after a prediction is selected.

// USE_LEGACY = false (New Places API billing):
//   Every Autocomplete keystroke is billed individually, since this app never calls Place Details
//   for its own data (it already gets the address text straight from the prediction).

object PlacesBillingConfig {
    const val USE_LEGACY = true
}