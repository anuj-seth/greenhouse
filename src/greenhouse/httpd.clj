(ns greenhouse.httpd
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [integrant.core :as ig]
            [compojure.api.sweet :as compojure-api :refer [POST GET]]
            [compojure.api.exception :as ex]
            [schema.core :as s]
            [ring.adapter.jetty :as jetty]
            [ring.util.http-response :as response]
            [ring.middleware.defaults :as middleware]
            [greenhouse.api :as bank-api]
            [greenhouse.util :as util]))

(defn request-validation-failed
  [f type]
  (fn [^Exception e data request]
    (f "Bad Request")))

(defn create-account
  [config request]
  request)

(defn app-routes
  [config]
  (compojure-api/api
   {:exceptions
    {:handlers {::ex/request-validation (request-validation-failed response/bad-request :error)}}
    :swagger {:ui "/"
              :spec "/swagger.json"
              :data {:info {:title "Bank API"
                            :description "Greenhouse bank"}
                     :tags [{:name "bank", :description "bank api"}]}}}
   (compojure-api/context "/" []
     :tags ["root"]
     (POST "/account" []
       :body [req {:name s/Str}]
       (let [{:keys [status data] :as response} (bank-api/in-transaction (:database config)
                                                                         bank-api/create-account
                                                                         {:account req})]
         (if (= status :ok)
           (response/ok (-> (:account data)
                            (select-keys [:id :name :balance])
                            (set/rename-keys {:id :account-number})))
           (response/bad-request (:error-msg response)))))
     (GET "/account/:id" []
       :path-params [id :- s/Int]
       (let [{:keys [status data] :as response} (bank-api/in-transaction (:database config)
                                                                         bank-api/view-account
                                                                         {:account {:id id}})]
         (if (= status :ok)
           (response/ok (-> (:account data)
                            (select-keys [:id :name :balance])
                            (set/rename-keys {:id :account-number})))
           (response/bad-request (:error-msg response)))))
     (POST "/account/:id/deposit" []
       :path-params [id :- s/Int]
       (let [{:keys [status data] :as response} (bank-api/in-transaction (:database config)
                                                                         bank-api/deposit
                                                                         {:account {:id id}})]
         (if (= status :ok)
           (response/ok (-> (:account data)
                            (select-keys [:id :name :balance])
                            (set/rename-keys {:id :account-number})))
           (response/bad-request (:error-msg response))))))))

(defmethod ig/init-key :greenhouse/httpd
  [_ {:keys [port] :as config}]
  (let [routes-and-middleware (middleware/wrap-defaults (app-routes config)
                                                        middleware/api-defaults)
        http-server (jetty/run-jetty routes-and-middleware
                                     {:port 3000
                                      :join? false})]
    (assoc config
           :http-server http-server)))

(defmethod ig/halt-key! :greenhouse/httpd
  [_ {:keys [http-server]}]
  (when http-server
    (log/info "Stopping http server")
    (.stop http-server)))



