(ns stonecutter.test.view.view-helpers
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [stonecutter.view.view-helpers :refer :all]))

(fact "can inject anti-forgery token"
      (let [page (-> "<html><form></form></html>"
                     html/html-snippet)]
        (-> page
            add-anti-forgery
            (html/select [:form (html/attr= :name "__anti-forgery-token")])) =not=> empty?))

(fact "can remove elements from enlive map"
      (let [page (-> "<html><form></form></html>"
                     html/html-snippet)]
        (html/select page [:form]) =not=> empty?
        (-> page
            (remove-element [:form])
            (html/select [:form])) => empty?))

(fact "templates caching"
      (let [file-name "html-file"
            html "some-html"]
        (fact "template are cached when caching is enabled"
              (reset-template-cache!)
              (enable-template-caching!)
              (load-template file-name) => html
              (provided (html/html-resource file-name) => html :times 1)
              (load-template file-name) => html
              (provided (html/html-resource file-name) => html :times 0))
        (fact "if caching is disabled then templates are always loaded from file"
              (disable-template-caching!)
              (load-template file-name) => html
              (provided (html/html-resource file-name) => html :times 1)
              (load-template file-name) => html
              (provided (html/html-resource file-name) => html :times 1))))

(fact "clj-wip class is removed"
     (let [page (-> "<html><p class='clj-wip'>Random element.</p></html>"
                    html/html-snippet)]
       (html/select page [:p]) =not=> empty?
       (-> page
           remove-work-in-progress 
           (html/select [:p])) => empty?))

(fact "helper function for transforming templates"
      (let [page-html "<html><body><p class=\"a\"></p></body></html>"
            file-name "myfile"
            default-context {:translator identity}]
        (facts "can transform a page-enlive"
              (against-background (load-template file-name) => (html/html-snippet page-html))
              (fact "if there are no transformations then template is untouched"
                    (transform-template default-context file-name) => page-html)
              (fact "applies each supplied transformation in turn"
                    (transform-template default-context file-name
                                        (fn [m] (html/at m [:.a] (html/content "Hello")))
                                        (fn [m] (html/at m [:p] (html/set-attr :class "b"))))
                    => "<html><body><p class=\"b\">Hello</p></body></html>"))))
