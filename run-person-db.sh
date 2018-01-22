#!/usr/bin/env bash

ROOT_DIR=$(pwd)

docker run --rm -p5432:5432 --name lambdawerk-backend-test -d postgres

cat ${ROOT_DIR}/person.sql.gz | gunzip | psql -h localhost -U postgres -w
