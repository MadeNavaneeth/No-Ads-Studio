---
status: canonical
---

# Dependency Denylist

Required by Requirement 4 criterion 11. Coordinates a Studio_Project may not depend on, directly or
transitively.

**Matching rule:** case-insensitive comparison of the `group:module` segments, version ignored. An entry
naming a group id or a group/module prefix matches any coordinate beginning with that entry followed by
`.`, `:`, or end of string.

---

## Networking

| Entry | Note |
|---|---|
| `com.squareup.okhttp3` | HTTP client |
| `com.squareup.retrofit2` | REST client |
| `io.ktor:ktor-client` | multiplatform HTTP |
| `com.android.volley` | legacy HTTP |
| `org.apache.httpcomponents` | HTTP |
| `io.grpc` | RPC |
| `com.google.api-client` | Google API access |

## Advertising

| Entry | Note |
|---|---|
| `com.google.android.gms:play-services-ads` | AdMob |
| `com.applovin` | mediation |
| `com.unity3d.ads` | Unity Ads |
| `com.ironsource` | mediation |
| `com.facebook.android:audience-network-sdk` | Meta Audience Network |
| `com.mopub` | legacy mediation |

## Analytics

| Entry | Note |
|---|---|
| `com.google.firebase:firebase-analytics` | |
| `com.google.android.gms:play-services-analytics` | |
| `com.amplitude` | |
| `com.mixpanel.android` | |
| `com.segment.analytics.android` | |
| `com.appsflyer` | attribution |
| `io.branch.sdk` | attribution |

## Crash reporting

| Entry | Note |
|---|---|
| `com.google.firebase:firebase-crashlytics` | |
| `io.sentry` | |
| `com.bugsnag` | |
| `ch.acra` | |
| `com.microsoft.appcenter` | |

## Also barred, by the Anti_Requirements rather than by category

| Entry | Reason |
|---|---|
| `androidx.room` | Requires KSP annotation processing. Decision D7 uses DataStore instead. |
| `com.google.devtools.ksp` | Build-time cost on the Reference_Machine. |
| `androidx.navigation:navigation-compose` | Decision D8 hand-rolls navigation for three destinations. |
| `androidx.leanback` | TV support is unneeded bloat. |
| `com.google.dagger` | Dependency injection is unwarranted at this size. |

---

## Permitted despite looking heavy

Recorded so the checker is not argued with case by case.

| Coordinate | Why it is fine |
|---|---|
| `androidx.datastore:datastore-preferences` | The chosen persistence layer, D7. No processor. |
| `app.cash.paparazzi` | `testImplementation` only. JVM screenshot tests, never in a release artifact. |
| `com.lemonappdev:konsist` | `testImplementation` only. The architecture test of R15. |
| `io.gitlab.arturbosch.detekt` | Static analysis for the R15 forbidden-type gate. Build tooling, not a runtime dependency. |
| `com.vk.vkompose` | `detektPlugins` and Gradle plugin only. Blocks recomposition defects at compile time, making the `avoid.md` recomposition rule enforceable. See `ideas/learning-library.md` §6. |

Anything `testImplementation` or `detektPlugins` scoped is out of scope for the release artifact and
therefore out of scope for the denylist, provided it appears in no other configuration.
