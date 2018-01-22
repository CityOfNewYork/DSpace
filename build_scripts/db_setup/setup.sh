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


# Create postgres key and certificates
openssl req \
       -newkey rsa:4096 -nodes -keyout /vagrant/build_scripts/db_setup/server.key \
       -x509 -days 365 -out /vagrant/build_scripts/db_setup/server.crt -subj "/C=US/ST=New York/L=New York/O=NYC Department of Records and Information Services/OU=IT/CN=dspace.dev"
cp /vagrant/build_scripts/db_setup/server.crt /vagrant/build_scripts/db_setup/root.crt

mv /vagrant/build_scripts/db_setup/root.crt /data/postgres
chmod 400 /data/postgres/root.crt
chown postgres:postgres /data/postgres/root.crt
mv /vagrant/build_scripts/db_setup/server.crt /data/postgres
chmod 600 /data/postgres/server.crt
chown postgres:postgres /data/postgres/server.crt
mv /vagrant/build_scripts/db_setup/server.key /data/postgres
chmod 600 /data/postgres/server.key
chown postgres:postgres /data/postgres/server.key

if [ "$1" != single_server ]; then
  # 8a. Setup Client Certificates for App Server
  mkdir -p /home/vagrant/.postgresql
  openssl req -new -nodes -keyout client.key -out client.csr -subj "/C=US/ST=New York/L=New York/O=NYC Department of Records and Information Services/OU=IT/CN=dspace.dev"
  openssl x509 -req -CAcreateserial -in client.csr -CA /data/postgres/root.crt -CAkey /data/postgres/server.key -out client.crt
  chown -R vagrant:vagrant /home/vagrant/.postgresql/
fi

# Start Postgres
sudo service rh-postgresql95-postgresql start

# MANUAL STEP: Add server.crt to the Java Keystore
# sudo keytool -import -alias dspace_db_ssl -keystore /usr/lib/jvm/java-1.8.0-openjdk.x86_64/jre/lib/security/cacerts -file /data/postgres/server.crt

