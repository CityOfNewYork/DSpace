#!/usr/bin/env bash

## Postgres setup
yum -y install rh-postgresql95
yum -y install rh-postgresql95-postgresql-contrib
chkconfig rh-postgresql95-postgresql on

mkdir -p /data/postgres
chown -R postgres:postgres /data/postgres
cp /vagrant/build_scripts/postgres.sh /etc/profile.d/
source /etc/profile.d/postgres.sh

postgresql-setup --initdb

mv /var/opt/rh/rh-postgresql95/lib/pgsql/data/* /data/postgres/
rm -rf /var/opt/rh/rh-postgresql95/lib/pgsql/data
ln -s /data/postgres /var/opt/rh/rh-postgresql95/lib/pgsql/data
chmod 700 /var/opt/rh/rh-postgresql95/lib/pgsql/data

mv /data/postgres/postgresql.conf /data/postgres/postgresql.conf.orig
mv /data/postgres/pg_hba.conf /data/postgres/pg_hba.conf.orig
cp -r /vagrant/build_scripts/postgresql.conf /data/postgres/
cp -r /vagrant/build_scripts/pg_hba.conf /data/postgres/
chown -R postgres:postgres /data/postgres


ln -s /opt/rh/rh-postgresql95/root/usr/lib64/libpq.so.rh-postgresql95-5 /usr/lib64/libpq.so.rh-postgresql95-5
ln -s /opt/rh/rh-postgresql95/root/usr/lib64/libpq.so.rh-postgresql95-5 /usr/lib/libpq.so.rh-postgresql95-5

sudo service rh-postgresql95-postgresql start

createuser --username=postgres -h 127.0.0.1 dspace
createdb --username=postgres -h 127.0.0.1 --owner=dspace --encoding=UNICODE dspace
psql --username=postgres -h 127.0.0.1 dspace -c "CREATE EXTENSION pgcrypto;"