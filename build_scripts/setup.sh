#!/usr/bin/env bash

## Java setup
yum -y install java-1.8.0-openjdk-devel
cp /vagrant/build_scripts/java.sh /etc/profile.d/
source /etc/profile.d/java.sh

## Maven setup
wget http://mirror.jax.hugeserver.com/apache/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.zip -P /tmp
unzip /tmp/apache-maven-3.5.0-bin.zip
cp /vagrant/build_scripts/maven.sh /etc/profile.d/
source /etc/profile.d/maven.sh

# Configure proxy for HTTP requests in Maven
mv /vagrant/apache-maven-3.5.0/conf/settings.xml /vagrant/apache-maven-3.5.0/conf/settings.xml.orig
ln -s /vagrant/build_scripts/settings.xml /vagrant/apache-maven-3.5.0/conf/

## Ant setup
wget http://apache.spinellicreations.com//ant/binaries/apache-ant-1.10.1-bin.tar.gz -P /tmp
tar xzvf /tmp/apache-ant-1.10.1-bin.tar.gz
cp /vagrant/build_scripts/ant.sh /etc/profile.d/
source /etc/profile.d/ant.sh

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

## Tomcat setup
wget http://apache.mirrors.hoobly.com/tomcat/tomcat-8/v8.5.23/bin/apache-tomcat-8.5.23.tar.gz -P /tmp
tar xzvf /tmp/apache-tomcat-8.5.23.tar.gz

# use UTF-8 as its default file encoding for international character support
# JAVA_OPTS="-Xmx512M -Xms64M -Dfile.encoding=UTF-8"

mv apache-tomcat-8.5.23/conf/server.xml apache-tomcat-8.5.23/conf/server.xml.orig
ln -s /vagrant/build_scripts/server.xml apache-tomcat-8.5.23/conf/server.xml

# tomcat owner must have read/write access to DSpace installation directory
# chown -R dspace:dspace apache-tomcat-8.5.23

createuser --username=postgres -h 127.0.0.1 dspace
createdb --username=postgres -h 127.0.0.1 --owner=dspace --encoding=UNICODE dspace
psql --username=postgres -h 127.0.0.1 dspace -c "CREATE EXTENSION pgcrypto;"

ln -s /vagrant/build_scripts/local.cfg /vagrant/dspace/config/

# Build installation package
cd /vagrant
mvn package

# Deploy web applications
ln -s /vagrant/build_scripts/jspui.xml /vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost/jspui.xml
ln -s /vagrant/build_scripts/xmlui.xml /vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost/xmlui.xml
ln -s /vagrant/build_scripts/solr.xml /vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost/solr.xml