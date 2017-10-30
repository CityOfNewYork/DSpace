#!/usr/bin/env bash

# Install Java
yum -y install java-1.8.0-openjdk-devel

# Set up JAVA_HOME environment variable on startup
cp /vagrant/build_scripts/java_setup/java.sh /etc/profile.d/
source /etc/profile.d/java.sh