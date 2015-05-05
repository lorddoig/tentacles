(ns tentacles.core
  (:require
    [tentacles.http.request :refer [make-request]]
    [tentacles.http.impl :refer [exec-request]]
    #+clj [tentacles.http.clj-http :refer [new-requester]]
    #+cljs [tentacles.http.xhrio :refer [new-requester]]))

(def ^:dynamic *requester* (new-requester))

(defn api-call
  ([method end-point] (api-call method end-point nil nil false))
  ([method end-point positional] (api-call method end-point positional nil false))
  ([method end-point positional query] (api-call method end-point positional query false))
  ([method end-point positional query raw?]
   (let [query (or query {})
         req (cond-> (make-request method end-point positional query)
                     raw? (assoc :raw-response? true))]
     (exec-request *requester* req))))

(defn raw-api-call
  ([method end-point] (api-call method end-point nil nil true))
  ([method end-point positional] (api-call method end-point positional nil true))
  ([method end-point positional query] (api-call method end-point positional query true)))

(defn api-meta
  [obj]
  (:api-meta (meta obj)))

(defn rate-limit
  ([] (api-call :get "rate_limit"))
  ([opts] (api-call :get "rate_limit" nil opts)))

(defmacro ^:also-cljs with-url [new-url & body]
  `(binding [tentacles.http.request/*url* ~new-url]
     ~@body))

(defmacro ^:also-cljs with-defaults [options & body]
  `(binding [tentacles.http.request/*defaults* ~options]
     ~@body))

(defmacro ^:also-cljs with-requester [requester & body]
  `(binding [*requester* ~requester]
     ~@body))
