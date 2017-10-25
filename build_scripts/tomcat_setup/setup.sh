#!/usr/bin/env bash

# Download and extract Tomcat
wget http://apache.mirrors.hoobly.com/tomcat/tomcat-8/v8.5.23/bin/apache-tomcat-8.5.23.tar.gz -P /tmp
tar xzvf /tmp/apache-tomcat-8.5.23.tar.gz -C /vagrant/

# use UTF-8 as its default file encoding for international character support
# JAVA_OPTS="-Xmx512M -Xms64M -Dfile.encoding=UTF-8"

# Create symlink of Tomcat configuration
mv /vagrant/apache-tomcat-8.5.23/conf/server.xml /vagrant/apache-tomcat-8.5.23/conf/server.xml.orig
ln -s /vagrant/build_scripts/tomcat_setup/server.xml /vagrant/apache-tomcat-8.5.23/conf/server.xml