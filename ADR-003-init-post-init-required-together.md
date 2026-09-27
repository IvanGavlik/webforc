# ADR-003: `gen-class` requires `:init` and `:post-init` to be declared together, and what we built because of it

## Status

Accepted 27.09.2026.

> **Note:** this isn't a regular ADR — it doesn't weigh a design choice
> made up front. It documents a problem we ran into *while implementing*
> ADR-002 and the bigger API decision
> I am using the same ADR format for this because it's still a real decision
> worth recording (what went wrong, what we considered, what we picked),
> just one that came from hands-on implementation rather than upfront
> design.

## Context

ADR-002 was about the annotation part. This one is about building the
component's actual content.

In webforJ, a route's view is a `Composite` — a class that owns one real
UI component (say, a `FlexLayout`) and is responsible for filling it with
children (fields, buttons, labels, whatever the screen needs), once,
right after it's built. Every `Composite`-based view `webforc`
goes through this same construction step, so a mistake here doesn't
just affect one view — it affects the API.

### How `gen-class` connects a `Composite` to Clojure

`Composite` is an ordinary Java class from webforJ's point of view — it
has no idea Clojure exists. To make a real Java class out of Clojure code,
we use `gen-class`, which reads a `:gen-class` form and generates a
genuine `.class` file behind the scenes. Any method you want that
generated class to have — including "run this once, right after the
object is constructed" — has to be spelled out as one of `gen-class`'s
own options, in its own vocabulary, not just as an ordinary Clojure
function sitting nearby.

## Problem

`gen-class` has an option for exactly this, `:post-init`: name a function,
and it gets called once the object exists. But `gen-class` only actually
calls it if the form *also* has an `:init` key present — even an `:init`
you don't otherwise need. Forget `:init`, and `:post-init` is silently
never called. No error, no warning.


Design a public API surface in `webforc` that lets a client project:

1. Define their own webforJ `App` entry point and `Composite`/`Component`
   views, in their own Clojure code, without needing to already know the
   `gen-class` fully-qualified-annotation-symbol rule (ADR-002) or the
   `:init`/`:post-init` pairing rule (this ADR).
2. Get a working, rendering, correctly-routed app — including real
   multi-view routing (`/user`, `/login-page`, `/user/:id`, etc., all
   directly supported by webforJ's own route-pattern syntax).



## Options considered

1. **Document it and rely on remembering.** Cheapest, but exactly the kind
   of mistake that's easy to make once and costly to diagnose, since
   there's no error to point at the cause.
2. **Always declare `:init`/`:post-init` together, every time, as a fixed
   pattern.** `:init` becomes a trivial `[[] nil]` function whenever there's
   no real constructor-argument logic to speak of; `:post-init` does the
   actual work.


## Decision

Option 2 — but a fixed pattern to remember still isn't good enough for
`webforc` as a *library* (the goal ADR-001 set): a consumer writing their
own view has no reason to know this rule at all unless we make it
impossible to break. That reframes the fix from "a pattern we follow" to
"an API we hand consumers that follows it for them" — which is the same
gap ADR-002's fully-qualified-symbol rule leaves open too. Both needed
solving together.


### Decision on the API shape

`defapp`/`defview` macros, in `src/webforc/api.clj`.

- `defapp` takes a class symbol and an options map (`:packages`, `:theme`,
  `:profile`) and expands into a correctly-annotated `gen-class` form
- `defview` takes a class symbol, a route path string (`"/"`,
  `"/user/:id"`, anything webforJ's own pattern syntax supports — no
  translation layer, passed straight to `@Route`'s `value()`)
  It expands into a `gen-class` form that:
  - attaches `@Route(path)` (ADR-002 pattern).
  - always declares **both** `:init` and `:post-init` (this ADR's fix),
    so a consumer cannot make that mistake — there's no `:init`/
    `:post-init` key exposed in `defview`'s own argument list at all.
  - implements `com.webforj.router.observer.DidEnterObserver`, so the
    wire function is called exactly once, after the component is
    attached and the router has matched its path — with a plain Clojure
    `{String String}` map of any matched route parameters (via
    `webforc.api/params->map`, converting webforJ's `ParametersBag`),
    not a raw `Optional`-wrapped Java type.
  - derives all internal `defn` names (`:init`, `:post-init`,
    `:initBoundComponent`, the `onDidEnter` implementation) from the
    target class name, not a fixed string, so multiple `defview` calls in
    the same namespace don't clobber each other's generated functions.

```clojure
(defview myapp.views.UserView "/user/:id" (FlexLayout.)
  (fn [self params]
    (.add self (Label. (str "User " (get params "id"))))))
```


## Consequences

- Fixed structurally, not just documented, for anyone using `webforc` as a
  library: `defview` always declares both keys together, so an end user
  cannot make this mistake through the public API — only by writing a raw
  `gen-class` form themselves, the way `webforc`'s own code used to.
- `webforc` now has an actual public API (`webforc.api/defapp`,
  `webforc.api/defview`) instead of just documented knowledge. A
  consumer never writes `gen-class`, never needs to know the ADR-002
  fully-qualified-symbol rule, and cannot make this ADR's `:init`/
  `:post-init` mistake either.
- Route parameters are delivered as plain Clojure data (`{"id" "42"}`),
  not a raw webforJ `ParametersBag`/`Optional` — a deliberate ergonomics
  choice, consistent with "declarative Clojure, not Java shapes."
