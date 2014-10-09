(ns cmr.mock-echo.api.acls
  "Defines the HTTP URL routes for acls"
  (:require [compojure.core :refer :all]
            [cheshire.core :as json]
            [cmr.mock-echo.data.acl-db :as acl-db]
            [cmr.mock-echo.api.api-helpers :as ah]
            [cmr.common.services.errors :as svc-errors]))

(defn create-acl
  [context body]
  (let [acl (json/decode body true)]
    (acl-db/create-acl context (json/decode body true))))

(def object-identity-type->acl-key
  {"CATALOG_ITEM" :catalog_item_identity
   "SYSTEM_OBJECT" :system_object_identity})

(defn get-acls
  [context params]
  (when-not (:object_identity_type params)
    (svc-errors/throw-service-error
      :bad-request
      "Mock ECHO currently only supports retrieving ACLS by object identity type"))
  (when (:provider_id params)
    (svc-errors/throw-service-error
      :bad-request
      "Mock ECHO does not currently support retrieving acls by provider."))
  (when-not (= "false" (:reference params))
    (svc-errors/throw-service-error
      :bad-request
      "Mock ECHO does not currently support retrieving acls as references"))

  (let [acl-key (object-identity-type->acl-key (:object_identity_type params))]
    (filter #(get-in % [:acl acl-key]) (acl-db/get-acls context))))

(defn delete-acl
  [context guid]
  (acl-db/delete-acl context guid))

(defn build-routes [system]
  (routes
    (context "/acls" []
      ;; Creates one ACL. Returns the created acl with a guid
      (POST "/" {params :params context :request-context body :body}
        (ah/status-created (create-acl context (slurp body))))

      ;; Mimics echo-rest retrieval of acls
      (GET "/" {context :request-context params :params headers :headers}
        (ah/require-sys-admin-token headers)
        (ah/status-ok (get-acls context params)))

      (context "/:guid" [guid]
        (DELETE "/" {context :request-context}
          (delete-acl context guid)
          {:status 200})))))

