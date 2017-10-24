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
ln -s /vagrant/build_scripts/settings.xml /vagrant/apache-maven-3.5.0/conf/settings.xml

## Ant setup
wget http://apache.spinellicreations.com//ant/binaries/apache-ant-1.10.1-bin.tar.gz -P /tmp
tar xzvf /tmp/apache-ant-1.10.1-bin.tar.gz
cp /vagrant/build_scripts/ant.sh /etc/profile.d/
source /etc/profile.d/ant.sh

## Tomcat setup
wget http://apache.mirrors.hoobly.com/tomcat/tomcat-8/v8.5.23/bin/apache-tomcat-8.5.23.tar.gz -P /tmp
tar xzvf /tmp/apache-tomcat-8.5.23.tar.gz

# use UTF-8 as its default file encoding for international character support
# JAVA_OPTS="-Xmx512M -Xms64M -Dfile.encoding=UTF-8"

mv apache-tomcat-8.5.23/conf/server.xml apache-tomcat-8.5.23/conf/server.xml.orig
ln -s /vagrant/build_scripts/server.xml apache-tomcat-8.5.23/conf/server.xml

ln -s /vagrant/build_scripts/local.cfg /vagrant/dspace/config/

# Build installation package
cd /vagrant
mvn package

cd /vagrant/dspace/target/dspace-installer
ant fresh_install

# Deploy web applications
ln -s /vagrant/build_scripts/jspui.xml /vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost/jspui.xml
ln -s /vagrant/build_scripts/xmlui.xml /vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost/xmlui.xml
ln -s /vagrant/build_scripts/solr.xml /vagrant/apache-tomcat-8.5.23/conf/Catalina/localhost/solr.xml