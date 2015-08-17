(ns stonecutter.jwt
  (import [org.jose4j.jws JsonWebSignature AlgorithmIdentifiers]
          [org.jose4j.jwt JwtClaims]))

(defn create-generator [rsa-key-pair]
  (fn [iss sub aud token-lifetime-minutes email]
    (let [jwt-claims (doto (JwtClaims.)
                       (.setIssuer iss)
                       (.setAudience aud)
                       (.setExpirationTimeMinutesInTheFuture token-lifetime-minutes)
                       (.setIssuedAtToNow)
                       (.setSubject sub)
                       (.setClaim "email" email))
          jws (doto (JsonWebSignature.)
                (.setPayload (.toJson jwt-claims))
                (.setKey (.getPrivateKey rsa-key-pair))
                (.setKeyIdHeaderValue (.getKeyId rsa-key-pair))
                (.setAlgorithmHeaderValue AlgorithmIdentifiers/RSA_USING_SHA256))]
      (.getCompactSerialization jws))))


