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
| `feature-protection` | Pure protection state machine and foreground decision policy. | Phase 8a: done |
| `feature-protection-runtime` | Persists protection state, coordinates events and health, and registers navigation. | Phase 8b: done |
| `foundation-testing` | Supplies fake modules, clock sources, storage backends, and event test support. | Phase 1-7: done |

Old v0.1 enforcement code (`MainActivity`, `ZeroAccessibilityService`) and the old accessibility health probe removed. The app launches to the Zero shell, which currently protects Android Settings during a commitment.

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

## Protection limits

- A user can turn off the accessibility service when no commitment is running.
- A protected app may flicker briefly before the redirect.
- Protection depends on the accessibility service being connected. If it disconnects, nothing is blocked, and Diagnostics shows `DEGRADED`.
- Phase 8b uses a two-minute test commitment and protects only Android Settings; later phases replace these defaults.
