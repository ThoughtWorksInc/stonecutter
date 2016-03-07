(ns stonecutter.session)

(defn request->user-login [request]
  (get-in request [:session :user-login]))

(defn request->access-token [request]
  (get-in request [:session :access_token]))

(defn request->return-to [request]
  (get-in request [:session :return-to]))

(defn request->profile-photo [request]
  (get-in request [:params :profile-photo]))

(defn request->first-name [request]
  (get-in request [:params :first-name]))

(defn request->last-name [request]
  (get-in request [:params :last-name]))

(defn set-user-login [response user-login]
  (assoc-in response [:session :user-login] user-login))

(defn set-user-role [response user-role]
  (assoc-in response [:session :role] user-role))

(defn set-access-token [response access-token]
  (assoc-in response [:session :access_token] access-token))

(defn replace-session-with [response existing-session]
  (assoc response :session existing-session))
