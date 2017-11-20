#!/usr/bin/env bash

# Download and extract Maven
# tar file to be provided if server does not have internet connection
wget https://archive.apache.org/dist/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.zip -P /tmp
unzip /tmp/apache-maven-3.5.0-bin.zip -d /home/vagrant/

# Add bin directory to PATH on startup
cp /vagrant/build_scripts/maven_setup/maven.sh /etc/profile.d/
source /etc/profile.d/maven.sh

# Configure proxy for HTTP requests in Maven
mv /home/vagrant/apache-maven-3.5.0/conf/settings.xml /home/vagrant/apache-maven-3.5.0/conf/settings.xml.orig
ln -s /vagrant/build_scripts/maven_setup/settings.xml /home/vagrant/apache-maven-3.5.0/conf/settings.xml