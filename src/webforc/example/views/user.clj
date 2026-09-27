(ns webforc.example.views.user
  "The '/user/:id' route -- example code, not part of the webforc library
  itself. See README.md 'Custom UI elements' for the UserHeading component
  used here."
  (:require [webforc.api :refer [defview]]
            [webforc.ui :as w])
  (:import (com.webforj.component.layout.flexlayout FlexLayout)
           (com.webforj.router Router)
           (webforc.example.components UserHeading)))

(defview webforc.example.views.UserView "/user/:id" (FlexLayout.)
  (fn [self params]
    (let [id (get params "id")
          heading (doto (UserHeading.) (.setUserId id))]
      (w/vbox! self {:spacing "0.75em"
                      :max-width 300
                      :style {"margin" "1em auto"}}
               heading
               (w/button {:text "Back home"
                          :theme :outlined-gray
                          :on-click (fn [_event]
                                      (.navigate (Router/getCurrent)
                                                 (com.webforj.router.history.Location. "/")))})))))
