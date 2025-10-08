CREATE DATABASE issuer_db;

CREATE ROLE issuer_user LOGIN PASSWORD 'iss123';

ALTER DATABASE issuer_db OWNER TO issuer_user;