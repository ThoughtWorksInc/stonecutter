(ns stonecutter.js.app
  (:require [dommy.core :as d]
            [stonecutter.js.controller.change-password :as cpc]
            [stonecutter.js.change-password :as cp]
            [stonecutter.js.dom.register-form :as rfd]
            [stonecutter.js.controller.register-form :as rfc]
            [stonecutter.js.controller.user-list :as ul])
  (:require-macros [dommy.core :as dm]))

(def registration-form-state (atom rfc/default-state))

(defn setup-listener [selector event function]
  (when-let [e (dm/sel1 selector)]
    (d/listen! e event function)))

(defn setup-multi-listeners [selector event function]
  (when-let [elems (dm/sel selector)]
    (doseq [elem elems] (d/listen! elem event function))))

(defn setup-registration-form-listener [event input-field event-handler]
  (setup-listener (rfd/input-selector input-field)
                  event
                  #(rfc/update-state-and-render! registration-form-state input-field event-handler)))

(defn start []
  (setup-multi-listeners :.clj--user-item__toggle :change ul/update-role)

  (setup-registration-form-listener :input :registration-first-name rfc/update-first-name-input)
  (setup-registration-form-listener :input :registration-last-name rfc/update-last-name-input)
  (setup-registration-form-listener :input :registration-email rfc/update-email-address-input)
  (setup-registration-form-listener :input :registration-password rfc/update-password-input)

  (setup-registration-form-listener :blur :registration-first-name rfc/update-first-name-blur)
  (setup-registration-form-listener :blur :registration-last-name rfc/update-last-name-blur)
  (setup-registration-form-listener :blur :registration-email rfc/update-email-address-blur)
  (setup-registration-form-listener :blur :registration-password rfc/update-password-blur)

  (setup-listener rfd/register-form-element-selector :submit (partial rfc/block-invalid-submit registration-form-state))

  (setup-listener cp/current-password-input :input #(cp/update-state-and-render :current-password cpc/update-current-password-input))
  (setup-listener cp/new-password-input :input #(cp/update-state-and-render :new-password cpc/update-new-password-input))
  (setup-listener cp/current-password-input :input #(cp/update-state-and-render :current-password cpc/update-new-password-input))

  (setup-listener cp/current-password-input :blur #(cp/update-state-and-render :current-password cpc/update-current-password-blur))
  (setup-listener cp/new-password-input :blur #(cp/update-state-and-render :new-password cpc/update-new-password-blur))

  (setup-listener cp/change-password-form :submit cp/block-invalid-submit))

(set! (.-onload js/window) start)