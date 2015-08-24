(ns stonecutter.test.jwt
  (:require [midje.sweet :refer :all]
            [stonecutter.jwt :as jwt]
            [stonecutter.test.util.time :as test-time]
            [stonecutter.util.time :as t])
  (:import [org.jose4j.jwk RsaJwkGenerator JsonWebKey$OutputControlLevel]
           [org.jose4j.jwt.consumer JwtConsumerBuilder]
           [org.jose4j.jwt NumericDate]))

(def sub "uid")
(def issuer "stonecutter-url")
(def aud "client-id")
(def token-lifetime-minutes 10)

(defn decode [clock rsa-key-pair audience issuer id-token]
  (let [jwtConsumer (-> (JwtConsumerBuilder.)
                        (.setRequireExpirationTime)
                        (.setAllowedClockSkewInSeconds 30)
                        (.setRequireSubject)
                        (.setExpectedIssuer issuer)
                        (.setExpectedAudience (into-array [audience]))
                        (.setVerificationKey (.getKey rsa-key-pair))
                        (.setEvaluationTime (NumericDate/fromMilliseconds (t/now-in-millis clock)))
                        (.build))]
    (.getClaimsMap (.processToClaims jwtConsumer id-token))))

(facts "about serialising and deserialising key-pairs as json"
       (let [rsa-key-pair (jwt/generate-rsa-key-pair "k2")]
         (fact "parses json web key json string to key-pair object"
               (let [json (jwt/key-pair->json rsa-key-pair :include-private-key)
                     key-pair-from-json (jwt/json->key-pair json)]

                 (.getPublicKey key-pair-from-json) =not=> nil?
                 (.getPublicKey key-pair-from-json) => (.getPublicKey rsa-key-pair)

                 (.getPrivateKey key-pair-from-json) =not=> nil?
                 (.getPrivateKey key-pair-from-json) => (.getPrivateKey rsa-key-pair)))

         (fact "serialising does not include private key unless explicitly requested"
               (let [only-public-key (-> (jwt/key-pair->json rsa-key-pair)
                                         jwt/json->key-pair)
                     with-private-key (-> (jwt/key-pair->json rsa-key-pair :include-private-key)
                                          jwt/json->key-pair)]

                 (.getPrivateKey only-public-key) => nil?
                 (.getPrivateKey with-private-key) =not=> nil?

                 (.getPublicKey only-public-key) =not=> nil?
                 (.getPublicKey with-private-key) =not=> nil?))))

(facts "about generating id tokens"
       (let [rsa-key-pair (jwt/load-key-pair "./test-resources/test-key.json")
             stub-clock (test-time/new-stub-clock 0)
             id-token-generator (jwt/create-generator stub-clock rsa-key-pair issuer)
             additional-claims {:some-claim "some claim value"
                                :some-other-claim "some other claim value"}
             id-token (id-token-generator sub aud token-lifetime-minutes additional-claims)
             decoded-token (decode stub-clock rsa-key-pair aud issuer id-token)]

         (fact "id tokens can be generated and signed"
               (get decoded-token "iss") => issuer
               (get decoded-token "sub") => sub
               (get decoded-token "aud") => aud
               (get decoded-token "some-claim") => "some claim value"
               (get decoded-token "some-other-claim") => "some other claim value"
               (get decoded-token "iat") => 0
               (get decoded-token "exp") => 600)))
