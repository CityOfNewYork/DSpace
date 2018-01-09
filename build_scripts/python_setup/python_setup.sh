#!/usr/bin/env bash

# 1. Install Python 3.5
yum -y install rh-python35

# 2. Setup /etc/profile.d/python.sh
bash -c "printf '#\!/bin/bash\nsource /opt/rh/rh-python35/enable\n' > /etc/profile.d/python35.sh"

# 3. Install Postgres Python Package (psycopg2) and Postgres Developer Package
yum -y install rh-postgresql95-postgresql-devel
yum -y install rh-python35-python-psycopg2

# 4. Install Required pip Packages
source /opt/rh/rh-python35/enable
pip install virtualenv
mkdir /home/vagrant/.virtualenvs
virtualenv --system-site-packages /home/vagrant/.virtualenvs/dspace
chown -R vagrant:vagrant /home/vagrant

# 5. Automatically Use Virtualenv
echo "source /home/vagrant/.virtualenvs/dspace/bin/activate" >> /home/vagrant/.bash_profile
