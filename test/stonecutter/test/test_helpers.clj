(ns stonecutter.test.test-helpers
  (:require [midje.sweet :as midje]
            [ring.mock.request :as mock]))

(defn create-request
  ([method url params]
   (-> (mock/request method url)
       (assoc :params params)
       (assoc-in [:context :translator] {})))
  ([method url params session]
   (-> (create-request method url params)
       (assoc :session session))))

(defn create-request-with-query-string
  ([method url params]
   (-> (mock/request method url params)
       (assoc :params params)
       (assoc-in [:context :translator] {})))
  ([method url params session]
   (-> (create-request-with-query-string method url params)
       (assoc :session session))))

(defn check-redirects-to [path]
  (midje/checker [response] (and
                        (= (:status response) 302)
                        (= (get-in response [:headers "Location"]) path))))

(defn check-signed-in [request user]
  (let [is-signed-in? #(and (= (:login user) (get-in % [:session :user-login]))
                            (contains? (:session %) :access_token))]
    (midje/checker [response]
             (let [session-not-changed (not (contains? response :session))]
               (or (and (is-signed-in? request)
                        session-not-changed)
                   (is-signed-in? response))))))

(defn add-config-request-context [request config]
  (assoc-in request [:context :config-m] config))

(defn create-user [login password]
  {:login    login
   :password password
   :name     nil
   :url      nil})
