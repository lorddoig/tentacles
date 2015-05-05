(defproject
  tentacles "0.4.0-SNAPSHOT"
  :description "A library for working with the Github API."
  :url "https://github.com/Raynes/tentacles"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src" "target/classes"]
  :test-paths ["test" "target/test-classes"]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.4.0"]
                 [com.cemerick/url "0.1.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [environ "1.0.0"]
                 [org.clojure/tools.reader "0.9.2"]]

  :clean-targets ^{:protect false} ["dev-resources/public/js"]

  :profiles {:dev
             {:dependencies [[org.clojure/clojurescript "0.0-3211"]
                             [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                             [clj-http "1.0.1"]]
              :plugins      [[com.keminglabs/cljx "0.6.0"]
                             [lein-pdo "0.1.1"]
                             [lein-cljsbuild "1.0.5"]
                             [com.cemerick/clojurescript.test "0.3.3"]]
              :cljsbuild    {:builds [{:id           "demo"
                                       :source-paths ["src" "target/classes" "dev-src"]
                                       :compiler     {:output-to     "dev-resources/public/js/demo.js"
                                                      :output-dir    "dev-resources/public/js"
                                                      :source-map    "dev-resources/public/js/demo.js.map"
                                                      :optimizations :whitespace}}]}}
             :provided
             {:dependencies [[org.clojure/clojurescript "0.0-3211"]
                             [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                             [clj-http "1.0.1"]]}}
  :cljx
  {:builds
   [{:source-paths ["src"] :rules :clj :output-path "target/classes"}
    {:source-paths ["src"] :rules :cljs :output-path "target/classes"}
    {:source-paths ["test"] :rules :clj :output-path "target/test-classes"}
    {:source-paths ["test"] :rules :cljs :output-path "target/test-classes"}]}

  :cljsbuild
  {:test-commands {"phantom" ["phantomjs" :runner "target/tests.js"]
                   ;"node"    ["node" :node-runner "target/tests-node.js"]
                   }

   :builds        [{:id           "main"
                    :source-paths ["src" "target/classes"]
                    :compiler     {:output-to     "target/main.js"
                                   :optimizations :whitespace
                                   :pretty-print  true}}
                   {:id           "tests"
                    :source-paths ["src" "target/classes" "test" "target/test-classes"]
                    :compiler     {:output-to     "target/tests.js"
                                   :optimizations :whitespace
                                   :pretty-print  true}}
                   ;{:id           "tests-node"
                   ; :source-paths ["src" "target/classes" "test" "target/test-classes"]
                   ; :compiler     {:output-to     "target/tests-node.js"
                   ;                :optimizations :simple
                   ;                :target        :nodejs
                   ;                :hashbang      false}}
                   ]}

  :auto-clean false
  :prep-tasks [["cljx" "once"] "javac" "compile"]

  :aliases {"test-all"       ["do" "clean," "cljx" "once,"
                              "test,"
                              "cljsbuild" "test"]
            "autobuild-demo" ["pdo" "cljx" "auto," "cljsbuild" "auto" "demo"]})
