(ns stonecutter.jwt
  (import [org.jose4j.jwk JsonWebKey$Factory JsonWebKey$OutputControlLevel RsaJwkGenerator]
          [org.jose4j.jws JsonWebSignature AlgorithmIdentifiers]
          [org.jose4j.jwt JwtClaims]))

(defn generate-rsa-key-pair [key-id]
  (doto (RsaJwkGenerator/generateJwk 2048)
    (.setKeyId key-id)))

(defn json->key-pair [json-string] (JsonWebKey$Factory/newJwk json-string))

(defn key-pair->json [key-pair] (.toJson key-pair JsonWebKey$OutputControlLevel/INCLUDE_PRIVATE))

(defn load-key-pair [path]
  (-> (slurp path) json->key-pair))

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


