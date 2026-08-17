# Blue — native Kotlin build

Same design as the mockup, rebuilt as a real Android app in Kotlin/XML —
same toolchain as Sentinel, no Flutter involved.

## Before your first build: local.properties

Just like Sentinel, this needs a `local.properties` file pointing at your
Android SDK, and it's not included in the zip (same reason as Sentinel —
it has to be recreated after every fresh extract). Create a file named
`local.properties` in the project root (next to `settings.gradle.kts`)
containing:

```
sdk.dir=C:\\Android\\sdk
```

(Double backslashes, same as usual for this file.)

## Building and running

Same commands as Sentinel:

```
gradle assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

If your global `gradle` version doesn't match what's in `build.gradle.kts`
/ `app/build.gradle.kts` (Android Gradle Plugin 8.2.0, Kotlin 1.9.22), you
may need to adjust those version numbers to whatever's already working for
Sentinel — I don't have those exact versions from your machine, so copy
them over if the build complains about a version mismatch.

## What's in it
- `MainActivity.kt` — everything lives in one Activity, using a
  `ViewFlipper` to switch between screens, same pattern you already used
  in `SessionHomeActivity.kt` for Sentinel.
- `res/layout/view_onboarding.xml` — logo badge, headline, feature list,
  Get Started + call row.
- `res/layout/view_request.xml` — free-form kg `EditText`, Now/Schedule
  toggle wired to a real `TimePickerDialog`.
- `res/layout/view_track.xml` — stage list + "Simulate ping" reveals the
  rider-arrived banner.
- `res/layout/view_pay.xml` — account/bank details, "I've paid" →
  "Checking..." → checkmark "Payment received" (`Handler.postDelayed`,
  same idea as your countdown timers in Sentinel).
- `res/layout/view_done.xml` — "Confirm received" delivery confirmation.
- `res/layout/activity_main.xml` + `item_nav.xml` — the bottom nav bar,
  built as plain `LinearLayout`s (not `BottomNavigationView`), so nav
  items can be individually locked/unlocked as the order progresses.
- Icons are emoji for now (🔥📍🏦✓📞 etc.), not a vector icon set — same
  approach as keeping things simple and dependency-free. Swap these for
  real vector drawables later the same way you did for Sentinel's
  DC/QS tile icon.
- The Call nav item and the onboarding call row both use a real
  `Intent.ACTION_DIAL` — tapping them opens the dialer with the number
  pre-filled, not just a fake button.

## What's simulated, not real, yet
- Payment confirmation is a timed delay, not a real bank/webhook check.
- The rider-arrived notification is a manual button, not a real push
  notification.
- All state is in memory — closing the app resets everything. No backend.

## Natural next steps
- Swap the `Handler.postDelayed` in `setupPayScreen()` for a real
  payment-verification call once you have a backend.
- Wire the ping button to real push instead of the manual trigger.
- Build the rider/ops-side screen or tablet view for logging confirmed
  deliveries on your end.
