(ns webforc.ui
  "Idiomatic Clojure component constructors (options maps, plain fns as
  listeners, variadic children), ported/adapted from webforj-clojure's
  webforj.ui. Every function returns a real webforJ component; nothing
  here is a wrapper type."
  (:require [webforc.interop :as interop])
  (:import (com.webforj.component.button Button ButtonTheme)
           (com.webforj.component.field TextField)
           (com.webforj.component.text Label)
           (com.webforj.component.layout.flexlayout FlexLayout FlexDirection)
           (com.webforj.component.window Frame)))

(defn- add-children! [container children]
  (when (seq children)
    (.add container (into-array com.webforj.component.Component children)))
  container)

(defn- apply-style! [component {:keys [style class-name width max-width]}]
  (doseq [[k v] style]
    (.setStyle component (name k) v))
  (when class-name
    (.addClassName component (into-array String (if (string? class-name)
                                                    [class-name]
                                                    class-name))))
  (when width
    (.setWidth component width))
  (when max-width
    (.setMaxWidth component (float max-width)))
  component)

(def button-themes
  {:default ButtonTheme/DEFAULT
   :primary ButtonTheme/PRIMARY
   :danger ButtonTheme/DANGER
   :success ButtonTheme/SUCCESS
   :warning ButtonTheme/WARNING
   :gray ButtonTheme/GRAY
   :info ButtonTheme/INFO
   :outlined-primary ButtonTheme/OUTLINED_PRIMARY
   :outlined-danger ButtonTheme/OUTLINED_DANGER
   :outlined-success ButtonTheme/OUTLINED_SUCCESS
   :outlined-warning ButtonTheme/OUTLINED_WARNING
   :outlined-gray ButtonTheme/OUTLINED_GRAY
   :outlined-info ButtonTheme/OUTLINED_INFO})

(defn button
  "opts: :text :theme (key of button-themes) :on-click (fn [event])
  :style :class-name :width :max-width"
  ^Button [{:keys [text theme on-click] :as opts}]
  (let [b (Button. (or text ""))]
    (when theme
      (.setTheme b (button-themes theme theme)))
    (when on-click
      (.addClickListener b (interop/->listener on-click)))
    (apply-style! b opts)))

(defn text-field
  "opts: :label :value :placeholder :on-change (fn [event])
  :style :class-name :width :max-width"
  ^TextField [{:keys [label value placeholder on-change] :as opts}]
  (let [f (cond
            (and value placeholder) (TextField. (or label "") value placeholder)
            value (TextField. (or label "") value)
            :else (TextField. (or label "")))]
    (when on-change
      (.addValueChangeListener f (interop/->listener on-change)))
    (apply-style! f opts)))

(defn label
  "Accepts a plain string, or an opts map (:text plus style/class-name/
  width/max-width)."
  ^Label [text-or-opts]
  (if (map? text-or-opts)
    (apply-style! (Label. (:text text-or-opts "")) text-or-opts)
    (Label. text-or-opts)))

(defn- split-opts [args]
  (if (map? (first args))
    [(first args) (rest args)]
    [{} args]))

(defn layout!
  "Configures an existing FlexLayout in place and returns it -- for a
  defview wire-fn's `self`, which defview already constructed, so there's
  no second layout to build. Optional leading opts map (:spacing plus
  style/class-name/width/max-width), then child components."
  ^FlexLayout [^FlexLayout self direction & args]
  (let [[{:keys [spacing] :as opts} children] (split-opts args)]
    (.setDirection self direction)
    (when spacing
      (.setSpacing self spacing))
    (apply-style! self opts)
    (add-children! self children)))

(defn vbox! ^FlexLayout [self & args] (apply layout! self FlexDirection/COLUMN args))
(defn hbox! ^FlexLayout [self & args] (apply layout! self FlexDirection/ROW args))

(defn- flex-box [direction args]
  (let [[opts children] (split-opts args)]
    (apply layout! (FlexLayout.) direction (cons opts children))))

(defn vbox
  "Constructs a new, nested FlexLayout -- use vbox! instead to configure a
  defview's own `self`."
  ^FlexLayout [& args]
  (flex-box FlexDirection/COLUMN args))

(defn hbox ^FlexLayout [& args] (flex-box FlexDirection/ROW args))

(defn frame
  "Not normally needed inside a defview (webforJ's router manages the
  Frame itself, see webforc.api/defapp) -- for App#run() bodies that skip
  routing entirely."
  ^Frame [& children]
  (doto (Frame.)
    (add-children! children)))
