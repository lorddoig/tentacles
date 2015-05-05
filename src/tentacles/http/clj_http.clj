(ns tentacles.http.clj-http
  (:use [tentacles.util])
  (:require [tentacles.http.impl :refer [exec-single-request exec-request
                                         cached-response cache-response!]]
            [clj-http.client :as http]
            [cemerick.url :as url])
  (:import (tentacles.http.impl IHttpCache)))

; Whether the requester should follow redirects or throw exceptions is fundamentally
; it's business, so having defaults configured at it's creation seems correct.
; The shared logic between the caching and non-caching requester makes it a pain
; to bubble these defaults through to make-request, though, and while we could
; make pluggable request builders too, that seems like overkill - so these
; dynamic vars are a small cheat.  These can be overridden by adding the keys
; to the request as before.
(def ^{:dynamic true :private true} *follow-redirects* true)
(def ^{:dynamic true :private true} *throw-exceptions* false)

(defmacro ^:private with-defaults [fr te & body]
  `(binding [*follow-redirects* ~fr
             *throw-exceptions* ~te]
     ~@body))

(defn- make-request [req]
  (let [or* (fn [o n] (or o n))
        basic-auth (:basic (:auth req))
        url (str (assoc (:url req) :query {}))
        query-params (:query (:url req))]
    (-> (cond-> (select-keys req [:method :follow-redirects
                                  :throw-exceptions :headers :body])
                basic-auth (assoc :basic-auth basic-auth))
        (assoc :query-params query-params)
        (assoc :url url)
        (update-in [:follow-redirects] or* *follow-redirects*)
        (update-in [:throw-exceptions] or* *throw-exceptions*))))

(defn- exec-single-request* [req]
  (let [resp (http/request (make-request req))]
    (if (:raw-response? req)
      resp
      (safe-parse resp))))

(defn- exec-request*
  ([req]
   (exec-request* nil req))
  ([single-req-fn req]
   (let [exec-single-request (or single-req-fn exec-single-request*)
         all-pages? (:all-pages req)
         update-req #(assoc-in %1 [:url :query] (:query (url/url %2)))
         resp (exec-single-request req)]
     (if (and all-pages? (-> resp meta :links :next))
       (let [new-req (update-req req (-> resp meta :links :next))]
         (lazy-cat resp (exec-single-request new-req)))
       resp))))

(defrecord HttpRequester [follow-redirects throw-exceptions]
  tentacles.http.impl/IHttpRequester
  (exec-single-request [_ req]
    (with-defaults follow-redirects throw-exceptions
                   (exec-single-request* req)))

  (exec-request [_ req]
    (with-defaults follow-redirects throw-exceptions
                   (exec-request* req))))

(defrecord CachedHttpRequester [cache follow-redirects throw-exceptions]
  tentacles.http.impl/IHttpRequester
  (exec-single-request [_ req]
    (with-defaults follow-redirects throw-exceptions
                   (if-let [cached (cached-response cache req)]
                     cached
                     (let [resp (exec-single-request* req)]
                       (cache-response! cache req resp)
                       resp))))

  (exec-request [this req]
    (with-defaults
      follow-redirects
      throw-exceptions
      (if-let [cached (cached-response cache req)]
        cached
        (let [resp (exec-request* (partial exec-single-request this) req)]
          (cache-response! cache req resp)
          resp)))))

(defn new-requester
  "Options:
    :cache               if provided, must implement tentacles.http.impl/IHttpCache
    :follow-redirects    defaults to true
    :throw-exceptions    defaults to false"
  [& {:keys [cache follow-redirects throw-exceptions]}]
  (if cache
    (if-not (satisfies? IHttpCache cache)
      (throw (ex-info "Provided cache does not implement IHttpCache." {:cache cache}))
      (CachedHttpRequester. cache follow-redirects throw-exceptions))
    (HttpRequester. follow-redirects throw-exceptions)))
