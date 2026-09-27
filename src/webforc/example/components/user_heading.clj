(ns webforc.example.components.user-heading
  "Custom UI element example (ElementComposite/@NodeName) -- not part of
  the webforc library itself. See README.md 'Custom UI elements'."
  (:import (com.webforj.component.element ElementComposite)))

(gen-class
 :name ^{com.webforj.component.element.annotation.NodeName {:value "div"}}
 webforc.example.components.UserHeading
 :extends com.webforj.component.element.ElementComposite
 :prefix "heading-"
 ;; :prefix only auto-exposes overrides of existing superclass methods;
 ;; setUserId is new, so it needs an explicit :methods entry or it fails
 ;; at call time instead of compile time.
 :methods [[setUserId [String] void]]
 :exposes-methods {getElement superGetElement})

(defn heading-setUserId [this id]
  (.setHtml (.superGetElement this)
            (str "<h1>User: " id "</h1>\n"
                 "<u>This is custom ui element</u>")))
