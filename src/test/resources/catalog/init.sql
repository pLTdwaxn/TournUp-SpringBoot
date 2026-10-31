CREATE ROLE catalog_test_reader
    LOGIN PASSWORD 'catalog_test_reader_password'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOREPLICATION
    NOBYPASSRLS;

REVOKE CONNECT, TEMPORARY ON DATABASE catalog_test FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

GRANT CONNECT ON DATABASE catalog_test TO catalog_test_reader;
GRANT USAGE ON SCHEMA public TO catalog_test_reader;

ALTER DEFAULT PRIVILEGES FOR ROLE catalog_test_owner IN SCHEMA public
GRANT SELECT ON TABLES TO catalog_test_reader;
