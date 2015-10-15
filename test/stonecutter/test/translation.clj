(ns stonecutter.test.translation
  (:require [midje.sweet :refer :all]
            [stonecutter.translation :as t]))

(facts "can load translations from a string"
       (fact "basic example"
             (t/load-translations-from-string "a-key: Hello") => {:a-key "Hello"})
       (fact "with nested keys"
                    (t/load-translations-from-string
                      "a:\n
                          hello: Hello\n
                          goodbye: Goodbye\n") => {:a {:hello "Hello" :goodbye "Goodbye"}}))

(facts "can load translations from a file"
       (t/load-translations-from-file "test-translations.yml") => {:a {:hello "Hello" :goodbye "Goodbye"}})

(facts "about getting locale from requests"
       (fact "request with no locales set defaults to :en"
             (t/get-locale-from-request {}) => :en)
       (fact "request with session locale set, always take session locale above others"
             (t/get-locale-from-request {:session {:locale :fi} :locale :en}) => :fi)
       (fact "request can take locale if no session locale is set"
             (t/get-locale-from-request {:session {:locale nil} :locale :fr}) => :fr))