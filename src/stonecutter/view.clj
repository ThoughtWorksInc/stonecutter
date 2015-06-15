(ns stonecutter.view
  (:require [hiccup.core :as hiccup])
  )

(defn registration-form [] 
 (hiccup/html [:html
               [:span "Hello Cruel World"] 
               [:form]])) 
  
