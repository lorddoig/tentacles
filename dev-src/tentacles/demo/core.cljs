(ns tentacles.demo.core
  (:require [tentacles.core :as t :include-macros true]
            [tentacles.users :as tu]
            [tentacles.http.impl :refer [IHttpRequester]]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)
(def token "my-token")

(defrecord DummyRequester []
  IHttpRequester
  (exec-single-request [_ req]
    (println "Single:" req)
    (go
      {:status 304}))
  (exec-request [_ req]
    (println "Multi:" req)
    (go
      {:status 304})))

(def rr (DummyRequester.))

(t/with-defaults
  {:oauth-token token}
  (t/with-requester rr
    (go
      (println (<! (tu/me {})))
      (println (<! (tu/my-followers {})))
      (println (<! (tu/emails {}))))))