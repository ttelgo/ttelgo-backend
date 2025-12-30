-- Run this in pgAdmin or DBeaver to clean the database
-- Connect to 'postgres' database first, then run these commands:

-- Terminate all connections
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'ttelgo_dev' AND pid <> pg_backend_pid();

-- Drop and recreate database
DROP DATABASE IF EXISTS ttelgo_dev;
CREATE DATABASE ttelgo_dev;

