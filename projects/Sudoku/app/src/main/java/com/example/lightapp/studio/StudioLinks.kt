package com.example.lightapp.studio

/**
 * External links for the studio shell. Decision: free forever, one support link.
 *
 * [DONATE_URL] is the "keep the lights on" link (site, coffee page, …) shown in
 * Settings → about. Blank until the site exists, and the row hides itself while
 * blank — a button to nowhere never ships. Opening it is a plain `ACTION_VIEW`
 * intent: no networking dependency, no permission, no SDK, so the offline gates
 * (R4) stay green. The app itself still makes zero network calls.
 */
object StudioLinks {
    const val DONATE_URL = ""
}
