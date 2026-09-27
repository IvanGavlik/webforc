(ns webforc.interop
  "Low-level helpers for bridging Clojure functions into webforJ's Java
  listener interfaces. Ported from webforj-clojure's webforj.interop.
  Everything here is a private escape hatch used by webforc.ui --
  application code should not normally need this namespace."
  (:import (com.webforj.dispatcher EventListener)))

(defn ->listener
  ^EventListener [f]
  (reify EventListener
    (onEvent [_ event] (f event))))
