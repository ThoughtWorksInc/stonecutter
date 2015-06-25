(ns stonecutter.validation
  (:require [clojure.string :as s]))

(def email-max-length 254)

(def password-min-length 8)

(def password-max-length 254)

(defn is-email-valid? [{email :email}]
  (when email
    (re-matches #"\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]+\b" email)))

(defn is-too-long? [string max-length]
  (> (count string) max-length))

(defn is-too-short? [string min-length]
  (< (count string) min-length))

(defn validate-registration-email [is-duplicate-user-fn params]
  (cond (is-too-long? (:email params) email-max-length) :too-long
        (not (is-email-valid? params)) :invalid
        (is-duplicate-user-fn (:email params)) :duplicate
        :default nil))

(defn validate-sign-in-email [params]
  (cond (is-too-long? (:email params) email-max-length) :too-long
        (not (is-email-valid? params)) :invalid
        :default nil))

(defn validate-password [params]
  (cond (s/blank? (:password params)) :blank
        (is-too-long? (:password params) password-max-length) :too-long
        (is-too-short? (:password params) password-min-length) :too-short
        :default nil))

(defn do-passwords-match? [{:keys [password confirm-password]}]
  (= confirm-password password))

(defn validate-if-passwords-match [params]
  (cond (not (do-passwords-match? params)) :invalid
        :default nil))

(defn run-validation [params [validation-key validation-fn]]
  [validation-key (validation-fn params)])

(defn registration-validations [is-duplicate-user-fn]
  {:email            (partial validate-registration-email is-duplicate-user-fn)
   :password         validate-password
   :confirm-password validate-if-passwords-match})

(defn validate-registration [params duplicate-user-fn]
  (->> (registration-validations duplicate-user-fn)
       (map (partial run-validation params))
       (remove (comp nil? second))
       (into {})))

(def sign-in-validations
  {:email            validate-sign-in-email
   :password         validate-password})

(defn validate-sign-in [params]
  (->> sign-in-validations
       (map (partial run-validation params))
       (remove (comp nil? second))
       (into {})))
