#!/usr/bin/env bash

# Create symlink of DSpace configuration file
ln -s /vagrant/build_scripts/dspace_install/local.cfg /vagrant/dspace/config/local.cfg

# Create symlink of DSpace LDAP configuration file
mv /vagrant/dspace/config/modules/authentication-ldap.cfg /vagrant/dspace/config/modules/authentication-ldap.cfg.orig
ln -s /vagrant/build_scripts/dspace_install/authentication-ldap.cfg /vagrant/dspace/config/modules/authentication-ldap.cfg

# Create assetstore directory in /data to store content from DSpace
mkdir -p /data/assetstore
chown -R vagrant:vagrant /data/assetstore

# Build installation package
cd /vagrant
mvn package

# Install DSpace
cd /vagrant/dspace/target/dspace-installer
ant fresh_install

# DSpace installation directory and Tomcat directory must have same owner
chown -R vagrant:vagrant /home/vagrant/dspace

# Manually install GeoLite database file
wget http://geolite.maxmind.com/download/geoip/database/GeoLiteCity.dat.gz -P /tmp
gunzip -c /tmp/GeoLiteCity.dat.gz > /home/vagrant/dspace/config/GeoLiteCity.dat

# Deploy web applications
mkdir -p /home/vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost
ln -s /vagrant/build_scripts/dspace_install/jspui.xml /home/vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost/jspui.xml
ln -s /vagrant/build_scripts/dspace_install/solr.xml /home/vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost/solr.xml
