(ns stonecutter.integration.kerodon.steps
  (:require [kerodon.core :as k]
            [stonecutter.integration.kerodon.kerodon-selectors :as ks]
            [stonecutter.integration.kerodon.kerodon-checkers :as kc]
            [stonecutter.routes :as r]
            [stonecutter.db.invitations :as i]))


(defn register
  ([state email password]
    (register state "dummy first" "dummy last" email password))
  ([state first-name last-name email password]
   (-> state
       (k/visit (r/path :index))
       (kc/check-and-fill-in ks/registration-first-name-input first-name)
       (kc/check-and-fill-in ks/registration-last-name-input last-name)
       (kc/check-and-fill-in ks/registration-email-input email)
       (kc/check-and-fill-in ks/registration-password-input password)
       (kc/check-and-press ks/registration-submit))))

(defn accept-invite [state first-name last-name password invite-store email clock expiry-days]
  (let [invite-id (i/generate-invite-id! invite-store email clock expiry-days (constantly "asdf"))]
    (-> state
        (k/visit (r/path :accept-invite :invite-id invite-id))
        (kc/check-and-fill-in ks/registration-first-name-input first-name)
        (kc/check-and-fill-in ks/registration-last-name-input last-name)
        (kc/check-and-fill-in ks/registration-password-input password)
        (kc/check-and-press ks/registration-submit))))

(defn sign-in [state email password]
  (-> state
      (k/visit (r/path :index))
      (kc/check-and-fill-in ks/sign-in-email-input email)
      (kc/check-and-fill-in ks/sign-in-password-input password)
      (kc/check-and-press ks/sign-in-submit)))

(defn sign-out [state]
  (-> state
      (k/visit (r/path :sign-out))))
