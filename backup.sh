#!/usr/bin/env bash

#!/bin/bash
DATE=$(date +"%Y-%m-%d")
LOGFILE="/data/dspace_backup_log-$DATE.log"

source /opt/rh/rh-postgresql95/enable
# If using vagrant:
/usr/bin/python /export/local/default/backup.py >> $LOGFILE 2>&1

# If using SPG server:
# /usr/bin/python /export/local/default/backup.py >> $LOGFILE 2>&1

