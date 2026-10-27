# Zero Architecture

## Foundation layout

```text
:foundation-contract
  zero.foundation.contract
:foundation-registry
  zero.foundation.registry
:foundation-clock
  zero.foundation.clock
:foundation-testing
  zero.foundation.testing
```

| Module | Purpose | Phase status |
|---|---|---|
| `foundation-contract` | Defines module identity, lifecycle, context, and status contracts. | Phase 1: done |
| `foundation-registry` | Explicitly registers modules and manages startup/shutdown order. | Phase 1: done |
| `foundation-clock` | Provides monotonic and wall-clock sources and boot reconciliation. | Phase 2: done |
| `foundation-testing` | Supplies fake modules, clock sources, and contract assertions for tests. | Phase 1-2: done |

## Dependency rules

- `foundation-contract` has no project dependencies.
- `foundation-registry` depends on `foundation-contract`; `foundation-testing` is test-only.
- `foundation-clock` has no main-code project dependencies; its tests use `foundation-testing`.
- `foundation-testing` exposes `foundation-contract` and `foundation-clock` to test consumers.
- `app` depends on `foundation-registry` and `foundation-clock`; no foundation module depends on `app`.
- Foundation modules are pure Kotlin/JVM and contain no Android imports.
- Android clock source adapters live in `app`; clock policy stays in `foundation-clock`.

## Not yet built

- Persistence
- Events
- Permissions
- Logging
- Navigation
- Design
- Composition root wiring
