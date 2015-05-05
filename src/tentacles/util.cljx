(ns tentacles.util
  #+clj
  (:require
    [cheshire.core :as json]
    [clojure.string :as str]
    [cemerick.url :as url])
  #+cljs
  (:require
    [clojure.string :as str]
    [goog.string :as gstr]
    [goog.string.format]
    [cemerick.url :as url]))

(defn parse-link [link]
  (let [[_ url] (re-find #"<(.*)>" link)
        [_ rel] (re-find #"rel=\"(.*)\"" link)]
    [(keyword rel) url]))

(defn parse-links
  "Takes the content of the link header from a github resp, returns a map of links"
  [link-body]
  (->> (str/split link-body #",")
       (map parse-link)
       (into {})))

(defn parse-number [s]
  #+clj (Long/parseLong s)
  #+cljs (gstr/parseInt s))

(defn extract-useful-meta
  [h]
  (let [{:strs [etag last-modified x-ratelimit-limit x-ratelimit-remaining
                x-poll-interval]}
        h]
    {:etag           etag :last-modified last-modified
     :call-limit     (when x-ratelimit-limit (parse-number x-ratelimit-limit))
     :call-remaining (when x-ratelimit-remaining (parse-number x-ratelimit-remaining))
     :poll-interval  (when x-poll-interval (parse-number x-poll-interval))}))

(defn parse-json
  "Same as json/parse-string but handles nil gracefully."
  [s]
  (when s
    #+clj (json/parse-string s true)
    #+cljs (js->clj (.parse js/JSON s) :keywordize-keys true)))

(defn safe-parse
  "Takes a response map and checks for certain status codes. If 204, return nil.
   If 400, 401, 204, 422, 403, 404 or 500, return the original response with the body parsed
   as json. Otherwise, parse and return the body if json, or return the body if raw.

   The response map must adhere to the following schema:

   :status     number, HTTP status code e.g. 200
   :headers    a String => String map of response headers with lower-case keys
   :body       the unparsed body string."
  [{:keys [status headers body] :as resp}]
  (cond
    (= 202 status)
    ::accepted
    (= 304 status)
    ::not-modified
    (#{400 401 204 422 403 404 500} status)
    (update-in resp [:body] parse-json)
    :else (let [links (parse-links (get headers "link" ""))
                content-type (get headers "content-type" "")
                metadata (extract-useful-meta headers)]
            (if-not
              #+clj (.contains content-type "raw")
              #+cljs (gstr/contains content-type "raw")
              (let [parsed (parse-json body)]
                (if (map? parsed)
                  (with-meta parsed {:links links :api-meta metadata})
                  (with-meta (map #(with-meta % metadata) parsed)
                             {:links links :api-meta metadata})))
              body))))

(defn query-map
  "Merge defaults, turn keywords into strings, and replace hyphens with underscores."
  [entries]
  (into {} (for [[k v] entries] [(str/replace (name k) "-" "_") v])))

(defn no-content?
  "Takes a response and returns true if it is a 204 response, false otherwise."
  [x] (= (:status x) 204))

(defn make-url
  "Creates a URL out of end-point and positional. Called URLEncoder/encode on
   the elements of positional and then formats them in."
  [base end-point positional query-params]
  (let [format-fn (fn [s args]
                    #+clj (apply format s args)
                    #+cljs (apply goog.string/format s args))]
    (-> (str base (format-fn end-point (map url/url-encode positional)))
        url/url (assoc :query query-params))))

(defn to-json
  "Generates a JSON string."
  [v]
  #+clj (json/generate-string v)
  #+cljs (.stringify js/JSON (clj->js v)))

(defn lower-case-keys [m]
  (into {} (for [[k v] m] [(str/lower-case k) v])))
