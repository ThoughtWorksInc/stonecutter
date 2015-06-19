(ns stonecutter.validation
  (:require [clojure.string :as s]))

(defn is-email-valid? [{email :email}] 
  (when email
    (re-matches #"\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]+\b" email)))

(defn is-password-valid? [{password :password}]
  (not (s/blank? password)))

(defn do-passwords-match? [{:keys [password confirm-password]}]
  (= confirm-password password))

(def registration-validations 
  {:email is-email-valid?
   :password is-password-valid?
   :confirm-password do-passwords-match?})

(defn run-validation [params [validation-key validation-fn]]
  (if-not (validation-fn params) 
    validation-key
    nil))

(defn validate-registration [params]
  (->> registration-validations 
    (map (partial run-validation params))
    (remove nil?)) )
