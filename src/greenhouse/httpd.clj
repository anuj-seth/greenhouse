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
    (f "Bad Request - request validation failed")))

(defn call-api
  [{:keys [database]} api-fn in-data response-fn]
  (let [{:keys [status data] :as response} (bank-api/in-transaction database
                                                                    api-fn
                                                                    (util/dbg in-data))]
    (if (= status :ok)
      (response/ok (-> (response-fn (util/dbg data))
                       (select-keys [:id :name :balance])
                       (set/rename-keys {:id :account-number})))
      (response/bad-request (:error-msg response)))))

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
       (call-api config
                 bank-api/create-account
                 {:account req}
                 :account))
     (GET "/account/:id" []
       :path-params [id :- s/Int]
       (call-api config
                 bank-api/view-account
                 {:account {:id id}}
                 :account))
     (POST "/account/:id/deposit" []
       :path-params [id :- s/Int]
       :body-params [amount :- s/Num]
       (call-api config
                 bank-api/deposit-and-return-balance
                 {:credit-account {:id id
                                   :amount amount}}
                 :credit-account))
     (POST "/account/:id/withdraw" []
       :path-params [id :- s/Int]
       :body-params [amount :- s/Num]
       (call-api config
                 bank-api/withdraw-and-return-balance
                 {:debit-account {:id id
                                  :amount amount}}
                 :debit-account))
     (POST "/account/:id/send" []
       :path-params [id :- s/Int]
       :body-params [amount :- s/Num
                     account-number :- s/Int]
       (call-api config
                 bank-api/transfer
                 {:debit-account {:id id
                                  :amount amount}
                  :credit-account {:id account-number
                                   :amount amount}}
                 :debit-account)))))

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



