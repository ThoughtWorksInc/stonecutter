(ns stonecutter.test.view.view-helpers
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :as vh]
            [stonecutter.view.error :as e]
            [stonecutter.test.test-helpers :as th]
            [stonecutter.view.index :as index]
            [stonecutter.view.change-email :as change-email]
            [stonecutter.view.invite-user :as invite-user]
            [stonecutter.view.delete-app :as delete-app]
            [stonecutter.view.apps-list :as apps-list]
            [stonecutter.view.user-list :as user-list]
            [stonecutter.view.unshare-profile-card :as unshare-profile-card]
            [stonecutter.view.authorise-failure :as authorise-failure]
            [stonecutter.view.authorise :as authorise]
            [stonecutter.view.reset-password :as reset-password]
            [stonecutter.view.forgotten-password-confirmation :as forgotten-password-confirmation-view]
            [stonecutter.view.forgotten-password :as forgotten-password-view]
            [stonecutter.view.change-password :as change-password]
            [stonecutter.view.delete-account :as delete-account]
            [stonecutter.view.profile-created :as profile-created]
            [stonecutter.view.profile :as profile]
            [stonecutter.view.confirmation-sign-in :as sign-in]))

(fact "can inject anti-forgery token"
      (let [page (-> "<html><form></form></html>"
                     html/html-snippet)]
        (-> page
            vh/add-anti-forgery
            (html/select [:form (html/attr= :name "__anti-forgery-token")])) =not=> empty?))

(fact "can remove elements from enlive map"
      (let [page (-> "<html><form></form></html>"
                     html/html-snippet)]
        (html/select page [:form]) =not=> empty?
        (-> page
            (vh/remove-element [:form])
            (html/select [:form])) => empty?))

(fact "templates caching"
      (let [file-name "html-file"
            html "some-html"]
        (fact "template are cached when caching is enabled"
              (vh/reset-template-cache!)
              (vh/enable-template-caching!)
              (vh/load-template file-name) => html
              (provided (html/html-resource file-name) => html :times 1)
              (vh/load-template file-name) => html
              (provided (html/html-resource file-name) => html :times 0))
        (fact "if caching is disabled then templates are always loaded from file"
              (vh/disable-template-caching!)
              (vh/load-template file-name) => html
              (provided (html/html-resource file-name) => html :times 1)
              (vh/load-template file-name) => html
              (provided (html/html-resource file-name) => html :times 1))))

(fact "clj-wip class is removed"
     (let [page (-> "<html><p class='clj-wip'>Random element.</p></html>"
                    html/html-snippet)]
       (html/select page [:p]) =not=> empty?
       (-> page
           vh/remove-work-in-progress
           (html/select [:p])) => empty?))


(fact "helper function for removing attributes"
      (let [page-html "<html a=\"b\"><body a=\"b\"></body></html>"]
        (-> page-html html/html-snippet (vh/remove-attribute [:body] :a) vh/enlive-to-str) => "<html a=\"b\"><body></body></html>"
        (-> page-html html/html-snippet (vh/remove-attribute-globally :a) vh/enlive-to-str) => "<html><body></body></html>"))

(fact "can append script to page"
      (let [page-html "<html><body><h1>Hello</h1></body></html>"]
        (-> page-html html/html-snippet (vh/add-script "js/blah.js") vh/enlive-to-str)
        => "<html><body><h1>Hello</h1><script src=\"js/blah.js\"></script></body></html>"))


(facts "about updating location settings"
       (fact "can update html lang attribute"
             (let [template (vh/load-template "public/index.html")
                   updated-template (vh/update-lang template "fi")
                   ]
               updated-template => (th/has-attr? [:html] :lang "fi")
               )))

(tabular
  (fact "about setting the language attribute based on the request language"
        (let [session-m {:locale :fi}
              context {:params {:app-id 5 :invite-id 3} :error-m {} :session session-m}]
          (first (html/select (?page context) [:html])) => (contains {:attrs (contains {:lang "fi"})})))
  ?page
  e/internal-server-error
  change-email/change-email-form
  index/accept-invite
  index/index
  invite-user/invite-user
  delete-app/delete-app-confirmation
  apps-list/apps-list
  user-list/user-list
  unshare-profile-card/unshare-profile-card
  authorise-failure/show-authorise-failure
  authorise/authorise-form
  reset-password/reset-password-form
  forgotten-password-confirmation-view/forgotten-password-confirmation
  forgotten-password-view/forgotten-password-form
  change-password/change-password-form
  delete-account/delete-account-confirmation
  profile-created/profile-created
  delete-account/profile-deleted
  profile/profile
  delete-account/email-confirmation-delete-account
  sign-in/confirmation-sign-in-form
  )

(fact "if given an unsupported locale code then will use :en"
      (let [session-m  {:locale :xx}
            context {:params {} :error-m {} :session session-m}]
        (first (html/select (index/index context) [:html])) => (contains {:attrs (contains {:lang "en"})})))
