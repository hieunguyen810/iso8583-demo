CREATE DATABASE acquirer_db;
CREATE DATABASE issuer_db;

CREATE ROLE acquirer_user LOGIN PASSWORD 'acq123';
CREATE ROLE issuer_user LOGIN PASSWORD 'iss123';

ALTER DATABASE acquirer_db OWNER TO acquirer_user;
ALTER DATABASE issuer_db OWNER TO issuer_user;

-- Connect to acquirer_db and create tables
\c acquirer_db;

CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    source_number VARCHAR(20),
    target_number VARCHAR(20),
    status VARCHAR(10),
    amount DECIMAL(15,2),
    transaction_time TIMESTAMP,
    update_time TIMESTAMP,
    stan VARCHAR(6),
    mti VARCHAR(4)
);

CREATE TABLE IF NOT EXISTS transaction_events (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT,
    event_type VARCHAR(20),
    iso_message TEXT,
    event_time TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

CREATE INDEX idx_transactions_stan ON transactions(stan);
CREATE INDEX idx_transactions_time ON transactions(transaction_time);
CREATE INDEX idx_events_transaction_id ON transaction_events(transaction_id);

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO acquirer_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO acquirer_user;
