#!/usr/bin/env bash

# Create symlink of DSpace configuration file
ln -s /vagrant/build_scripts/dspace_install/local.cfg /vagrant/dspace/config/local.cfg

# Build installation package
cd /vagrant
mvn package

# Install DSpace
cd /vagrant/dspace/target/dspace-installer
ant fresh_install

# DSpace installation directory and Tomcat directory must have same owner
chown -R vagrant:vagrant /home/vagrant/dspace

# Deploy web applications
mkdir -p /home/vagrant/apache-tomcat-8.5.24/conf/Catalina/localhost
ln -s /vagrant/build_scripts/dspace_install/jspui.xml /home/vagrant/apache-tomcat-8.5.24/conf/Catalina/localhost/jspui.xml
ln -s /vagrant/build_scripts/dspace_install/xmlui.xml /home/vagrant/apache-tomcat-8.5.24/conf/Catalina/localhost/xmlui.xml
ln -s /vagrant/build_scripts/dspace_install/solr.xml /home/vagrant/apache-tomcat-8.5.24/conf/Catalina/localhost/solr.xml
