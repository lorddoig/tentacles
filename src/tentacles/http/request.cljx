(ns tentacles.http.request
  (:require [tentacles.http.impl]
            [tentacles.util :refer [query-map make-url to-json]]))

(defrecord HttpRequest [method url headers auth body follow-redirects
                        throw-exceptions timeout all-pages raw-response?]
  tentacles.http.impl/IHttpRequest
  (cache-hash [_]
    (hash (set [method url headers body (boolean all-pages) (boolean raw-response?)]))))

(def ^:dynamic *url*
  "The Github API endpoint.  Must end in a trailing slash."
  "https://api.github.com/")

(def ^:dynamic *defaults*
  "A map of default query parameters to be attached to every request."
  {})

(def ^:dynamic *map->request-fn*
  "The function that takes the map generated in make-request and turns it into
  an HttpRequest object.  Overriding potentially useful for custom caching etc."
  map->HttpRequest)

(defn make-request
  "Builds an HttpRequest.  Arguments:

   method               a keyword representing the request method e.g. :get
   end-point            a format string e.g. \"users/%s/%s\"
   positional           a sequence of args for end-point e.g. [\"a\" \"b\"]
   query                a map with possible keys:
     :accept              will be added as Accept header
     :oauth-token         will be added as Authorization header
     :etag                will be added as If-None-Match header
     :if-modified-since   will be added as If-Modified-Since header
     :user-agent          will be added as User-Agent header
     :auth                will cause the returned map to have {:auth {:basic auth}}
                          iff :oauth-token was not given
     :all-pages           should the client fetch and concat all pages of the response?
     :throw-exceptions    for requester use, passed through unmodified
     :follow-redirects    Ibid
     :timeout             Ibid

     All other keys will in query will be treated as being parameters for the Github
     API and will be attached to the query string or body of the request as appropriate
     for the method."
  [method end-point positional query]
  (let [{:keys [accept oauth-token etag if-modified-since all-pages user-agent
                auth throw-exceptions follow-redirects timeout]
         :or   {all-pages false}} (merge *defaults* query)
        query-in-body? (#{:post :put :delete} method)
        forbidden-query-keys #{:user-agent :oauth-token :throw-exceptions
                               :follow-redirects :all-pages :accept :timeout
                               :etag :if-modified-since}
        raw-query (:raw query)
        proper-query (query-map (apply dissoc query forbidden-query-keys))
        query-params (if query-in-body? {} proper-query)
        req {:method           method
             :url              (make-url *url* end-point positional query-params)
             :headers          (cond-> {}
                                       accept (assoc "Accept" accept)
                                       oauth-token (assoc "Authorization" (str "token " oauth-token))
                                       etag (assoc "If-None-Match" etag)
                                       if-modified-since (assoc "If-Modified-Since" if-modified-since)
                                       user-agent (assoc "User-Agent" user-agent))
             :auth             (cond
                                 oauth-token {:oauth-token oauth-token}
                                 auth {:basic auth}
                                 :else {})
             :body             (if query-in-body? (to-json (or raw-query proper-query)))
             :all-pages        all-pages
             :timeout          timeout
             :follow-redirects follow-redirects
             :throw-exceptions throw-exceptions}]
    (*map->request-fn* req)))
