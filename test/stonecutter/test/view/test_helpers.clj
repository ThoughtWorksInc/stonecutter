(ns stonecutter.test.view.test-helpers)

(defn create-request [translator err params]
  {:context {:translator translator
             :errors err}
   :params params})
