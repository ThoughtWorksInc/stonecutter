(ns stonecutter.test.jwt
  (:require [midje.sweet :refer :all]
            [stonecutter.jwt :as jwt])
  (:import [org.jose4j.jwk RsaJwkGenerator]
           [org.jose4j.jwt.consumer JwtConsumerBuilder]))

(def sub "uid")
(def iss "stonecutter-url")
(def aud "client-id")
(def token-lifetime-minutes 10)
(def email "user@email.com")

(defn decode [rsa-key-pair audience id-token]
  (let [jwtConsumer (-> (JwtConsumerBuilder.)
                        (.setExpectedAudience (into-array [audience]))
                        (.setVerificationKey (.getKey rsa-key-pair))
                        (.build))
        claims-set (.processToClaims jwtConsumer id-token)]
    {:sub (.getSubject claims-set)
     :iss (.getIssuer claims-set)
     :aud (.getAudience claims-set)
     :iat (-> claims-set .getIssuedAt .getValue)
     :exp (-> claims-set .getExpirationTime .getValue)
     :email (.getClaimValue claims-set "email")}))

(facts "about generating id tokens"
       (let [rsa-key-pair (doto (RsaJwkGenerator/generateJwk 2048)
                            (.setKeyId  "k1"))
             id-token-generator (jwt/create-generator rsa-key-pair)
             id-token (id-token-generator iss sub aud token-lifetime-minutes email)]
         (fact "id tokens can be generated and signed"
               (:sub (decode rsa-key-pair aud id-token)) => sub
               (:iss (decode rsa-key-pair aud id-token)) => iss
               (:aud (decode rsa-key-pair aud id-token)) => [aud]
               (:email (decode rsa-key-pair aud id-token)) => email)

         (fact "token expiry time is set correctly, based on token lifetime in minutes"
               (let [decoded-token (decode rsa-key-pair aud id-token)
                     issued-at (:iat decoded-token)
                     expiry (:exp decoded-token)
                     token-lifetime-in-seconds (* 60 token-lifetime-minutes)]
                 (- expiry issued-at) => token-lifetime-in-seconds))))


