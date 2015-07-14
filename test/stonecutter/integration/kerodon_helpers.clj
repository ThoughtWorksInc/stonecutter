(ns stonecutter.integration.kerodon-helpers
  (:require [midje.sweet :refer :all]
            [cheshire.core :as json]
            [net.cgrand.enlive-html :as html]
            [kerodon.core :as k]
            [clojure.string :as string]))

(defn page-title [state]
  (-> state :enlive (html/select [:title]) first html/text))

(defn page-title-is [state title]
  (fact {:midje/name "Checking page title:"}
        (page-title state) => title)
  state)

(defn page-uri-is [state uri]
  (fact {:midje/name "Checking page uri:"}
        (-> state :request :uri) => uri)
  state)

(defn response-status-is [state status]
  (fact {:midje/name "Checking response status"}
        (-> state :response :status) => status)
  state)

(defn selector-exists [state selector]
  (fact {:midje/name "Check element exists"}
        (-> state :enlive (html/select selector)) =not=> empty?)
  state)

(defn selector-includes-content [state selector content]
  (fact {:midje/name "Check if element contains string"}
        (-> state :enlive (html/select selector) first html/text) => (contains content))
  state)

(defn location-contains [state path]
  (fact {:midje/name "Checking location in header:"}
        (-> state :response (get-in [:headers "Location"])) => (contains path))
  state)

(defn response-has-access-token [state]
  (fact {:midje/name "Checking if response has access bearer token"}
        (let [response-body (-> state
                                :response
                                :body
                                (json/parse-string keyword))]
          (:access_token response-body) => (just #"[A-Z0-9]{32}")
          (:token_type response-body) => "bearer"))
  state)

(defn response-has-user-email [state email]
  (fact {:midje/name "Checking if response has user email"}
        (let [response-body (-> state
                                :response
                                :body
                                (json/parse-string keyword))]
          (:user-email response-body) => email))
  state)

(defn response-has-id [state]
  (fact {:midje/name "Checking if response has user id"}
        (let [response-body (-> state
                                :response
                                :body
                                (json/parse-string keyword))]
          (:user-id response-body) =not=> string/blank?))
  state)

;; FIXME can't reuse the body because it's a buffered input stream
(defn replay-last-request [state]
  (let [request (-> state :request)]
    (k/visit state (:uri request) :body (:body state) :headers (:headers state) :request-method :post)))
