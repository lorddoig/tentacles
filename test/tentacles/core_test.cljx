(ns tentacles.core-test
  (:require
    [tentacles.core :as core]
    [tentacles.util :as util]
    [tentacles.http.request :as req]
    [tentacles.test-support :refer [test-data]]
    #+clj [clojure.test :refer [deftest is]]
    #+cljs [cemerick.cljs.test :refer-macros [deftest is done]]
    #+cljs [cljs.core.async :refer [<!]])
  #+cljs
  (:require-macros [cljs.core.async.macros :refer [go]]))

#+clj
(deftest request-contains-user-agent
  (let [request (req/make-request :get "test" nil {:user-agent "Mozilla"})
        url (:url request)]
    (do (is (empty? (:query url)))
        (is (contains? (:headers request) "User-Agent"))
        (is (= (get (:headers request) "User-Agent") "Mozilla")))))

(deftest url-formatting-works
  (is (= "http://github.com/a/b/c"
         (str (util/make-url "http://github.com/" "a/%s/%s" ["b" "c"] {})))))


(deftest hitting-rate-limit-is-propagated
  (is (= (:status (util/safe-parse {:status 403}))
         403)))

(deftest rate-limit-details-are-propagated
  (is (= 60 (:call-limit (core/api-meta
                           (util/safe-parse {:status 200 :headers {"x-ratelimit-limit" "60"
                                                                   "content-type"      ""}}))))))

(deftest poll-limit-details-are-propagated
  (is (= 61 (:poll-interval (core/api-meta
                              (util/safe-parse {:status  200
                                                :headers {"x-poll-interval" "61"
                                                          "content-type"    ""}}))))))
