(ns tentacles.http.xhrio
  (:require [clojure.string :as str]
            [tentacles.util :refer [lower-case-keys safe-parse]]
            [cljs.core.async :refer [chan <! >! put!]]
            [goog.net.XhrIo :as xhr]
            [tentacles.http.impl :refer [exec-request exec-single-request
                                         cached-response cache-response!
                                         IHttpCache]])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:import goog.net.XhrIo))

(defn exec-single-request*
  "Asynchronously performs the request, passes it through *wrap-response-fn*, and
  returns the output of calling safe-parse on it."
  [req]
  (let [ret (chan)
        to-resp (fn [resp] {:headers (lower-case-keys (js->clj (.getResponseHeaders resp)))
                            :status  (.getStatus resp)
                            :body    (.getResponseText resp)})]
    (xhr/send (str (:url req))
              (fn [e] (let [resp (to-resp (.-target e))]
                        (put! ret (if (:raw-response? req)
                                    resp
                                    (safe-parse resp)))))
              (str/upper-case (name (:method req)))
              (or (:body req) "")
              (clj->js (:headers req))
              (:timeout req))
    ret))

(defn exec-request*
  [single-req-fn req]
  (let [ret (chan)
        all-pages? (:all-pages req)]
    (go (let [resp (<! (single-req-fn req))]
          (if (and all-pages? (-> resp meta :links :next))
            (let [new-req (assoc req :url (-> resp meta :links :next))]
              (>! ret (concat resp (<! (exec-request* single-req-fn new-req)))))
            (>! ret resp))))
    ret))

(defrecord HttpRequester []
  tentacles.http.impl/IHttpRequester
  (exec-single-request [_ req]
    (exec-single-request* req))

  (exec-request [this req]
    (exec-request* (partial exec-single-request this) req)))

(defrecord CachedHttpRequester [cache]
  tentacles.http.impl/IHttpRequester
  (exec-single-request [_ req]
    (go (if-let [cached (cached-response cache req)]
          cached
          (let [resp (<! (exec-single-request* req))]
            (cache-response! cache req resp)
            resp))))

  (exec-request [this req]
    (go (if-let [cached (cached-response cache req)]
          cached
          (let [resp (<! (exec-request* (partial exec-single-request this) req))]
            (cache-response! cache req resp)
            resp)))))

(defn new-requester
  ([] (HttpRequester.))
  ([cache]
   (if-not (satisfies? IHttpCache cache)
     (throw (ex-info "Provided cache does not implement IHttpCache." {:cache cache})))
    (CachedHttpRequester. cache)))