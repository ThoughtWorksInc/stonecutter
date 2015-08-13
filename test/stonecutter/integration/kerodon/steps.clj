(ns stonecutter.integration.kerodon.steps
  (:require [kerodon.core :as k]
            [stonecutter.integration.kerodon.kerodon-selectors :as ks]
            [stonecutter.routes :as r]
            [stonecutter.integration.kerodon.kerodon-helpers :as kh]))

(defn register [state email password]
  (-> state
      (k/visit (r/path :show-registration-form))
      (k/fill-in ks/registration-email-input email)
      (k/fill-in ks/registration-password-input password)
      (k/fill-in ks/registration-confirm-input password)
      (k/press ks/registration-submit)))

(defn sign-in [state email password]
  (-> state
      (k/visit (r/path :show-sign-in-form))
      (k/fill-in ks/sign-in-email-input email)
      (k/fill-in ks/sign-in-password-input password)
      (k/press ks/sign-in-submit)))

(defn sign-out [state]
  (-> state
      (k/visit (r/path :sign-out))))
