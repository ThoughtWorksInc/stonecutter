(ns stonecutter.test.handler
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [stonecutter.handler :refer [app register-user]]
            [clauth.user :as user-store]))

(defn p [v] (prn v) v)

(fact "registration url returns a 200 response"
      (-> (mock/request :get "/register") app :status) => 200 
      )

(fact "user data is saved"
      (let [user-registration-data {:login "valid@email" 
                                    :password "encrypted-password" 
                                    :name nil
                                    :url nil}]
        (-> (mock/request :post "/register") 
            (assoc :params {:username "valid@email" :password "password"}) 
            register-user 
            :status) => 200 

        (provided
          (user-store/new-user "valid@email" "password") => user-registration-data
          (user-store/store-user user-registration-data) => anything)))
