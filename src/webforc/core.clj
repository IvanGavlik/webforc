(ns webforc.core
  "Embedded Jetty bootstrap for webforJ, replacing the Maven jetty-ee11 +
  webforj-maven-plugin toolchain used by Java/Maven webforJ apps."
  (:import (org.eclipse.jetty.server Server)
           (org.eclipse.jetty.ee11.servlet ServletContextHandler ServletHolder)
           (com.webforj.servlet WebforjServlet))
  (:gen-class))

(defn- build-server [port]
  (let [server (Server. (int port))
        holder (doto (ServletHolder. (WebforjServlet.))
                 ;; matches testapp's web.xml <load-on-startup>1</load-on-startup>:
                 ;; initialize eagerly at context start, not lazily on first request.
                 (.setInitOrder 1))
        ctx (doto (ServletContextHandler. ServletContextHandler/SESSIONS)
              (.setContextPath "/")
              (.addServlet holder "/*"))]
    (.setHandler server ctx)
    server))

(defn start
  "Starts the embedded webforJ server on `port` (default 8080). Returns the
  running Jetty Server, which can be passed to `stop`."
  ([] (start 8080))
  ([port]
   (let [server (build-server port)]
     (.start server)
     (println (str "webforc listening on http://localhost:" port "/"))
     server)))

(defn stop [^Server server]
  (.stop server))

(defn -main [& args]
  (let [port (if-let [p (first args)] (Integer/parseInt p) 8080)]
    (start port)))
