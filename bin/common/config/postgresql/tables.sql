-- Drop and recreate the database (à faire en dehors de ce script si nécessaire)
-- DROP DATABASE IF EXISTS datafari;
-- CREATE DATABASE datafari;
-- \c datafari

CREATE TABLE access_tokens (
    username VARCHAR(255) NOT NULL,
    api VARCHAR(255) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    a_token VARCHAR(2048),
    last_refresh TIMESTAMP,
    PRIMARY KEY (username, api, identifier)
);

CREATE TABLE oauth_access_tokens (
    access_token_id VARCHAR(255) PRIMARY KEY,
    access_token VARCHAR(255)
);

CREATE TABLE oauth_authentications (
    access_token_id VARCHAR(255) PRIMARY KEY,
    authentication VARCHAR(255)
);

CREATE TABLE oauth_auth_to_access_token (
    auth_key VARCHAR(255) PRIMARY KEY,
    access_token VARCHAR(255)
);

CREATE TABLE oauth_client_id_to_access_token (
    client_id VARCHAR(255) NOT NULL,
    access_token VARCHAR(255) NOT NULL,
    PRIMARY KEY (client_id, access_token)
);

CREATE TABLE oauth_refresh_tokens (
    refresh_token_id VARCHAR(255) PRIMARY KEY,
    refresh_token VARCHAR(255)
);

CREATE TABLE oauth_refresh_token_auth (
    refresh_token_id TEXT PRIMARY KEY,
    authentication TEXT
);

CREATE TABLE oauth_refresh_token_to_access_token (
    refresh_token_id VARCHAR(255) PRIMARY KEY,
    access_token_id VARCHAR(255)
);

CREATE TABLE oauth_username_to_access_token (
    approval_key VARCHAR(255) NOT NULL,
    access_token VARCHAR(255) NOT NULL,
    PRIMARY KEY (approval_key, access_token)
);

CREATE TABLE oauth_clients (
    client_id TEXT PRIMARY KEY,
    client_secret TEXT,
    access_token_validity_seconds INTEGER,
    refresh_token_validity_seconds INTEGER
);

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    keyword VARCHAR(255),
    filters TEXT,
    core VARCHAR(255),
    frequency VARCHAR(255),
    mail VARCHAR(255),
    subject VARCHAR(255),
    user VARCHAR(255),
    last_refresh TIMESTAMP
);

CREATE TABLE department (
    username VARCHAR(255) PRIMARY KEY,
    department VARCHAR(255),
    last_refresh TIMESTAMP
);

CREATE TABLE favorite (
    username VARCHAR(255) NOT NULL,
    document_id VARCHAR(255) NOT NULL,
    document_title TEXT,
    last_refresh TIMESTAMP,
    PRIMARY KEY (username, document_id)
);

CREATE TABLE search (
    username VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    request TEXT NOT NULL,
    last_refresh TIMESTAMP,
    CONSTRAINT search_unique_key UNIQUE (username, name, request)
);

CREATE TABLE liked (
    username VARCHAR(255) NOT NULL,
    document_id VARCHAR(255) NOT NULL,
    last_refresh TIMESTAMP,
    PRIMARY KEY (username, document_id)
);

CREATE TABLE users (
    username VARCHAR PRIMARY KEY,
    password VARCHAR NOT NULL,
    is_imported BOOLEAN NOT NULL DEFAULT false,
    last_refresh TIMESTAMP NOT NULL
);

CREATE TABLE lang (
    username VARCHAR(255) PRIMARY KEY,
    lang VARCHAR(10),
    last_refresh TIMESTAMP
);

CREATE TABLE ui_config (
    username VARCHAR PRIMARY KEY,
    ui_config TEXT NOT NULL,
    last_refresh TIMESTAMP NOT NULL
);

CREATE TABLE roles (
    username VARCHAR NOT NULL,
    role VARCHAR NOT NULL,
    last_refresh TIMESTAMP NOT NULL,
    PRIMARY KEY (username, role),
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);

CREATE TABLE crawled_document (
    id TEXT,
    last_check TIMESTAMP,
    processed BOOLEAN,
    errored BOOLEAN,
    doc_path TEXT,
    solr_core TEXT,
    solr_update_handler TEXT,
    PRIMARY KEY (processed, errored)
);

CREATE TABLE user_search_actions (
    query_id VARCHAR NOT NULL,
    user_id VARCHAR NOT NULL,
    time_stamp TIMESTAMP NOT NULL,
    action VARCHAR NOT NULL,
    parameters JSONB NOT NULL,
    CONSTRAINT user_search_actions_pk PRIMARY KEY (query_id, time_stamp)
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
    licence_id VARCHAR(255) PRIMARY KEY,
    licence BYTEA
);

-- Indexes
CREATE INDEX alerts_user_idx ON alerts(user);
CREATE INDEX users_is_imported_idx ON users(is_imported);
CREATE INDEX user_search_actions_userid_idx ON user_search_actions(user_id);
CREATE INDEX favorite_username_idx ON favorite(username);
CREATE INDEX favorite_document_idx ON favorite(document_id);
CREATE INDEX liked_username_idx ON liked(username);
CREATE INDEX liked_document_idx ON liked(document_id);
CREATE INDEX search_username_idx ON search(username);
CREATE INDEX roles_username_idx ON roles(username);
CREATE INDEX roles_role_idx ON roles(role);
CREATE INDEX lang_username_idx ON lang(username);
CREATE INDEX ui_config_username_idx ON ui_config(username);
CREATE INDEX department_username_idx ON department(username);
CREATE INDEX alerts_last_refresh_idx ON alerts(last_refresh);
CREATE INDEX crawled_document_lastcheck_idx ON crawled_document(last_check);
CREATE INDEX user_search_actions_timestamp_idx ON user_search_actions(time_stamp);
CREATE INDEX query_document_features_doc_idx ON query_document_features(document_id);
CREATE INDEX query_document_features_query_idx ON query_document_features(query);
CREATE INDEX document_features_rights_idx ON document_features USING GIN(document_rights);
CREATE INDEX document_features_clicks_idx ON document_features USING GIN(clicks);
CREATE INDEX document_features_ttc_idx ON document_features USING GIN(time_to_click);