# ADR-001: Hosting webforJ with embedded Jetty instead of Maven plugins

## Status

Accepted 27.09.2026.

## Context

[`webforJ`](https://webforj.com) is a Java framework for building server-rendered
web UIs: compose UI out of `Component` subclasses, and the framework pushes
DOM updates to the browser over a proprietary long-polling protocol
("BBj"/DWC).

Every reference project I looked into is a Maven `war`, hosted by
`jetty-ee11-maven-plugin` via a `pom.xml`.

## Problem

`webforc` should be **usable as a plain Clojure library**: another
project adds `webforc` to its own `deps.edn` (`:mvn/version`,
`:local/root`, or `:git/url`) and calls straight into it —
`(require 'webforc.core) (webforc.core/start MyApp 8080)` — the way any
ordinary Clojure library works.

Needed a way to run webforJ without Maven's help.

WebforJ's actual entry point, `com.webforj.servlet.WebforjServlet`,
is a plain servlet
* knows how to build the UI
* talks to the browser,
* can't listen on a port by itself.

Something has to start a web server, tell it to listen
on a port, and hand every incoming request to that servlet. Normally
that "something" is Maven's `jetty-ee11-maven-plugin`, which starts a
Jetty server for you as part of running `mvn`.

Since we'd already ruled Maven out (see below), we needed to do that
server-starting step ourselves, in Clojure.


### Why Maven specifically doesn't fit that goal

When another project depends on a library, it
only gets that library's compiled code (the `.class` files, packed into a
jar) plus whatever other libraries it needs. It does **not** get that
library's `pom.xml`, its `web.xml`, or any of its Maven plugin settings.

So if `webforc` were hosted the Maven way, a consumer wouldn't be able to
just add it as a dependency and use it. They'd have to copy `webforc`'s
`pom.xml`/`web.xml` into their own project and set up their own Maven
build to match — copying the project, not depending on it.


## Options considered

1. **Maven.** Keep a thin `pom.xml` just for the Jetty/webforJ-maven-plugin
   wiring, alongside `deps.edn` for the Clojure dependency graph.

   Pros:
   - Version pinning becomes Maven's problem again (`webforj-bom` import
     works normally), solved the same way every other webforJ project
     solves it.
   - No AOT-compile-before-run step to build ourselves (ADR-004) — Maven's
     `compile` phase already runs before anything else, every time.
   - Easier to compare against `testapp` when something behaves
     differently, since both would run the same way.

   Cons:
   - Still Maven in the loop — someone has to understand a `pom.xml`, even
     a thin one.
   - Two build tools instead of one (`deps.edn` and `pom.xml`), each with
     its own place to declare dependency versions, which can quietly drift
     out of sync with each other.
   - Doesn't fully satisfy "it should be deps" — you still need `mvn`
     installed, even if you never touch the `pom.xml` yourself.

   Rejected: none of those cons is really the deciding one — the actual
   dealbreaker is that this shape isn't usable by a downstream `:deps.edn`
   consumer at all (see "Why Maven specifically doesn't fit that goal"
   above).

2. **Embedded Jetty.** Embed a servlet container (Jetty, Tomcat, Undertow —
   any jakarta-servlet-6-compatible one) as a plain **library** dependency
   in `deps.edn`, and construct `Server`/`ServletContextHandler`
   programmatically. Concretely: `org.eclipse.jetty:jetty-server` +
   `org.eclipse.jetty.ee11:jetty-ee11-servlet`, pinned to `12.1.8` — the
   exact version `testapp`'s `jetty-ee11-maven-plugin` resolves to,
   confirmed against the locally cached `~/.m2` artifacts to avoid a
   version drift bug hiding in here later. `src/webforc/core.clj` builds a
   `Server`, a `ServletContextHandler`, wraps `WebforjServlet` in a
   `ServletHolder` (`.setInitOrder 1`, matching `testapp`'s `web.xml`
   `<load-on-startup>1</load-on-startup>` for eager init), and calls
   `.addServlet` at `"/*"`. No war, no `web.xml`.

   Pros:
   - Works as a real Clojure library — a consumer just adds `webforc` to
     their own `deps.edn` and calls `webforc.core/start` directly, no
     copying anything.
   - One build tool, one place dependency versions live (`deps.edn`).
   - No `mvn` required at all, ever — matches "it should be deps" exactly.

   Cons:
   - No BOM import in `deps.edn` — every webforJ module version has to be
     hand-pinned and kept in sync ourselves.
   - have to build our own AOT-compile-before-run step (`bin/dev`,
     ADR-004), instead of getting it for free from Maven.
   - Frontend bundling, hotswap, and live-reload aren't available for
     now.

   Accepted: this is the only shape that actually satisfies the Problem
   above (usable as a plain Clojure library) — see "Why Maven specifically
   doesn't fit that goal."

## Consequences

WebforJ needs a few files that Maven normally copies into `WEB-INF/classes`
I had to copy by hand into `resources/`
* `webforj.conf`
* `certificate.bls`/`blsclient.conf` — without the latter two, the app
doesn't crash, it just **hangs indefinitely** on startup retrying a
license-server connection, which cost real debugging time before we traced
it to the missing files.
