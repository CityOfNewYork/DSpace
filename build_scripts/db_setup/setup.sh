#!/usr/bin/env bash

# Install Postgres
yum -y install rh-postgresql95
yum -y install rh-postgresql95-postgresql-contrib  # pgcrypto extension

# Autostart Postgres
chkconfig rh-postgresql95-postgresql on

# Setup data directory for Postgres (store data from Postgres where it's not normally stored)
mkdir -p /data/postgres
chown -R postgres:postgres /data/postgres

# Copy script (enable postgres commands in command line) to /etc/profile.d
cp /vagrant/build_scripts/db_setup/postgres.sh /etc/profile.d/
source /etc/profile.d/postgres.sh

postgresql-setup --initdb

# Setup data directory (move data files into created Postgres data directory)
mv /var/opt/rh/rh-postgresql95/lib/pgsql/data/* /data/postgres/
rm -rf /var/opt/rh/rh-postgresql95/lib/pgsql/data
ln -s /data/postgres /var/opt/rh/rh-postgresql95/lib/pgsql/data
chmod 700 /var/opt/rh/rh-postgresql95/lib/pgsql/data

# Setup Postgres configurations
mv /data/postgres/postgresql.conf /data/postgres/postgresql.conf.orig
mv /data/postgres/pg_hba.conf /data/postgres/pg_hba.conf.orig
cp -r /vagrant/build_scripts/db_setup/postgresql.conf /data/postgres/
cp -r /vagrant/build_scripts/db_setup/pg_hba.conf /data/postgres/
chown -R postgres:postgres /data/postgres

# Link Postgres libraries
ln -s /opt/rh/rh-postgresql95/root/usr/lib64/libpq.so.rh-postgresql95-5 /usr/lib64/libpq.so.rh-postgresql95-5
ln -s /opt/rh/rh-postgresql95/root/usr/lib64/libpq.so.rh-postgresql95-5 /usr/lib/libpq.so.rh-postgresql95-5

# Create backup directory for Postgres
mkdir /backup
chown postgres:postgres /backup

# Start Postgres
sudo service rh-postgresql95-postgresql start

