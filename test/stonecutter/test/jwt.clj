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
     :email (.getClaimValue claims-set "email")}))

(fact "id tokens can be generated and signed"
      (let [rsa-key-pair (doto (RsaJwkGenerator/generateJwk 2048)
                           (.setKeyId  "k1"))
            id-token-generator (jwt/create-generator rsa-key-pair)
            id-token (id-token-generator iss sub aud token-lifetime-minutes email)]
        (:sub (decode rsa-key-pair aud id-token)) => sub
        (:iss (decode rsa-key-pair aud id-token)) => iss
        (:aud (decode rsa-key-pair aud id-token)) => [aud]
        (:email (decode rsa-key-pair aud id-token)) => email))


