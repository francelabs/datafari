DROP KEYSPACE IF EXISTS datafari;
CREATE KEYSPACE datafari
WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
use datafari;

CREATE TABLE access_tokens (
  username text,
  api text,
  identifier text,
  a_token text,
  last_refresh timestamp,
  PRIMARY KEY (username, api, identifier)
);

CREATE TABLE oauth_access_tokens (
  accesstokenid text PRIMARY KEY,
  accesstoken text
);

CREATE TABLE oauth_authentications (
  accesstokenid text PRIMARY KEY,
  authentication text
);

CREATE TABLE oauth_auth_to_access_token (
  authkey text PRIMARY KEY,
  accesstoken text
);

CREATE TABLE oauth_client_id_to_access_token (
  clientid text,
  accesstoken text,
  PRIMARY KEY (clientid, accesstoken )
);

CREATE TABLE oauth_refresh_tokens (
  refreshtokenid text PRIMARY KEY,
  refreshtoken text
);

CREATE TABLE oauth_refresh_token_auth (
  refreshtokenid text PRIMARY KEY,
  authentication text
);

CREATE TABLE oauth_refresh_token_to_access_token (
  refreshtokenid text PRIMARY KEY,
  accesstokenid text
);

CREATE TABLE oauth_username_to_access_token (
  approvalkey text,
  accesstoken text,
  PRIMARY KEY (approvalkey, accesstoken )
);

CREATE TABLE oauth_clients (
  clientid text PRIMARY KEY,
  clientsecret text,
  resourceids set<text>,
  scope set<text>,
  authorizedgranttypes set<text>,
  registeredredirecturi set<text>,
  authorities set<text>,
  accesstokenvalidityseconds int,
  refreshtokenvalidityseconds int
);

CREATE TABLE alerts (
  id uuid PRIMARY KEY,
  keyword varchar,
  filters varchar,
  core varchar,
  frequency varchar,
  mail varchar,
  subject varchar,
  user varchar,
  last_refresh timestamp
);
CREATE INDEX alerts_search on alerts(user);

CREATE TABLE department (
  username varchar PRIMARY KEY,
  department varchar,
  last_refresh timestamp
);


CREATE TABLE favorite (
  username varchar,
  document_id varchar,
  document_title varchar,
  last_refresh timestamp,
  PRIMARY KEY (username, document_id )
);

CREATE TABLE search (
  username varchar,
  name varchar,
  request varchar,
  last_refresh timestamp,
  PRIMARY KEY (username, name, request )
);

CREATE TABLE like (
  username varchar,
  document_id varchar,
  last_refresh timestamp,
  PRIMARY KEY (username, document_id )
);

CREATE TABLE user (
  username varchar PRIMARY KEY,
  password varchar,
  isImported boolean,
  last_refresh timestamp
);
CREATE INDEX user_search on user(isImported);

CREATE TABLE lang (
	username varchar PRIMARY KEY,
	lang varchar,
	last_refresh timestamp
);

CREATE TABLE ui_config (
	username varchar PRIMARY KEY,
	ui_config varchar,
	last_refresh timestamp
);

CREATE TABLE user_history (
  username varchar PRIMARY KEY,
  history list<text>,
  last_refresh timestamp
);

CREATE TABLE role (
  username varchar,
  role varchar,
  last_refresh timestamp,
  PRIMARY KEY (username, role )
);

CREATE TABLE crawled_document (
    id text,
    lastcheck timestamp,
    processed boolean,
    errored boolean,
    doc_path text,
    fields frozen<map<text, list<text>>>,
    solr_core text,
    solr_update_handler text,
    annotators list<text>,
PRIMARY KEY ((processed,errored), lastcheck, id)
) WITH CLUSTERING ORDER BY (lastcheck ASC)
;

CREATE TABLE user_search_actions (
  query_id text,
  user_id text,
  action text,
  time_stamp timestamp,
  parameters text,
  PRIMARY KEY (query_id, time_stamp)
);
CREATE INDEX user_search_sa on user_search_actions(user_id);

CREATE TABLE document_features (
  document_id text,
  document_rights set<text>,
  clicks set<frozen<tuple<int,int>>>,
  time_to_click set<frozen<tuple<int,int,int>>>,
  PRIMARY KEY (document_id)
);

CREATE TABLE query_document_features (
  query text,
  document_id text,
  clicks set<frozen<tuple<int,int>>>,
  time_to_click set<frozen <tuple<int,int,int>>>,
  PRIMARY KEY (query, document_id)
);

CREATE TABLE licence (
  licence_id text,
  licence blob,
  PRIMARY KEY (licence_id)
);

INSERT INTO access_tokens (username, api, identifier, a_token, last_refresh)
VALUES ('john', 'An api 1', 'An identifier 1', 'A token 1', toTimeStamp(NOW()));
INSERT INTO access_tokens (username, api, identifier, a_token, last_refresh)
VALUES ('john', 'An api 2', 'An identifier 2', 'A token 2', toTimeStamp(NOW()));

INSERT INTO department (username, department, last_refresh)
VALUES ('john', 'department 1', toTimeStamp(NOW()));
INSERT INTO department (username, department, last_refresh)
VALUES ('jack', 'department 2', toTimeStamp(NOW()));

INSERT INTO alerts (id, keyword, core, filters, frequency, mail, subject, user, last_refresh)
VALUES (uuid(), 'keyword 1', 'core 1', 'filters 1', 'frequency 1', 'mail 1', 'subject 1', 'john', toTimeStamp(NOW()));
INSERT INTO alerts (id, keyword, core, filters, frequency, mail, subject, user, last_refresh)
VALUES (uuid(), 'keyword 2', 'core 2', 'filters 2', 'frequency 2', 'mail 2', 'subject 2', 'alice', toTimeStamp(NOW()));

INSERT INTO favorite (username, document_id, document_title, last_refresh)
VALUES ('john', 'document_id 1', 'document_title 1', toTimeStamp(NOW()));
INSERT INTO favorite (username, document_id, document_title, last_refresh)
VALUES ('john', 'document_id 2', 'document_title 2', toTimeStamp(NOW()));

INSERT INTO like (username, document_id, last_refresh)
VALUES ('john', 'document_id 2', toTimeStamp(NOW()));
INSERT INTO like (username, document_id, last_refresh)
VALUES ('john', 'document_id 3', toTimeStamp(NOW()));

INSERT INTO lang (username, lang, last_refresh)
VALUES ('john', 'lang 1', toTimeStamp(NOW()));

INSERT INTO search (username, name, request, last_refresh)
VALUES ('john', 'name 1', 'request 1',toTimeStamp(NOW()));
INSERT INTO search (username, name, request, last_refresh)
VALUES ('john', 'name 2', 'request 2',toTimeStamp(NOW()));
INSERT INTO search (username, name, request, last_refresh)
VALUES ('alice', 'name 3', 'request 3',toTimeStamp(NOW()));

INSERT INTO user_search_actions (user_id, query_id, action, parameters, time_stamp)
VALUES ('alice', 'query 1', 'action 1', '{"param1":"value1"}','2022-04-01T12:34:56.000+0000');
INSERT INTO user_search_actions (user_id, query_id, action, parameters, time_stamp)
VALUES ('alice', 'query 2', 'action 2', '{"param2":"value2"}','2022-04-01T12:34:56.001+0000');

INSERT INTO ui_config (username, ui_config, last_refresh)
VALUES ('john', '{"left": [],"center": [],"right": []}', toTimeStamp(NOW()));

INSERT INTO user (username, password, isImported, last_refresh)
VALUES ('john', 'password 1', false, toTimeStamp(NOW()));

INSERT INTO role (username, role, last_refresh)
VALUES ('john', 'SearchAdministrator', toTimeStamp(NOW()));

INSERT INTO user_history (username, history, last_refresh)
VALUES ('john', [$$enron energy$$,$$france labs search engine$$], toTimeStamp(NOW()));