(ns stonecutter.integration.kerodon.steps
  (:require [kerodon.core :as k]
            [stonecutter.integration.kerodon.kerodon-selectors :as ks]
            [stonecutter.integration.kerodon.kerodon-checkers :as kc]
            [stonecutter.routes :as r]
            [stonecutter.integration.kerodon.kerodon-helpers :as kh]))

(defn register [state email password]
  (-> state
      (k/visit (r/path :index))
      (kc/check-and-fill-in ks/registration-email-input email)
      (kc/check-and-fill-in ks/registration-password-input password)
      (kc/check-and-fill-in ks/registration-confirm-input password)
      (kc/check-and-press ks/registration-submit)))

(defn sign-in [state email password]
  (-> state
      (k/visit (r/path :index))
      (kc/check-and-fill-in ks/sign-in-email-input email)
      (kc/check-and-fill-in ks/sign-in-password-input password)
      (kc/check-and-press ks/sign-in-submit)))

(defn sign-out [state]
  (-> state
      (k/visit (r/path :sign-out))))
