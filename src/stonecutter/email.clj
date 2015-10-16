(ns stonecutter.email
  (:require [clojure.java.shell :as shell]
            [clojure.tools.logging :as log]
            [stonecutter.routes :as r]))

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

(defn confirmation-email-body [base-url confirmation-id]
  (str
    "Hi,\n"
    "Click this link to confirm your email address:\n"
    base-url (r/path :confirm-email-with-id :confirmation-id confirmation-id)
    "\nCheers,"
    "\nAdmin\n\n\n\n"
    "This was not me (delete account):\n"
    base-url (r/path :show-confirmation-delete :confirmation-id confirmation-id)))

(defn confirmation-renderer [email-data]
  {:subject (format "Confirm your email for %s" (:app-name email-data))
   :body (confirmation-email-body (:base-url email-data) (:confirmation-id email-data))})

(defn invite-email-body [base-url app-name]
  (let [invite-url (str base-url)]
    (str
      "Hi,\n"
      "Click this link to join "
      app-name
      "\n"
      invite-url
      "\nCheers,"
      "\nAdmin")))

(defn invite-renderer [email-data]
  {:subject (format "You've been invited to join %s" (:app-name email-data))
   :body (invite-email-body (:base-url email-data) (:app-name email-data))})

(defn forgotten-password-email-body [base-url forgotten-password-id]
  (let [reset-path (r/path :show-reset-password-form :forgotten-password-id forgotten-password-id)
        reset-url (str base-url reset-path)]
    (str
      "Hi,\n"
      "Click this link to reset your password:\n"
      reset-url
      "\nCheers,"
      "\nAdmin")))

(defn forgotten-password-renderer [email-data]
  (let [forgotten-password-id (:forgotten-password-id email-data)
        base-url (:base-url email-data)
        app-name (:app-name email-data)]
    {:subject (format "Reset password for %s" app-name)
     :body    (forgotten-password-email-body base-url forgotten-password-id)}))

(defn get-confirmation-renderer []
  confirmation-renderer)

(defn get-forgotten-password-renderer []
  forgotten-password-renderer)

(defn get-invite-renderer []
  invite-renderer)

(defn renderer-retrievers []
  {:confirmation (get-confirmation-renderer)
   :forgotten-password (get-forgotten-password-renderer)
   :invite (get-invite-renderer)})

(defn send! [sender template email-address email-data]
  (log/debug (format "sending template '%s' to '%s'." template email-address))
  (let [{:keys [subject body]} ((template (renderer-retrievers)) email-data)]
    (send-email! sender email-address subject body)))

(defn bash-sender-factory [email-script-path]
  (if email-script-path
    (create-bash-email-sender email-script-path)
    (create-stdout-email-sender)))



