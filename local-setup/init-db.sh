#!/usr/bin/env bash

set -e

# load the gzipped dump and ignore errors
cat /tmp/person.sql.gz | gunzip | psql -v ON_ERROR_STOP=0 -h localhost --username "$POSTGRES_USER"

# create unique index
psql -v ON_ERROR_STOP=1 -h localhost --username "$POSTGRES_USER" <<-EOSQL
    ALTER TABLE person ALTER dob TYPE VARCHAR(10) USING(dob::VARCHAR(10));
    UPDATE person SET dob = '' WHERE dob IS NULL;
    UPDATE person SET lname = '' WHERE lname IS NULL;
    ALTER TABLE person ADD PRIMARY KEY (fname, lname, dob);
EOSQL
