(ns webforc.api
  "Public API: defapp/defview macros over gen-class, avoiding the
  gen-class gotchas in ADR-002/ADR-003. Consumers only see plain Clojure
  functions and data, never gen-class itself."
  (:import (com.webforj.router.history ParametersBag)))

(defn params->map
  "ParametersBag -> plain {String String}, dropping unmatched optionals."
  [^ParametersBag bag]
  (into {}
        (keep (fn [^java.util.Map$Entry entry]
                (when-let [v (.getValue entry)]
                  [(.getKey entry) v])))
        (seq bag)))

(defmacro defapp
  "Defines a webforJ App entry point: `class-sym` extending com.webforj.App,
  with @Routify (and, if given, @AppTheme/@AppProfile) attached.

  opts:
    :packages   - vector of package name strings to scan for @Route views
                  (default: the defapp call site's own namespace-as-package)
    :theme      - string, -> @AppTheme
    :profile    - map with :short-name (required) and :name (required),
                  plus any other AppProfile attribute keys -> @AppProfile

  Example:
    (defapp myapp.Application
      {:packages [\"myapp.views\"]
       :theme \"system\"
       :profile {:short-name \"MYAPP\" :name \"myapp\"}})"
  [class-sym {:keys [packages theme profile]}]
  (let [ns-package (str (ns-name *ns*))
        anno-meta (cond-> {'com.webforj.annotation.Routify
                            {:packages (or packages [ns-package])}}
                    theme (assoc 'com.webforj.annotation.AppTheme {:value theme})
                    profile (assoc 'com.webforj.annotation.AppProfile
                                   (-> profile
                                       (dissoc :short-name)
                                       (assoc :shortName (:short-name profile)))))
        named (with-meta class-sym anno-meta)]
    `(do
       (gen-class
        :name ~named
        :extends com.webforj.App
        :prefix ~(str (name class-sym) "-"))
       (defn ~(symbol (str (name class-sym) "-run")) [~'_this]))))

(defmacro defview
  "Defines a webforJ route view: `class-sym` extending Composite, with
  @Route(path) attached and :init/:post-init always paired (ADR-003).

  path is a webforJ RoutePattern string: \"/\", \"/user/:id\",
  \"/user/:id<[0-9]+>\", \"/user/:path*\", etc. -- passed straight through
  to @Route's value(), no translation.

  bound-ctor-form is an expression that constructs the bound component,
  e.g. (FlexLayout.).

  wire-fn is a function of [bound-component params], called once after
  construction, where params is a plain {String String} map of matched
  route parameters (present only if the pattern declares any).

  Example:
    (defview myapp.views.UserView \"/user/:id\" (FlexLayout.)
      (fn [self params]
        (.add self (Label. (str \"User \" (get params \"id\"))))))"
  [class-sym path bound-ctor-form wire-fn]
  (let [prefix (str (name class-sym) "-")
        init-bound-sym (symbol (str prefix "initBoundComponent"))
        init-sym (symbol (str prefix "init"))
        post-init-sym (symbol (str prefix "post-init"))
        did-enter-sym (symbol (str prefix "onDidEnter"))
        ;; :exposes-methods wants a plain method name, not a :prefix-dispatched
        ;; var lookup -- unlike init/post-init below, this doesn't need
        ;; per-class uniqueness.
        get-bound-sym 'superGetBoundComponent
        named (with-meta class-sym {'com.webforj.router.annotation.Route {:value path}})]
    `(do
       (gen-class
        :name ~named
        :extends com.webforj.component.Composite
        :implements [com.webforj.router.observer.DidEnterObserver]
        :prefix ~prefix
        :init ~(symbol "init")
        :post-init ~(symbol "post-init")
        :exposes-methods {~'getBoundComponent ~get-bound-sym})

       ;; wire-fn runs from onDidEnter, not post-init: route params aren't
       ;; known yet at post-init time (construction happens before routing).
       (defn ~init-bound-sym [~'_this] ~bound-ctor-form)
       (defn ~init-sym [] [[] nil])
       (defn ~post-init-sym [~'_this])
       (defn ~did-enter-sym [~'this ~'_event params#]
         (~wire-fn (. ~'this ~get-bound-sym) (params->map params#))))))
