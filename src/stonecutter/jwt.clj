(ns stonecutter.jwt
  (:require [clojure.tools.logging :as log]
            [stonecutter.routes :as routes]
            [stonecutter.util.time :as t])
  (:import [org.jose4j.jwk JsonWebKey$Factory JsonWebKey$OutputControlLevel RsaJwkGenerator JsonWebKeySet]
           [org.jose4j.jws JsonWebSignature AlgorithmIdentifiers]
           [org.jose4j.jwt JwtClaims NumericDate]
           [org.jose4j.jwx HeaderParameterNames]))

(defn generate-rsa-key-pair [key-id]
  (doto (RsaJwkGenerator/generateJwk 2048)
    (.setKeyId key-id)))

(defn json->key-pair [json-string] (JsonWebKey$Factory/newJwk json-string))

(defn key-pair->json
  ([key-pair]
   (key-pair->json key-pair nil))

  ([key-pair flag]
   (if (= flag :include-private-key)
     (.toJson key-pair JsonWebKey$OutputControlLevel/INCLUDE_PRIVATE)
     (.toJson key-pair))))

(defn json-web-key->json-web-key-set [json-web-key]
  (.toJson (JsonWebKeySet. [json-web-key])))

(defn load-key-pair [path]
  (try
    (-> (slurp path) json->key-pair)
    (catch Exception e
      (log/error e "Invalid RSA key file provided. App startup aborted.")
      (throw (Exception. "App startup aborted")))))

(defn set-additional-claims [jwt-claims claims-m]
  (doseq [[claim value] claims-m] (.setClaim jwt-claims (name claim) value)))

(defn create-generator [clock rsa-key-pair issuer]
  (fn [sub aud token-lifetime-minutes additional-claims]
    (let [now (t/now-in-millis clock)
          expiration-time (+ now (* token-lifetime-minutes 60 1000))
          jwt-claims (doto (JwtClaims.)
                       (.setIssuer issuer)
                       (.setAudience aud)
                       (.setExpirationTime (NumericDate/fromMilliseconds expiration-time))
                       (.setIssuedAt (NumericDate/fromMilliseconds now))
                       (.setSubject sub)
                       (set-additional-claims additional-claims))
          jws (doto (JsonWebSignature.)
                (.setPayload (.toJson jwt-claims))
                (.setKey (.getPrivateKey rsa-key-pair))
                (.setKeyIdHeaderValue (.getKeyId rsa-key-pair))
                (.setAlgorithmHeaderValue AlgorithmIdentifiers/RSA_USING_SHA256)
                (.setHeader HeaderParameterNames/JWK_SET_URL (str issuer (routes/path :jwk-set))))]
      (.getCompactSerialization jws))))
