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
| `foundation-testing` | Supplies fake modules, clock sources, storage backends, and event test support. | Phase 1-5: done |

## Dependency rules

- `foundation-contract` has no project dependencies.
- `foundation-registry` depends on `foundation-contract`; `foundation-testing` is test-only.
- `foundation-clock` has no main-code project dependencies; its tests use `foundation-testing`.
- `foundation-persistence` exposes `foundation-contract` and uses `foundation-testing` only in tests.
- `foundation-events` exposes `foundation-contract`, `foundation-clock`, and `foundation-persistence`; its tests use `foundation-testing`.
- `foundation-runtime` exposes `foundation-contract`, `foundation-clock`, `foundation-persistence`, and `foundation-events`; its tests use `foundation-testing` and `foundation-registry`.
- `foundation-testing` exposes `foundation-contract`, `foundation-clock`, `foundation-persistence`, and `foundation-events` to test consumers.
- `app` depends on `foundation-registry`, `foundation-clock`, `foundation-persistence`, `foundation-runtime`, and `foundation-events`; no foundation module depends on `app`.
- Foundation modules are pure Kotlin/JVM and contain no Android imports.
- Android clock and persistence adapters live in `app`; foundation policies stay in their JVM modules.
- `ZeroApplication` is the composition root and starts the registry once per process.

## Not yet built

- Permissions
- Logging
- Navigation
- Design
