(ns stonecutter.jwt
  (import [org.jose4j.jwk JsonWebKey$Factory]
          [org.jose4j.jws JsonWebSignature AlgorithmIdentifiers]
          [org.jose4j.jwt JwtClaims]))

(defn json->key-pair [json-string] (JsonWebKey$Factory/newJwk json-string))

(defn set-additional-claims [jwt-claims claims-m]
  (doseq [[claim value] claims-m] (.setClaim jwt-claims (name claim) value)))

(defn create-generator [rsa-key-pair issuer]
  (fn [sub aud token-lifetime-minutes additional-claims]
    (let [jwt-claims (doto (JwtClaims.)
                       (.setIssuer issuer)
                       (.setAudience aud)
                       (.setExpirationTimeMinutesInTheFuture token-lifetime-minutes)
                       (.setIssuedAtToNow)
                       (.setSubject sub)
                       (set-additional-claims additional-claims))
          jws (doto (JsonWebSignature.)
                (.setPayload (.toJson jwt-claims))
                (.setKey (.getPrivateKey rsa-key-pair))
                (.setKeyIdHeaderValue (.getKeyId rsa-key-pair))
                (.setAlgorithmHeaderValue AlgorithmIdentifiers/RSA_USING_SHA256))]
      (.getCompactSerialization jws))))


