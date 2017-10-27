#!/usr/bin/env bash

# Download and extract Ant
# tar file to be provided if server does not have internet connection
wget http://apache.spinellicreations.com/ant/binaries/apache-ant-1.10.1-bin.tar.gz -P /tmp
tar xzvf /tmp/apache-ant-1.10.1-bin.tar.gz -C /home/vagrant/

# Add bin directory to PATH on startup
cp /vagrant/build_scripts/ant_setup/ant.sh /etc/profile.d/
source /etc/profile.d/ant.sh