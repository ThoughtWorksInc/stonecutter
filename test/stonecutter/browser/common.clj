(ns stonecutter.browser.common
  (:require [clj-webdriver.taxi :as wd]))


;; COMMON
(def stonecutter-index-page-body ".func--index-page")
(def stonecutter-sign-in-email-input ".func--sign-in-email__input")
(def stonecutter-sign-in-password-input ".func--sign-in-password__input")
(def stonecutter-sign-in-button ".func--sign-in__button")
(def stonecutter-register-first-name-input ".func--registration-first-name__input")
(def stonecutter-register-last-name-input ".func--registration-last-name__input")
(def stonecutter-register-email-input ".func--registration-email__input")
(def stonecutter-register-password-input ".func--registration-password__input")
(def stonecutter-register-create-profile-button ".func--create-profile__button")
(def stonecutter-trust-toggle ".clj--user-item__toggle")

(defn input-register-credentials-and-submit []
  (wd/input-text stonecutter-register-first-name-input "Journey")
  (wd/input-text stonecutter-register-last-name-input "Test")
  (wd/input-text stonecutter-register-email-input "stonecutter-journey-test@tw.com")
  (wd/input-text stonecutter-register-password-input "password")
  (wd/click stonecutter-register-create-profile-button))

(defn wait-for-selector [selector]
  (try
    (wd/wait-until #(not (empty? (wd/css-finder selector))) 5000)
    (catch Exception e
      (prn (str ">>>>>>>>>> Selector could not be found: " selector))
      (prn "==========  PAGE SOURCE ==========")
      (prn (wd/page-source))
      (prn "==========  END PAGE SOURCE ==========")
      (throw e))))

(def localhost "localhost:5439")

(def user-list-page (str localhost "/admin/users"))
;;______________