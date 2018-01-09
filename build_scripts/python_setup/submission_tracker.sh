#!/usr/bin/env bash

#!/bin/bash
DATE=$(date +"%Y-%m-%d")
LOGFILE="/data/dspace_status/dspace_status_log-$DATE.log"

source /opt/rh/rh-postgresql95/enable
source /opt/rh/rh-python35/enable
export MAIL_SERVER=<MAIL_SERVER>
export MAIL_PORT=<MAIL_PORT>
export RECIPIENT_DL=<RECIPIENT_DL>
export EMAIL_PATH=/data/dspace_status/
/home/vagrant/.virtualenvs/dspace/bin/python /vagrant/build_scripts/python_setup/status_email.py >> $LOGFILE 2>&1
