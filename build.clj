(ns build
  "AOT-compiles the gen-class namespaces. This is the only reason a build
  step exists at all: gen-class only produces real, classpath-discoverable
  .class files (with real RUNTIME annotations baked in - see the readme)
  when compiled ahead of time. Plain `require`/`load` never writes a .class
  file to disk, so webforJ's ClassGraph-based scanner would never see
  webforc.example.Application or webforc.example.views.HelloWorldView
  without this."
  (:require [clojure.tools.build.api :as b]))

(def class-dir "classes")

(defn clean [_]
  (b/delete {:path class-dir}))

(defn compile-clj [_]
  (b/compile-clj {:basis (b/create-basis {:project "deps.edn"})
                   :class-dir class-dir
                   :ns-compile '[webforc.example.app webforc.example.views.hello
                                 webforc.example.components.user-heading
                                 webforc.example.views.user
                                 webforc.core]}))

(defn all [opts]
  (clean opts)
  (compile-clj opts))
