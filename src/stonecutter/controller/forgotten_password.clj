(ns stonecutter.controller.forgotten-password
  (:require [stonecutter.validation :as v]
            [stonecutter.view.forgotten-password :as forgotten-password-view]
            [stonecutter.helper :as sh]
            [stonecutter.email :as email]))

(defn show-forgotten-password-form [request]
  (sh/enlive-response (forgotten-password-view/forgotten-password-form request) (:context request)))

(defn forgotten-password-form-post [email-sender request]
  (let [params (:params request)
        email-address (:email params)
        err (v/validate-forgotten-password params)
        app-name (get-in request [:context :config-m :app-name]) ;; FIXME JOHN 7/8 make functions for retrieving stuff from context
        base-url (get-in request [:context :config-m :base-url])
        request-with-validation-errors (assoc-in request [:context :errors] err)]

    (if (empty? err) (email/send! email-sender :forgotten-password email-address {:app-name app-name :base-url base-url :forgotten-password-id ""})
                   (show-forgotten-password-form request-with-validation-errors))))
