/* Add service kind */ 
INSERT INTO service_categories (kind) VALUES (0);
INSERT INTO service_categories (kind) VALUES (1);
INSERT INTO service_categories (kind) VALUES (2);
INSERT INTO service_categories (kind) VALUES (3);
INSERT INTO service_categories (kind) VALUES (4);
INSERT INTO service_categories (kind) VALUES (5);
INSERT INTO service_categories (kind) VALUES (6);
INSERT INTO service_categories (kind) VALUES (7);
INSERT INTO service_categories (kind) VALUES (8);
INSERT INTO service_categories (kind) VALUES (9);

/* Add retail customers */ 
INSERT INTO retail_customers (username, first_name, last_name, password, enabled, role) VALUES ('alan',    'Alan',    'Turing',       'koala', TRUE, 'ROLE_USER');
INSERT INTO retail_customers (username, first_name, last_name, password, enabled, role) VALUES ('marissa', 'Marissa',  'Meyer',       'koala', TRUE, 'ROLE_USER');
INSERT INTO retail_customers (username, first_name, last_name, password, enabled, role) VALUES ('sam',     'Sam',      'White',       'koala', TRUE, 'ROLE_USER');
INSERT INTO retail_customers (username, first_name, last_name, password, enabled, role) VALUES ('grace',   'Grace',    'Hopper',      'koala', TRUE, 'ROLE_CUSTODIAN');

/* Add application_information */ 
INSERT INTO application_information (id, uuid, dataCustodianId, clientId, thirdPartyApplicationName, clientSecret, thirdPartyScopeSelectionScreenURI, authorizationServerAuthorizationEndpoint, authorizationServerTokenEndpoint, redirectUri) VALUES (1,'550e8400-e29b-41d4-a716-4466554413a0', 'data_custodian', 'third_party', 'ConEdison (GreenButtonData)', 'secret', 'http://services.greenbuttondata.org/DataCustodian/RetailCustomer/ScopeSelectionList', 'http://services.greenbuttondata.org/DataCustodian/oauth/authorize', 'http://services.greenbuttondata.org/DataCustodian/oauth/token', 'http://services.greenbuttondata.org/ThirdParty/espi/1_1/OAuthCallBack');
INSERT INTO application_information (id, uuid, dataCustodianId, clientId, thirdPartyApplicationName, clientSecret, thirdPartyScopeSelectionScreenURI, authorizationServerAuthorizationEndpoint, authorizationServerTokenEndpoint, redirectUri) VALUES (2, '550e8400-e29b-41d4-a716-4466554413a1', 'data_custodian2', 'third_party', 'NJ Electric (GreenButtonData)', 'secret', 'http://services.greenbuttondata.org/DataCustodian/RetailCustomer/ScopeSelectionList', 'http://services.greenbuttondata.org/DataCustodian/oauth/authorize', 'http://services.greenbuttondata.org/DataCustodian/oauth/token', 'http://services.greenbuttondata.org/DataCustodian/espi/1_1/OAuthCallBack');

/* Add application_information_scopes */ 
INSERT INTO application_information_scopes (application_information_id, scope) VALUES (1, 'FB=4_5_15;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13');
INSERT INTO application_information_scopes (application_information_id, scope) VALUES (1, 'FB=4_5_16;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13');
INSERT INTO application_information_scopes (application_information_id, scope) VALUES (2, 'FB=4_5_16;IntervalDuration=3600;BlockDuration=monthly;HistoryLength=13');
