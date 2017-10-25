#!/usr/bin/env bash

# Create symlink of DSpace configuration file
ln -s /vagrant/build_scripts/local.cfg /vagrant/dspace/config/local.cfg

# Build installation package
cd /vagrant
mvn package

# Install DSpace
cd /vagrant/dspace/target/dspace-installer
ant fresh_install

# DSpace installation directory and Tomcat directory must have same owner
chown -R vagrant:vagrant /dspace

# Deploy web applications
mkdir -p /vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost
ln -s /vagrant/build_scripts/dspace_install/jspui.xml /vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost/jspui.xml
ln -s /vagrant/build_scripts/dspace_install/xmlui.xml /vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost/xmlui.xml
ln -s /vagrant/build_scripts/dspace_install/solr.xml /vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost/solr.xml
