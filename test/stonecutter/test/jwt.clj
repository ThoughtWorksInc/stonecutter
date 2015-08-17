(ns stonecutter.test.jwt
  (:require [midje.sweet :refer :all]
            [stonecutter.jwt :as jwt])
  (:import [org.jose4j.jwk RsaJwkGenerator]
           [org.jose4j.jwt.consumer JwtConsumerBuilder]))

(def sub "uid")
(def iss "stonecutter-url")
(def aud "client-id")
(def token-lifetime-minutes 10)

(defn decode [rsa-key-pair audience id-token]
  (let [jwtConsumer (-> (JwtConsumerBuilder.)
                        (.setExpectedAudience (into-array [audience]))
                        (.setVerificationKey (.getKey rsa-key-pair))
                        (.build))]
    (.getClaimsMap (.processToClaims jwtConsumer id-token))))

(facts "about generating id tokens"
       (let [rsa-key-pair (doto (RsaJwkGenerator/generateJwk 2048)
                            (.setKeyId  "k1"))
             id-token-generator (jwt/create-generator rsa-key-pair)
             additional-claims {:some-claim "some claim value"
                                :some-other-claim "some other claim value"}
             id-token (id-token-generator iss sub aud token-lifetime-minutes additional-claims)
             decoded-token (decode rsa-key-pair aud id-token)]
         (fact "id tokens can be generated and signed"
               (get decoded-token "sub") => sub
               (get decoded-token "iss") => iss
               (get decoded-token "aud") => aud
               (get decoded-token "some-claim") => "some claim value"
               (get decoded-token "some-other-claim") => "some other claim value")

         (fact "token expiry time is set correctly, based on token lifetime in minutes"
               (let [issued-at (get decoded-token "iat")
                     expiry (get decoded-token "exp")
                     token-lifetime-in-seconds (* 60 token-lifetime-minutes)]
                 (- expiry issued-at) => token-lifetime-in-seconds))))


