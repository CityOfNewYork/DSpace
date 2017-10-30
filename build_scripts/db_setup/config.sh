#!/usr/bin/env bash

# Create user and database
createuser --username=postgres -h 127.0.0.1 dspace
createdb --username=postgres -h 127.0.0.1 --owner=dspace --encoding=UNICODE dspace
psql --username=postgres -h 127.0.0.1 dspace -c "CREATE EXTENSION pgcrypto;"