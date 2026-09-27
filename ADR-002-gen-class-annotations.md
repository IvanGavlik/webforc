# ADR-002: Expressing webforJ annotations with `gen-class` metadata, no Java shims

## Status

Accepted 27.09.2026.

## Context

webforJ discovers its app entry point and routes via **runtime-retained**
annotations 

* `@Routify`

* `@Route` 

* `@BundleEntry`

*  `@AppTheme`

*  `@AppProfile`

* `@AppEntry`
  
  

Clojure's `gen-class` is the standard way to produce a real, named `.class`
file that extends a Java superclass, but its docstring gives no indication
it supports class-level annotations, and unqualified attempts to attach one
silently do nothing.

## Options considered

1. **Thin Java stub per annotated class.** Write a ~15-line `.java` file
   carrying the annotation, extending the required superclass, delegating
   every method body to a Clojure var via `clojure.java.api.Clojure`. This
   is the standard, proven pattern for Clojure interop with
   annotation-driven Java frameworks 
   
   Costs: a `javac` build step, and one boilerplate
   file per annotated class forever (every new `@Route` view needs one).
2.  **`gen-class`  attach real annotations directly**
   removing the Java layer entirely.

## Decision

Option 3, confirmed by direct experimentation before writing any project
code. 

Clojure's `gen-class` **does** support class-level runtime
annotations — this is implemented (if undocumented in the public docstring)
in `clojure.core/add-annotations`, called from `clojure.core/gen-class` with
metadata read off the `:name` symbol:

```clojure
(ns webforc.app
  (:gen-class
   :name ^{com.webforj.annotation.Routify {:packages ["webforc.views"]}
           com.webforj.annotation.AppTheme {:value "system"}
           com.webforj.annotation.AppProfile {:shortName "WEBFORC" :name "webforc"}}
   webforc.Application
   :extends com.webforj.App
   :prefix "app-"))
```

## Gotcha found and fixed

Annotation-class symbols in that metadata **must be fully-qualified**
(`com.webforj.annotation.Routify`, not `Routify`), even when the class is
also brought in via a sibling `:import` in the same `ns` form. `gen-class`
resolves the metadata symbols while *expanding* the `ns` form, before the
`:import` clause has actually been evaluated — so an unqualified name
silently `resolve`s to `nil` and `add-annotations` just skips it, with no
error, warning, or exception anywhere. This produces a class that looks
fine, compiles fine, and is simply missing the annotation — diagnosed only
by inspecting the compiled `.class` file's constant pool directly
(`javap -verbose`) and noticing `attributes: 0`.

## Does this generalize to webforJ's other annotations?

The  ones that don't fit are three field-level annotations 

* `@Synchronize`

* `@PropertyExclude`

* `@PropertyMethods`



`gen-class` simply has no mechanism to declare an arbitrary annotated field, since it only auto-generates fields for its own internal dispatch machinery. 

But those only matter if you're building a brand-new low-level custom component
wrapper (`ElementComposite`-based), not ordinary routes/views, and webforJ
even documents a plain method-call alternative (`synchronize()`) for the
main one.

## Consequences

The generated `App`/`Composite` classes this pattern produces (originally
`webforc.Application`/`src/webforc/app.clj` and
`webforc.views.HelloWorldView`/`src/webforc/views/hello.clj`; now the
`webforc.example.*` files under `src/webforc/example/`, moved there once
the example was separated from the library proper) are pure Clojure —
zero `.java` files anywhere in the project.

A `gen-class` mistake unrelated to this annotation question — missing `:init`/`:post-init` declarations — caused a long-standing blank-screen bug in these same files; see ADR-003 for that
separate story.) 

`build.clj`/`tools.build` still exists, but for a narrower, still-unavoidable reason: `gen-class` onlywrites a real `.class` file during ahead-of-time `compile`, never during
plain `require`/`load`, so *something* must AOT-compile the `gen-class`
namespaces before ClassGraph's classpath scan can see them. That's a
property of `gen-class` itself, independent of the annotation question —
see ADR-004 for the build-automation follow-up this causes.

`gen-class` + fully-qualified annotation metadata is  a documented,
reusable pattern (see `README.md`) for any future annotated class this
project needs (e.g. a second `App` lifecycle listener via
`@AppListenerPriority`).
