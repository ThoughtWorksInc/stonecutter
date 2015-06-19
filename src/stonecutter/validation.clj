(ns stonecutter.validation
  (:require [clojure.string :as s]))

(defn is-email-valid? [{email :email}] 
  (when email
    (re-matches #"\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]+\b" email)))

(defn validate-email [params]
  (cond (not (is-email-valid? params)) :invalid
        :default nil))

(defn is-password-valid? [{password :password}]
  (not (s/blank? password)))

(defn validate-password [params]
  (cond (not (is-password-valid? params)) :invalid
        :default nil))

(defn do-passwords-match? [{:keys [password confirm-password]}]
  (= confirm-password password))

(defn validate-if-passwords-match [params]
  (cond (not (do-passwords-match? params)) :invalid
        :default nil))

(def registration-validations 
  {:email validate-email
   :password validate-password
   :confirm-password validate-if-passwords-match})

(defn run-validation [params [validation-key validation-fn]]
  [validation-key (validation-fn params)])

(defn validate-registration [params]
  (->> registration-validations 
    (map (partial run-validation params))
    (remove (comp nil? second))
    (into {})))
