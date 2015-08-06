(ns stonecutter.controller.forgotten-password
  (:require [stonecutter.validation :as v]
            [stonecutter.view.forgotten-password :as forgotten-password-view]
            [stonecutter.helper :as sh]))

(defn show-forgotten-password-form [request]
  (sh/enlive-response (forgotten-password-view/forgotten-password-form request) (:context request)))

(defn forgotten-password-form-post [request]
  (let [params (:params request)
        err (v/validate-forgotten-password params)
        request-with-validation-errors (assoc-in request [:context :errors] err)]
    (show-forgotten-password-form request-with-validation-errors)))
