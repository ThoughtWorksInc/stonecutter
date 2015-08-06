(ns stonecutter.email
  (:require [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]))

(defprotocol EmailSender
  (send-email! [this email-address subject body]))

(defrecord BashEmailSender [email-script-path]
  EmailSender
  (send-email! [this email-address subject body]
    (log/debug (format "sending email to '%s' using bash script: '%s'." email-address email-script-path))
    (try (let [shell-response (shell/sh email-script-path
                                        (str email-address)
                                        (str subject)
                                        (str body))]
           (log/debug (format "script returned exit code: '%s'" (:exit shell-response)))
           (when-not (= 0 (:exit shell-response))
             (log/error (format "script failed to send email. Here is the output of the script: '%s'" (:out shell-response))))
           shell-response)
         (catch Exception e (log/error e (format "Failed while calling '%s'." email-script-path))))))

(defn create-bash-email-sender [path]
  (log/debug (format "Initialising email sender to path: '%s'." path))
  (BashEmailSender. path))

(defrecord StdoutSender []
  EmailSender
  (send-email! [this email-address subject body]
    (log/warn "Cannot send confirmation email as the path to the email sending script has not been set. Please set the EMAIL_SCRIPT_PATH environment variable to the appropriate script.")
    (log/debug "email-address: " email-address "\nsubject: " subject "\nbody: " body)))

(defn create-stdout-email-sender []
  (StdoutSender. ))

(def template-to-renderer-map (atom nil))

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

(defn send! [sender template email-address email-data]
  (log/debug (format "sending template '%s' to '%s'." template email-address))
  (let [{:keys [subject body]} ((template @template-to-renderer-map) email-data)]
    (send-email! sender email-address subject body)))

(defn bash-sender-factory [email-script-path]
  (if email-script-path
    (create-bash-email-sender email-script-path)
    (create-stdout-email-sender)))

(defn initialise! [template-to-renderer-config]
  (reset! template-to-renderer-map template-to-renderer-config))

(initialise! {:confirmation confirmation-renderer})


