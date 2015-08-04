(ns stonecutter.email
  (:require [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]))

(def email-script-path (atom nil))

(def ^:dynamic email-renderers {:confirmation-email identity})

(def sender (atom nil))
(def template-to-renderer-map (atom nil))

(defn null-sender [email-address subject body] nil)
(defn null-renderer [email-data] {:subject nil :body nil})

(defn stdout-sender [email-address subject body]
  (log/warn "Cannot send confirmation email as the path to the email sending script has not been set. Please set the EMAIL-SCRIPT-PATH environment variable to the appropriate script.")
  (log/debug "email-address: " email-address "\nsubject: " subject "\nbody: " body))

(defn stdout-renderer [email-data] {:subject nil :body email-data})

(defn confirmation-email-body [base-url confirmation-id]
  (str
    "Hi,\n"
    "Click this link to confirm your email address:\n"
    base-url "/confirm-email/" confirmation-id
    "\nCheers,"
    "\nAdmin"))

(defn confirmation-renderer [email-data]
  {:subject (format "Confirm your email for %s" (:app-name email-data))
   :body (confirmation-email-body (:base-url email-data) (:confirmation-id email-data))})

(defn send! [template email-address email-data]
  (let [{:keys [subject body]} ((template @template-to-renderer-map) email-data)]
    (@sender email-address subject body)))

(defn initialise! [sender-fn template-to-renderer-config]
  (reset! sender sender-fn)  
  (reset! template-to-renderer-map template-to-renderer-config))

(defn reset-email-configuration! []
  (reset! sender nil)
  (reset! template-to-renderer-map nil))

(defn bash-sender-factory [email-script-path]
  (if email-script-path
    (fn [email-address subject body] 
      (shell/sh email-script-path 
                (str email-address)
                (str subject)
                (str body))) 
    stdout-sender))

(defn configure-email [path]
  (initialise! (bash-sender-factory path)
               {:confirmation confirmation-renderer}))
