#!/usr/bin/env bash

set -e

[ -f $PWD/local-setup/person.sql.gz ] || { echo "Make sure person.sql.gz exists in local-setup."; exit 1; }
[ -f $PWD/local-setup/update-file.xml.gz ] || { echo "Make sure update-file.xml.gz exists in local-setup."; exit 1; }

docker-compose up -d
