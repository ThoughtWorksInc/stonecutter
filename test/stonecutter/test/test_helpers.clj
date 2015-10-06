(ns stonecutter.test.test-helpers
  (:require [midje.sweet :as midje]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as mock]
            [stonecutter.db.user :as user]
            [stonecutter.session :as session]
            [stonecutter.db.client :as client]
            [clauth.client :as cl-client]))

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

(defn check-renders-page [body-class-enlive-selector]
  (midje/checker [response] (and
                              (= (:status response) 200)
                              (not-empty (-> (html/html-snippet (:body response))
                                             (html/select [body-class-enlive-selector]))))))

(defn check-signed-in [request user]
  (let [is-signed-in? #(and (= (:login user) (session/request->user-login %))
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

(defn store-user!
  ([user-store email password]
   (store-user! user-store "Frank" "Lasty" email password))
  ([user-store first-name last-name email password]
   (user/store-user! user-store first-name last-name email password)))

(defn create-client [client-id]
  {:name          "name"
   :client-id     client-id
   :client-secret "secret"
   :url           "url"})

(defn store-client! [client-store client-id]
  (cl-client/store-client client-store (create-client client-id)))
