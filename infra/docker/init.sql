SELECT 'CREATE DATABASE taskflow' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'taskflow')\gexec
SELECT 'CREATE DATABASE taskflow_users' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'taskflow_users')\gexec
GRANT ALL PRIVILEGES ON DATABASE taskflow TO taskflow;
GRANT ALL PRIVILEGES ON DATABASE taskflow_users TO taskflow;