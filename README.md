# webforc

`webforc` is a Clojure wrapper for [webforJ](https://webforj.com) — a Java
framework for building server-rendered web UIs. It lets you write a webforJ
app entirely in Clojure, just `deps.edn` and an embedded Jetty server.


## Running

```sh
./bin/dev [port]   # always rebuilds classes/, then runs. Defaults to 8080.
```
Then open `http://localhost:8080/`.

Or

```sh
clojure -T:build all                # AOT-compiles the gen-class namespaces
                                    # into classes/
clojure -M -m webforc.core [port]   # defaults to 8080
```

For a plain REPL (no AOT needed first — `require`/`load` build a
`gen-class`'d class in memory just fine for REPL purposes;

```sh
clojure -M:repl
```

## Demo / example app

`./bin/dev` (above) starts `webforc`'s own example app

Once it's running, open:

- `http://localhost:8080/` — a text field and a "Say Hello" button, built
  with `webforc.ui`'s idiomatic component API
  (`src/webforc/example/views/hello.clj`).
- `http://localhost:8080/user/42` (any id works) — proves route
  parameters and a real custom UI element together: a "User: 42" heading
  built with `com.webforj.component.element.ElementComposite`/`@NodeName`
  (`src/webforc/example/components/user_heading.clj`, used from
  `src/webforc/example/views/user.clj`), plus a "Back home" button.


## Public API

`webforc.api` is the public surface

### `defapp` / `defview`

Every webforJ app needs one entry point (`defapp`) and one or more routes
(`defview`). These macros generate the Java classes webforJ expects.

```clojure
(ns myapp.app
  (:require [webforc.api :refer [defapp]]))

(defapp myapp.Application
  {:packages ["myapp.views"]
   :theme "dark"
   :profile {:short-name "MYAPP" :name "myapp"}})
```

If you want more control

```clojure
(ns myapp.views.home
  (:require [webforc.api :refer [defview]]
            [webforc.ui :as w])
  (:import (com.webforj.component.layout.flexlayout FlexLayout)))

(defview myapp.views.HomeView "/" (FlexLayout.)
  (fn [self _params]
    (w/vbox! self {} (w/label "Hello!"))))
```


### Using components

`webforc.ui` wraps common webforJ components as plain functions taking an
options map, so you build UI with data instead of `Button.`/`.add` calls:

```clojure
(w/vbox! self {:spacing "1em"}
  (w/label "Name:")
  (w/text-field {:label "Name"})
  (w/button {:text "Save" :theme :primary :on-click (fn [_event] ...)}))
```


See "UI components" below for what's wrapped so far -- anything else is
used directly as raw webforJ Java.

### Custom UI elements

If there's no pre-built component for what you need, webforJ lets you
bind your own class to an HTML tag via `ElementComposite` + `@NodeName`.
`src/webforc/example/components/user_heading.clj` is a minimal example:

```clojure
(ns webforc.example.components.user-heading
  (:import (com.webforj.component.element ElementComposite)))

(gen-class
 :name ^{com.webforj.component.element.annotation.NodeName {:value "div"}}
 webforc.example.components.UserHeading
 :extends com.webforj.component.element.ElementComposite
 :prefix "heading-"
 :methods [[setUserId [String] void]]
 :exposes-methods {getElement superGetElement})

(defn heading-setUserId [this id]
  (.setHtml (.superGetElement this) (str "<h1>User: " id "</h1>")))
```

This is hand-written `gen-class`, not a macro like `defapp`/`defview` --
there's only one example of it so far. See ADR-002 for the annotation
mechanism this relies on.


## Support status

**Framework/API layer**

- ✅ App entry point (`defapp`) — `@Routify`/`@AppTheme`/`@AppProfile`
- ✅ Routing (`defview`) — `@Route`, path params (`:id`), multi-view
- ✅ Route parameters delivered as plain Clojure maps (`DidEnterObserver`)
- ✅ Query strings — via plain `Request.getQueryParameter` (no wrapper
  needed, already idiomatic enough as a one-line Java interop call)
- ✅ Embedded Jetty hosting (no Maven, library-usable — see ADR-001)
- ✅ Dev license/config scaffolding (`bin/fetch-license-files`)
- ⬜ Frontend asset bundling (`@BundleEntry`, custom CSS/JS bundles)
- ⬜ Hotswap / live-reload devtools
- ⬜ Playwright integration test wiring

**UI components (`webforc.ui`)**

- ✅ `button` (`com.webforj.component.button.Button`)
- ✅ `text-field` (`com.webforj.component.field.TextField`)
- ✅ `label` (`com.webforj.component.text.Label`)
- ✅ `vbox` / `vbox!`, `hbox` / `hbox!` (`FlexLayout`)
- ✅ `frame` (`com.webforj.component.window.Frame`)
- ⬜ Other form fields (`TextArea`, `PasswordField`, `NumberField`,
  `DateField`, `TimeField`, `DateTimeField`, `ColorField`, masked
  variants — all in `component.field`, same package as `TextField`)
- ⬜ Choice inputs (`CheckBox`, `RadioButton`, `RadioButtonGroup` —
  `component.optioninput`)
- ⬜ Data/tables (`webforj-table`, `webforj-infinite-scroll`)
- ⬜ Navigation (`component.tabbedpane`, `component.navigator`,
  `webforj-appnav`)
- ⬜ Toolbar/toast/dialog (`webforj-toolbar`, `webforj-toast` — `Toast`
  itself is used directly, unwrapped, in `webforc.example.views.hello`;
  `webforj-dialog`)
- ⬜ Layout extras (`webforj-splitter`, `webforj-columnslayout`,
  `webforj-applayout`)
- ⬜ Everything else in webforJ's component catalog (tree, slider,
  progress bar, upload, HTML editor, markdown viewer, terminal, charts,
  accordion, avatar, badge, card, drawer, spinner, login) — none wrapped,
  all directly usable as raw Java

**Custom components**

- ✅ Wrapping/authoring a custom web component via
  `com.webforj.component.element.ElementComposite`/`@NodeName` — see
  "Custom UI elements" above.

## TODO

- Package `webforc` as a real, installable library other projects can
  `:deps` on, instead of copying this project's files. Until then, to use
  `webforc` in another project, copy:
  - `src/webforc/` (skip `src/webforc/example/`)
  - `build.clj`, `deps.edn` (its `:build` alias), `bin/dev`
  - `resources/webforj.conf`, `bin/fetch-license-files`
- Move the example app into a fully separate top-level `examples/`
- No frontend bundling step (the Maven `webforj-maven-plugin` normally
  builds `src/main/frontend` into `static/frontend/*.css`/`.js` and wires
  `@BundleEntry`) — not needed yet, no custom frontend assets.
- No hotswap/live-reload devtools. No Playwright integration tests.

## ADRs

Design decisions and problems found along the way are recorded as ADRs:

- `ADR-001-embedded-jetty.md`
- `ADR-002-gen-class-annotations.md`
- `ADR-003-init-post-init-required-together.md`
- `ADR-004-build-automation.md`

## Why is there a `classes/` folder?

`gen-class` (expanded from `defapp`/`defview` in `src/webforc/api.clj`)
only writes a real `.class` file to disk when you AOT-compile it — just
loading the file with `require` builds the class in memory for that one
run and never saves it anywhere. webforJ needs an actual `.class` file to
exist on disk, because it doesn't call your code directly — it scans the
classpath for classes carrying the right annotation. `classes/` is where
that compiled file lives, and `clojure -T:build all` is what puts it
there. It's build output, like Maven's `target/`: gitignored, safe to
delete, and gets recreated the next time you build.
