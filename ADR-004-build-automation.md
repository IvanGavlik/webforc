# ADR-004: Automating the build step with two invocations, not one

## Status

Accepted (partial — see README TODO for what's left).

## Context

`gen-class` needs AOT compilation to produce a real `.class` file —
that's true regardless of annotations, it would be just as true for a
`gen-class` form with zero annotations on it (see ADR-002 for the
annotation-specific reasoning; this ADR is about the AOT requirement on
its own). Given that, how do you make sure that compile step always runs
before the app starts, without Maven's help? That's a `deps.edn`/Clojure
CLI tooling problem, not a `gen-class`-authoring one.


## Problem

Concretely: `clojure -T:build all` has to run before every
`clojure -M -m webforc.core`, and it's easy to forget — you'd edit
`app.clj`, run the app, and get a stale class silently. Maven never has
this problem: `compile` is an implicit, un-skippable phase before any
`run` goal. We wanted the equivalent here.


## Options considered

1. **One combined `-X`/`-T` command**: a function in `build.clj` that
   AOT-compiles, then `require`s and starts `webforc.core` in the same JVM
   process, so a single `clojure -X:build serve` does both. Tried first,
   since it's the more "native" Clojure-CLI-idiomatic shape.
2. **Two separate invocations**, wired together by a tiny wrapper script.

## Decision

Option 2 (`bin/dev`). We tried option 1 first, and ran into a problem
along the way.

An alias in `deps.edn` is supposed to only *add* dependencies, never take
any away. Didn't work that way: the moment our `:build` alias
(which just pulls in `tools.build`, to compile `gen-class` files) was
active, **all of our project's own dependencies — webforJ, Jetty,
everything — disappeared from the classpath**, not overridden, just gone.

I confirmed it wasn't specific to `webforc` by reproducing it in a small,
unrelated test project. Rather than dig further into why, decided to just avoid
combining `tools.build` with our own dependencies in the same command at
all.


## Consequences

`bin/dev` is a 3-line shell script:
`clojure -T:build all && exec clojure -M -m webforc.core "$@"` — two
separate JVM invocations, each using an alias in isolation exactly the way
they already work today, sidestepping the classpath issue entirely rather
than fixing it. Simple, boring, robust.

Not yet generalized into a reusable library/template for other Clojure +
webforJ projects to depend on instead of hand-copying this project's
`build.clj`/`deps.edn`/`bin/dev` shape — tracked in `README.md`'s
