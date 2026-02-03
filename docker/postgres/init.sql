CREATE DATABASE wallet_db_0;
CREATE DATABASE wallet_db_1;

CREATE USER sharding_user WITH PASSWORD 'SecurePass123!';
GRANT ALL PRIVILEGES ON DATABASE wallet_db_0 TO sharding_user;
GRANT ALL PRIVILEGES ON DATABASE wallet_db_1 TO sharding_user;