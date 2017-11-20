#!/usr/bin/env bash

#!/bin/bash
DATE=$(date +"%Y-%m-%d")
LOGFILE="/data/dspace_backup_log-$DATE.log"

source /opt/rh/rh-postgresql95/enable
/usr/bin/python /export/local/default/backup.py >> $LOGFILE 2>&1

# If using vagrant run:
# /usr/bin/python /vagrant/backup.py >> $LOGFILE 2>&1

