import sys

import psycopg2
from datetime import datetime, timedelta


import os
import smtplib
import getpass
import csv
#import mimetypes

from email.utils import formataddr
from email.utils import formatdate
from email.utils import COMMASPACE

from email.header import Header
from email import encoders

from email.mime.multipart import MIMEMultipart
from email.mime.base import MIMEBase
from email.mime.text import MIMEText
from email.mime.image import MIMEImage

# Constants
DBNAME='dspace'
USER='dspace'
PASSWORD='dspace'
HOST='127.0.0.1'
PORT=5432


SENDER_NAME = 'DSpace Submission Tracker'
SENDER_ADDR = 'do-not-reply@records.nyc.gov'
MAIL_SERVER = os.getenv('MAIL_SERVER')
MAIL_PORT = os.getenv('MAIL_PORT')
RECIPIENT_ADDR = [os.getenv('RECIPIENT_DL')]

# SQL Queries
GET_ALL_SUBMISSIONS = """
SELECT
  submission_tracker.submission_id,
  submission_tracker.date_created,
  item.in_archive
FROM submission_tracker
  JOIN item ON submission_tracker.submission_id = item.uuid
WHERE item.in_archive IS FALSE;
"""

GET_SUBMISSION_METADATA = """
SELECT
  submission_tracker.date_created,
  metadatavalue.text_value
FROM submission_tracker
  JOIN item ON submission_tracker.submission_id = item.uuid
  JOIN dspaceobject ON item.uuid = dspaceobject.uuid
  JOIN metadatavalue ON dspaceobject.uuid = metadatavalue.dspace_object_id
WHERE submission_id = '{submission_id}' AND metadata_field_id IN (70, 9);
"""  # 9 = Author; 70 = Title

EMAIL_TEXT = """
Municipal Library Staff,<br />
<br />
The following submissions have not been approved in the DSpace System. Please log in and review these submissions.<br /><br />
"""

SUBMISSION_EMAIL_STRING = """
-------------------------------------------------------------------------------------------------<br />
Title: {title}<br />
Agency: {agency}<br />
Due Data: {due_date}<br />
<br />
"""


def connect(dbname: str, user: str, password: str, host: str = '127.0.0.1', port: int = 5432):
    """
    Connect to a PostgreSQL Database as the specified user.

    :param dbname: the database name
    :type dbname: string
    :param user: user name used to authenticate
    :type user: string
    :param password: password used to authenticate
    :type password: string
    :param host: database host address (IPv4), defaults to 127.0.0.1
    :type host: string (format W.X.Y.Z)
    :param port: connection port number, defaults to 5432
    :type port: integer

    :returns: psycopg2.connection.cursor object
    """
    try:
        connection_string = "dbname='{dbname}' user='{user}' password='{password}' host='{host}' port='{port}'".format(
            dbname=dbname,
            user=user,
            password=password,
            host=host,
            port=port
        )
        conn = psycopg2.connect(connection_string)
        return conn.cursor()
    except psycopg2.Error as e:
        print("""
              Failed to connect to database for the following reason
              {postgres_error_code}
              {postgres_error_desc}
              """.format(
                postgres_error_code=e.pgcode,
                postgres_error_desc=e.pgerror
              )
        )
        sys.exit(1)

def send_email(sender_name: str, sender_addr: str, smtp: str, port: str,
               recipient_addr: list, subject: str, html: str, text: str,
               fn: str='last.eml', save: bool=False):

    sender_name = Header(sender_name, 'utf-8').encode()

    msg_root = MIMEMultipart('mixed')
    msg_root['Date'] = formatdate(localtime=1)
    msg_root['From'] = formataddr((sender_name, sender_addr))
    msg_root['To'] = COMMASPACE.join(recipient_addr)
    msg_root['Cc'] = None
    msg_root['Bcc'] = None
    msg_root['Subject'] = Header(subject, 'utf-8')
    msg_root.preamble = 'This is a multi-part message in MIME format.'

    msg_related = MIMEMultipart('related')
    msg_root.attach(msg_related)
    msg_root

    msg_alternative = MIMEMultipart('alternative')
    msg_related.attach(msg_alternative)

    msg_text = MIMEText(text.encode('utf-8'), 'plain', 'utf-8')
    msg_alternative.attach(msg_text)

    msg_html = MIMEText(html.encode('utf-8'), 'html', 'utf-8')
    msg_alternative.attach(msg_html)

    mail_server = smtplib.SMTP(smtp, port)
    mail_server.send_message(msg_root)
    mail_server.quit()

    if save:
        with open(fn, 'w') as f:
            f.write(msg_root.as_string())

def main():
    """
    Generates a status email for the DSpace - Government Publications Submissions Portal.
    """

    cur = connect(
      dbname=DBNAME,
      user=USER,
      password=PASSWORD,
      host=HOST,
      port=PORT
    )

    email_text = EMAIL_TEXT

    cur.execute(GET_ALL_SUBMISSIONS)
    current_submissions = cur.fetchall()

    for submission_id, date_submitted, accepted in current_submissions:
        query = GET_SUBMISSION_METADATA.format(submission_id=submission_id)
        cur.execute(query)
        data = cur.fetchall()

        due_date = date_submitted + timedelta(days=10)
        due_date = due_date.strftime('%m/%d/%Y')


        submission_info = SUBMISSION_EMAIL_STRING.format(title=data[0][1], agency=data[1][1], due_date=due_date)

        email_text += submission_info

    send_email(
        sender_name=SENDER_NAME,
        sender_addr=SENDER_ADDR,
        smtp=MAIL_SERVER,
        port=MAIL_PORT,
        recipient_addr=RECIPIENT_ADDR,
        subject='DSpace - Unapproved Submissions - {date}'.format(date=datetime.now().strftime('%m/%d/%Y %I:%M %p')),
        html="""
                <html>
                <head>
                <meta http-equiv="content-type" content="text/html;charset=utf-8" />
                </head>
                <body>
                <font face="verdana" size=2>{}<br/></font>
                </body>
                </html>
                """.format(email_text),
        text=email_text,
        fn=os.path.join(os.getenv('EMAIL_PATH'), ("submission_tracker-{date}.eml".format(date=datetime.now().strftime('%m-%d-%Y-%H:%M:%S')))),
        save=True
    )

if __name__ == '__main__':
    main()
