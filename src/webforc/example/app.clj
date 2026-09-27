(ns webforc.example.app
  "Example app entry point -- not part of the webforc library itself."
  (:require [webforc.api :refer [defapp]]))

(defapp webforc.example.Application
  {:packages ["webforc.example.views"]
   :theme "system"
   :profile {:short-name "WEBFORC" :name "webforc"}})
