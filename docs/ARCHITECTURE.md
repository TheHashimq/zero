# Zero Architecture

## Foundation layout

```text
:foundation-contract
  zero.foundation.contract
:foundation-registry
  zero.foundation.registry
:foundation-clock
  zero.foundation.clock
:foundation-persistence
  zero.foundation.persistence
:foundation-runtime
  zero.foundation.runtime
:foundation-events
  zero.foundation.events
:foundation-health
  zero.foundation.health
:foundation-navigation
  zero.foundation.navigation
:foundation-design
  zero.foundation.design
:feature-protection
  zero.feature.protection
:feature-protection-runtime
  zero.feature.protection.runtime
:foundation-testing
  zero.foundation.testing
```

| Module | Purpose | Phase status |
|---|---|---|
| `foundation-contract` | Defines module identity, lifecycle, context, and status contracts. | Phase 1: done |
| `foundation-registry` | Explicitly registers modules and manages startup/shutdown order. | Phase 1: done |
| `foundation-clock` | Provides monotonic and wall-clock sources and boot reconciliation. | Phase 2: done |
| `foundation-persistence` | Provides namespaced key-value storage behind a backend interface. | Phase 3: done |
| `foundation-runtime` | Supplies services passed to modules at startup. | Phase 4: done |
| `foundation-events` | Append-only event log, in-process bus, and single record entry point. | Phase 5: done |
| `foundation-health` | Component health reports and permission state. | Phase 6: done |
| `foundation-navigation` | Menu entries contributed by modules, grouped by category. | Phase 7: done |
| `foundation-design` | Color palette tokens and status roles. | Phase 7: done |
| `feature-protection` | Pure protection state machine, foreground decision policy, and one-way commitment minimum rules. | Phase 8a-10a: done |
| `feature-protection-runtime` | Persists protection state, the add-only protected-app list, and the commitment minimum; coordinates events and health and registers navigation. | Phase 8b-10b: done |
| `foundation-testing` | Supplies fake modules, clock sources, storage backends, and event test support. | Phase 1-7: done |

Old v0.1 enforcement code (`MainActivity`, `ZeroAccessibilityService`) and the old accessibility health probe removed. The app launches to the Zero shell; users select launchable apps to protect.

## Dependency rules

- `foundation-contract` has no project dependencies.
- `foundation-registry` depends on `foundation-contract`; `foundation-testing` is test-only.
- `foundation-clock` has no main-code project dependencies; its tests use `foundation-testing`.
- `foundation-persistence` exposes `foundation-contract` and uses `foundation-testing` only in tests.
- `foundation-events` exposes `foundation-contract`, `foundation-clock`, and `foundation-persistence`; its tests use `foundation-testing`.
- `foundation-health` exposes `foundation-contract` and `foundation-clock`; its tests use `foundation-testing`.
- `foundation-navigation` exposes `foundation-contract`; its tests use `foundation-testing`.
- `foundation-design` is pure Kotlin with no project dependencies.
- `foundation-runtime` exposes the foundation services, including `foundation-health` and `foundation-navigation`; its tests use `foundation-testing` and `foundation-registry`.
- `feature-protection` is pure Kotlin and has no project dependencies.
- `feature-protection-runtime` exposes `feature-protection` and `foundation-runtime`; its tests use `foundation-testing` and `foundation-registry`.
- `foundation-testing` exposes `foundation-contract`, `foundation-clock`, `foundation-persistence`, and `foundation-events` to test consumers.
- `app` depends on the foundation and protection runtime modules; no foundation or feature module depends on `app`.
- Foundation modules are pure Kotlin/JVM and contain no Android imports.
- Android clock and persistence adapters live in `app`; foundation policies stay in their JVM modules.
- `ZeroApplication` is the composition root, starts the registry once per process, and freezes navigation after startup.
- The protection service observes window-state package names only; it does not request window contents.

## Not yet built

- Logging

## Pending verification

- Phase 4 device checks: startup count, boot-state sequence, and self-check errors remain unconfirmed.
- Phase 7 device checks: shell drawer, Diagnostics health display, and Logcat launch lines remain unconfirmed.
- Phase 9 emulator checks: launcher-app discovery, add confirmation, persistent selection, protected-app count, and foreground redirect were verified.
- Phase 10 emulator checks: Q1-Q3 and Q5 were verified. The UI omits the current minimum from its raise options, so same-value behavior was verified by unit tests rather than a second UI tap. Reboot and service-disable checks still require extended manual verification.

## Protection limits

- A user can turn off the accessibility service when no commitment is running.
- A protected app may flicker briefly before the redirect.
- Protection depends on the accessibility service being connected. If it disconnects, nothing is blocked, and Diagnostics shows `DEGRADED`.
- The protected-app list is persistent and add-only. Malformed stored entries are ignored; there is no removal or clearing UI.
- The picker shows app labels only, excludes Zero, and lists only apps with launcher activities visible to the app.
- Entries are package identifiers and are retained if an app is uninstalled; a missing app is shown by its package name.
- The commitment minimum starts at 90 minutes and can only be raised, up to 180 minutes. The Protection screen offers no way to lower it, so raising it by mistake cannot be undone in the app.
- A missing stored minimum means 90 minutes. An unreadable or out-of-range stored value fails closed at 180 minutes; this is deliberate and cannot be recovered inside the app.
- Each commitment uses the current minimum; no separate duration can be selected.
- Phase 8b used a two-minute test commitment; Phase 10 replaces it with the committed minimum.
