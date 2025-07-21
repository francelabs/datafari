-- Create the database
CREATE DATABASE datafari;

-- Connect to the database
\c datafari;


CREATE TABLE access_tokens (
  "username" TEXT,
  "api" TEXT,
  "identifier" TEXT,
  "a_token" TEXT,
  "last_refresh" TIMESTAMP,
  PRIMARY KEY (username, api, identifier)
);

CREATE TABLE oauth_access_tokens (
  "accesstokenid" text PRIMARY KEY,
  "accesstoken" TEXT
);

CREATE TABLE oauth_authentications (
  "accesstokenid" text PRIMARY KEY,
  "authentication" TEXT
);

CREATE TABLE oauth_auth_to_access_token (
  "authkey" text PRIMARY KEY,
  "accesstoken" TEXT
);

CREATE TABLE oauth_client_id_to_access_token (
  "clientid" TEXT,
  "accesstoken" TEXT,
  PRIMARY KEY (clientid, accesstoken)
);

CREATE TABLE oauth_refresh_tokens (
  "refreshtokenid" text PRIMARY KEY,
  "refreshtoken" TEXT
);

CREATE TABLE oauth_refresh_token_auth (
  "refreshtokenid" text PRIMARY KEY,
  "authentication" TEXT
);

CREATE TABLE oauth_refresh_token_to_access_token (
  "refreshtokenid" text PRIMARY KEY,
  "accesstokenid" TEXT
);

CREATE TABLE oauth_username_to_access_token (
  "approvalkey" TEXT,
  "accesstoken" TEXT,
  PRIMARY KEY (approvalkey, accesstoken)
);

CREATE TABLE oauth_clients (
  "clientid" text PRIMARY KEY,
  "clientsecret" TEXT,
  "accesstokenvalidityseconds" INTEGER,
  "refreshtokenvalidityseconds" INTEGER
);

CREATE TABLE alerts (
  "id" uuid PRIMARY KEY,
  "keyword" TEXT,
  "filters" TEXT,
  "core" TEXT,
  "frequency" TEXT,
  "mail" TEXT,
  "subject" TEXT,
  "user" TEXT,
  "last_refresh" TIMESTAMP
);

CREATE TABLE department (
  "username" varchar PRIMARY KEY,
  "department" TEXT,
  "last_refresh" TIMESTAMP
);

CREATE TABLE favorite (
  "username" TEXT,
  "document_id" TEXT,
  "document_title" TEXT,
  "last_refresh" TIMESTAMP,
  PRIMARY KEY (username, document_id)
);

CREATE TABLE search (
  "username" TEXT,
  "name" TEXT,
  "request" TEXT,
  "last_refresh" TIMESTAMP,
  PRIMARY KEY (username, name, request)
);

CREATE TABLE "like" (
  "username" TEXT,
  "document_id" TEXT,
  "last_refresh" TIMESTAMP,
  PRIMARY KEY (username, document_id)
);

CREATE TABLE "user" (
  "username" varchar PRIMARY KEY,
  "password" TEXT,
  "isImported" BOOLEAN,
  "last_refresh" TIMESTAMP
);

CREATE TABLE lang (
  "username" varchar PRIMARY KEY,
  "lang" TEXT,
  "last_refresh" TIMESTAMP
);

CREATE TABLE ui_config (
  "username" varchar PRIMARY KEY,
  "ui_config" TEXT,
  "last_refresh" TIMESTAMP
);

CREATE TABLE role (
  "username" TEXT,
  "role" TEXT,
  "last_refresh" TIMESTAMP,
  PRIMARY KEY (username, role)
);

CREATE TABLE crawled_document (
  "id" TEXT,
  "lastcheck" TIMESTAMP,
  "processed" BOOLEAN,
  "errored" BOOLEAN,
  "doc_path" TEXT,
  "solr_core" TEXT,
  "solr_update_handler" TEXT,
  PRIMARY KEY (processed, errored)
);

CREATE TABLE user_search_actions (
  query_id TEXT,
  user_id TEXT,
  action TEXT,
  time_stamp TIMESTAMP,
  parameters TEXT,
  PRIMARY KEY (query_id, time_stamp)
);

CREATE TABLE document_features (
  document_id TEXT PRIMARY KEY,
  document_rights JSONB,
  clicks JSONB,
  time_to_click JSONB
);

CREATE TABLE query_document_features (
  query TEXT,
  document_id TEXT,
  clicks JSONB,
  time_to_click JSONB,
  PRIMARY KEY (query, document_id)
);

CREATE TABLE licence (
  "licence_id" TEXT,
  "licence" BYTEA,
  PRIMARY KEY (licence_id)
);

CREATE INDEX alerts_user_idx ON alerts("user");
CREATE INDEX user_isimported_idx ON "user"("isImported");
CREATE INDEX user_search_actions_userid_idx ON user_search_actions("user_id");
CREATE INDEX favorite_username_idx ON favorite("username");
CREATE INDEX favorite_document_idx ON favorite("document_id");
CREATE INDEX like_username_idx ON "like"("username");
CREATE INDEX like_document_idx ON "like"("document_id");
CREATE INDEX search_username_idx ON search("username");
CREATE INDEX role_username_idx ON role("username");
CREATE INDEX role_role_idx ON role("role");
CREATE INDEX lang_username_idx ON lang("username");
CREATE INDEX ui_config_username_idx ON ui_config("username");
CREATE INDEX department_username_idx ON department("username");
CREATE INDEX alerts_last_refresh_idx ON alerts("last_refresh");
CREATE INDEX crawled_document_lastcheck_idx ON crawled_document("lastcheck");
CREATE INDEX user_search_actions_timestamp_idx ON user_search_actions("time_stamp");
CREATE INDEX query_document_features_doc_idx ON query_document_features("document_id");
CREATE INDEX query_document_features_query_idx ON query_document_features("query");
CREATE INDEX document_features_rights_idx ON document_features USING GIN("document_rights");
CREATE INDEX document_features_clicks_idx ON document_features USING GIN("clicks");
CREATE INDEX document_features_ttc_idx ON document_features USING GIN("time_to_click");