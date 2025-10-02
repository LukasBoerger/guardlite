CREATE TYPE status_t AS ENUM ('GREEN','YELLOW','RED','UNKNOWN');

CREATE TABLE users (
                       id CHAR(36) PRIMARY KEY,
                       email TEXT UNIQUE NOT NULL,
                       password_hash TEXT NOT NULL,
                       role TEXT NOT NULL DEFAULT 'USER',
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE websites (
                          id CHAR(36) PRIMARY KEY,
                          owner_user_id CHAR(36) NOT NULL REFERENCES users(id),
                          url TEXT NOT NULL,
                          cms TEXT NOT NULL DEFAULT 'WORDPRESS',
                          active BOOLEAN NOT NULL DEFAULT true,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE checks (
                        id CHAR(36) PRIMARY KEY,
                        website_id CHAR(36) NOT NULL REFERENCES websites(id) ON DELETE CASCADE,
                        type TEXT NOT NULL,
                        cadence_cron TEXT NOT NULL,
                        enabled BOOLEAN NOT NULL DEFAULT true,
                        last_run_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE check_results (
                               id CHAR(36) PRIMARY KEY,
                               check_id CHAR(36) NOT NULL REFERENCES checks(id) ON DELETE CASCADE,
                               run_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               status VARCHAR(255) NOT NULL,
                               payload_json VARCHAR(255) NOT NULL,
                               advice_text TEXT
);

CREATE TABLE alerts (
                        id CHAR(36) PRIMARY KEY,
                        website_id CHAR(36) NOT NULL REFERENCES websites(id) ON DELETE CASCADE,
                        check_id CHAR(36) REFERENCES checks(id),
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        status VARCHAR(255) NOT NULL DEFAULT 'OPEN',
                        message TEXT NOT NULL
);
