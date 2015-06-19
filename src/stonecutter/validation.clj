(ns stonecutter.validation
  (:require [clojure.string :as s]))

(defn is-email-valid? [{email :email}]
  (when email
    (re-matches #"\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]+\b" email)))

(defn validate-email [is-duplicate-user-fn params]
  (cond (not (is-email-valid? params)) :invalid
        (is-duplicate-user-fn (:email params)) :duplicate
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

(defn registration-validations [is-duplicate-user-fn]
  {:email            (partial validate-email is-duplicate-user-fn)
   :password         validate-password
   :confirm-password validate-if-passwords-match})

(defn run-validation [params [validation-key validation-fn]]
  [validation-key (validation-fn params)])

(defn validate-registration [params duplicate-user-fn]
  (->> (registration-validations duplicate-user-fn)
       (map (partial run-validation params))
       (remove (comp nil? second))
       (into {})))
