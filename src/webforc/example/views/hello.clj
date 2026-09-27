(ns webforc.example.views.hello
  "The '/' route -- example code, not part of the webforc library itself."
  (:require [webforc.api :refer [defview]]
            [webforc.ui :as w])
  (:import (com.webforj.component Theme)
           (com.webforj.component.layout.flexlayout FlexLayout)
           (com.webforj.component.toast Toast)))

(defview webforc.example.views.HelloWorldView "/" (FlexLayout.)
  (fn [self _params]
    (let [hello (w/text-field {:label "What is your name?"})]
      (w/vbox! self {:max-width 300 :style {"margin" "1em auto"}}
               hello
               (w/button {:text "Say Hello"
                          :theme :primary
                          :on-click (fn [_event]
                                      (Toast/show
                                       (str "Welcome to webforC " (.getValue hello) "!")
                                       Theme/GRAY))})))))
