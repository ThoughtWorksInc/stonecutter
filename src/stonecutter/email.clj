(ns stonecutter.email
  (:require [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]))

(def email-script-path (atom nil))

(defn configure-email [path]
  (reset! email-script-path path))

(defn send-confirmation-email! [email-address]
  (if-let [path @email-script-path]
    (shell/sh @email-script-path (str email-address))
    (log/info "Email script path not set - unable to send email")))
