(ns stonecutter.js.app
  (:require [dommy.core :as d]
            [stonecutter.js.dom.change-password :as cpd]
            [stonecutter.js.dom.register-form :as rfd]
            [stonecutter.js.controller.change-password :as cpc]
            [stonecutter.js.controller.register-form :as rfc]
            [stonecutter.js.controller.user-list :as ul]
            [stonecutter.js.controller.change-profile-form :as cpfc]
            [stonecutter.js.dom.upload-photo :as ulp]
            [stonecutter.js.dom.change-profile-form :as cpfd])
  (:require-macros [dommy.core :as dm]))

(def registration-form-state (atom rfc/default-state))
(def change-password-form-state (atom cpc/default-state))
(def change-profile-details-form-state (atom cpfc/default-state))

(defn setup-listener [selector event function]
  (when-let [e (dm/sel1 selector)]
    (d/listen! e event function)))

(defn setup-multi-listeners [selector event function]
  (when-let [elems (dm/sel selector)]
    (doseq [elem elems] (d/listen! elem event function))))

(defn setup-change-profile-form-listener [event input-field event-handler select-fn]
  (setup-listener (cpfd/input-selector input-field)
                  event
                  #(cpfc/update-state-and-render! change-profile-details-form-state input-field event-handler select-fn)))

(defn setup-change-profile-picture-form-listener [event input-field event-handler]
  (setup-change-profile-form-listener event input-field event-handler cpfd/get-file))

(defn setup-change-name-form-listener [event input-field event-handler]
  (setup-change-profile-form-listener event input-field event-handler cpfd/get-value))

(defn setup-registration-form-listener [event input-field event-handler]
  (setup-listener (rfd/input-selector input-field)
                  event
                  #(rfc/update-state-and-render! registration-form-state input-field event-handler)))

(defn setup-change-password-form-listener [event input-field event-handler]
  (setup-listener (cpd/input-selector input-field)
                  event
                  #(cpc/update-state-and-render! change-password-form-state input-field event-handler)))

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

  (setup-listener ulp/profile-card-photo__input :change ulp/upload-image)
  (setup-listener ulp/profile-card-photo__selector :mouseenter ulp/show-button)
  (setup-listener ulp/profile-card-photo__selector :mouseleave ulp/hide-button)

  (setup-change-password-form-listener :input :current-password cpc/update-current-password-input)
  (setup-change-password-form-listener :input :new-password cpc/update-new-password-input)

  (setup-change-password-form-listener :blur :current-password cpc/update-current-password-blur)
  (setup-change-password-form-listener :blur :new-password cpc/update-new-password-blur)

  (setup-change-name-form-listener :input :change-first-name cpfc/update-first-name-input)
  (setup-change-name-form-listener :input :change-last-name cpfc/update-last-name-input)

  (setup-change-name-form-listener :blur :change-first-name cpfc/update-first-name-blur)
  (setup-change-name-form-listener :blur :change-last-name cpfc/update-last-name-blur)

  (setup-change-profile-picture-form-listener :change :change-profile-picture cpfc/update-profile-picture-change)

  (setup-listener cpfd/change-profile-details-form-element-selector :submit (partial cpfc/block-invalid-submit change-profile-details-form-state))

  (setup-listener cpd/change-password-form-element-selector :submit (partial cpc/block-invalid-submit change-password-form-state)))

(set! (.-onload js/window) start)