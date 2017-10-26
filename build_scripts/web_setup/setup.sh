#!/usr/bin/env bash

# Install nginx
yum -y install rh-nginx18

# Autostart nginx
chkconfig rh-nginx18-nginx on

# Setup /etc/profile.d/nginx18.sh
bash -c "printf '#\!/bin/bash\nsource /opt/rh/rh-nginx18/enable\n' > /etc/profile.d/nginx18.sh"
source /etc/profile.d/nginx18.sh

# Configure nginx
mv /etc/opt/rh/rh-nginx18/nginx/nginx.conf /etc/opt/rh/rh-nginx18/nginx/nginx.conf.orig

# SymLink nginx.conf
ln -s /vagrant/build_scripts/web_setup/nginx_conf/nginx.conf /etc/opt/rh/rh-nginx18/nginx/nginx.conf

# Restart nginx
sudo service rh-nginx18-nginx restart