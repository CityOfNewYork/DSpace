#!/usr/bin/env bash

# Download and extract Ant
wget http://apache.spinellicreations.com//ant/binaries/apache-ant-1.10.1-bin.tar.gz -P /tmp
tar xzvf /tmp/apache-ant-1.10.1-bin.tar.gz -C /vagrant/

# Add bin directory to PATH on startup
cp /vagrant/build_scripts/ant_setup/ant.sh /etc/profile.d/
source /etc/profile.d/ant.sh