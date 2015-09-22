(ns stonecutter.js.app
  (:require [dommy.core :as d]
            [stonecutter.js.controller.change-password :as cpc]
            [stonecutter.js.change-password :as cp]
            [stonecutter.js.dom.register-form :as rfr]
            [stonecutter.js.controller.register-form :as rfc]
            [stonecutter.js.controller.user-list :as ul])
  (:require-macros [dommy.core :as dm]))

(defn setup-listener [selector event function]
  (when-let [e (dm/sel1 selector)]
    (d/listen! e event function)))

(defn setup-multi-listeners [selector event function]
  (when-let [elems (dm/sel selector)]
    (doseq [elem elems] (d/listen! elem event function))))

(defn start []
  (setup-multi-listeners :.clj--user-item__toggle :change ul/update-role)

  (setup-listener (rfr/input-selector :registration-first-name) :input #(rfr/update-state-and-render! :registration-first-name rfc/update-first-name-input))
  (setup-listener (rfr/input-selector :registration-last-name) :input #(rfr/update-state-and-render! :registration-last-name rfc/update-last-name-input))
  (setup-listener (rfr/input-selector :registration-email) :input #(rfr/update-state-and-render! :registration-email rfc/update-email-address-input))
  (setup-listener (rfr/input-selector :registration-password) :input #(rfr/update-state-and-render! :registration-password rfc/update-password-input))

  (setup-listener (rfr/input-selector :registration-first-name) :blur #(rfr/update-state-and-render! :registration-first-name rfc/update-first-name-blur))
  (setup-listener (rfr/input-selector :registration-last-name) :blur #(rfr/update-state-and-render! :registration-last-name rfc/update-last-name-blur))
  (setup-listener (rfr/input-selector :registration-email) :blur #(rfr/update-state-and-render! :registration-email rfc/update-email-address-blur))
  (setup-listener (rfr/input-selector :registration-password) :blur #(rfr/update-state-and-render! :registration-password rfc/update-password-blur))

  (setup-listener rfr/register-form-element-selector :submit rfc/block-invalid-submit)

  (setup-listener cp/current-password-input :input #(cp/update-state-and-render :current-password cpc/update-current-password-input))
  (setup-listener cp/new-password-input :input #(cp/update-state-and-render :new-password cpc/update-new-password-input))
  (setup-listener cp/current-password-input :input #(cp/update-state-and-render :current-password cpc/update-new-password-input))

  (setup-listener cp/current-password-input :blur #(cp/update-state-and-render :current-password cpc/update-current-password-blur))
  (setup-listener cp/new-password-input :blur #(cp/update-state-and-render :new-password cpc/update-new-password-blur))

  (setup-listener cp/change-password-form :submit cp/block-invalid-submit))

(set! (.-onload js/window) start)