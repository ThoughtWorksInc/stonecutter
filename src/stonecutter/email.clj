(ns stonecutter.email
  (:require [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]))

(def email-script-path (atom nil))

(def ^:dynamic email-renderers {:confirmation-email identity})

(defn configure-email [path]
  (reset! email-script-path path))

(defn render-email [email-data template-label]
  (let [renderer (email-renderers template-label)]
    (renderer email-data)))

(defn send-email [{:keys [email-address subject body] :as email-content}]
  (if-let [path @email-script-path]
    (shell/sh @email-script-path (str email-address) (str subject) (str body))
    (log/info "Email script path not set - unable to send email")))

(defn send-confirmation-email! [email-address]
  (-> {:email-address email-address :confirmation-uuid "pass-this-as-an-argument-at-some-point"}
      (render-email :confirmation-email)
      send-email))
