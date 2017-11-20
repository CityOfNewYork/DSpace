#!/usr/bin/env bash

#!/bin/bash
DATE=$(date +"%Y-%m-%d")
LOGFILE="/data/dspace_backup_log-$DATE.log"

source /opt/rh/rh-postgresql95/enable
/usr/bin/python /vagrant/build_scripts/db_setup/backup.py >> $LOGFILE 2>&1