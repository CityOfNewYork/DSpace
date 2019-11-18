import csv
import json
import os

'''
This script will take in a CSV of required reports and format them into XML and JSON form to use in DSpace.

Steps:
    - Export the environment variable REQUIRED_REPORTS_PATH with the full path the your CSV file.
    - Run this script using python required_reports_format.py
    - It will create 2 file. required_reports_pairs.txt and required_reports_json.txt
    - In required_reports_pairs.txt, copy the contents to sublime. Find all & characters and replace with &amp;
    - Copy the contents into dspace/config/input-forms.xml inside the required-report-names element
    - Format with IntelliJ (CMD + OPTION + L)
    - Delete the extra new line at the end of the required-report-names element
    - In the first pair of the required-report-names element, make it a single space character instead of empty string
    - In required_reports_json.txt, copy the contents to sublime. Find all \" characters and replace with '
    - Copy the contents into a json formatter such as https://jsonformatter.curiousconcept.com/
    - Replace the json in /DSpace/dspace-jspui/src/main/webapp/static/js/required-reports.js with the contents of the
      json formatter
    - Format with IntelliJ (CMD + OPTION + L)
'''

required_reports_pairs = open('required_report_pairs.txt', 'w')
required_reports_json = open('required_reports_json.txt', 'w')
required_reports = {}

with open(os.getenv('REQUIRED_REPORTS_PATH'), encoding='latin-1') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    next(csv_reader)
    # add agencies with empty arrays
    for row in csv_reader:
        required_reports[str(row[1])] = []

    # reset csv reader
    csv_file.seek(0)
    next(csv_reader)

    required_reports_pairs.write('<pair><displayed-value> </displayed-value><stored-value> </stored-value></pair>\n')
    for row in csv_reader:
        required_reports_pairs.write('<pair><displayed-value>{0}</displayed-value><stored-value>{0}</stored-value></pair>\n'.format(str(row[2])))
        report = {"report_id": str(row[0]), "report_name": str(row[2])}
        required_reports[row[1]].append(report)
    required_reports_pairs.close()

required_reports_json.write(json.dumps(required_reports))
required_reports_json.close()