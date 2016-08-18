(ns alloy.stele.server.main
	(:require [ring.adapter.jetty :refer :all]))

(defn app-handler [request]
	{:status 200
	 :headers {"Content-Type" "text/html"}
	 :body "Hello from Ring"})

(run-jetty app-handler {:port 3000})