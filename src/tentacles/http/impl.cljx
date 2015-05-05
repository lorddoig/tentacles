(ns tentacles.http.impl)

(defprotocol IHttpRequester
  (exec-single-request [this req])
  (exec-request [this req]))

(defprotocol IHttpCache
  (cached-response [this req])
  (cache-response! [this req resp]))

(defprotocol IHttpRequest
  (cache-hash [r]))
