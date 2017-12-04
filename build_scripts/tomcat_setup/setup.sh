#!/usr/bin/env bash

# Download and extract Tomcat
# tar file to be provided if server does not have internet connection
wget http://apache.mirrors.hoobly.com/tomcat/tomcat-8/v8.5.24/bin/apache-tomcat-8.5.24.tar.gz -P /tmp
tar xzvf /tmp/apache-tomcat-8.5.24.tar.gz -C /home/vagrant/
chown -R vagrant:vagrant /home/vagrant/apache-tomcat-8.5.24

# Create symlink of Tomcat configuration
mv /home/vagrant/apache-tomcat-8.5.24/conf/server.xml /home/vagrant/apache-tomcat-8.5.24/conf/server.xml.orig
ln -s /vagrant/build_scripts/tomcat_setup/server.xml /home/vagrant/apache-tomcat-8.5.24/conf/server.xml