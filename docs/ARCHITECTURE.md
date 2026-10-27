# Zero Architecture

## Phase 1 foundation layout

```text
:foundation-contract
  zero.foundation.contract
:foundation-registry
  zero.foundation.registry
:foundation-testing
  zero.foundation.testing
```

| Module | Purpose | Phase status |
|---|---|---|
| `foundation-contract` | Defines module identity, lifecycle, context, and status contracts. | Phase 1: done |
| `foundation-registry` | Explicitly registers modules and manages startup/shutdown order. | Phase 1: done |
| `foundation-testing` | Supplies fake modules and contract assertions for tests. | Phase 1: done |

## Dependency rules

- `foundation-contract` has no project dependencies.
- `foundation-registry` depends on `foundation-contract`; `foundation-testing` is test-only.
- `foundation-testing` exposes `foundation-contract` to test consumers.
- `app` depends on `foundation-registry`; no foundation module depends on `app`.
- Foundation modules are pure Kotlin/JVM and contain no Android imports.

## Not yet built

- Persistence
- Clock
- Events
- Permissions
- Logging
- Navigation
- Design
- Composition root wiring
