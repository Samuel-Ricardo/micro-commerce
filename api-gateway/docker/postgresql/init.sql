SELECT 'CREATE DATABASE order_service'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'order_service'
)\gexec

--GRANT ALL PRIVILEGES ON DATABASE order_service TO admin;
