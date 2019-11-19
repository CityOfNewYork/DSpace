import csv
import json
import os
import urllib.parse

'''
This script will take in a CSV of required reports and format them to be used the following ways in DSpace:
    - XML to be used as options for the Required Report Name dropdown
    - JSON to be used to filter required reports based on agency
    - URLs for searches of each required report

Steps:
    - Export the environment variable REQUIRED_REPORTS_PATH with the full path to your CSV file
    - Export the environment variable DSPACE_URL with the homepage URL of your DSpace instance with no ending slash
    - Run this script using python required_reports_format.py
    - It will create 3 files. required_reports_pairs.txt, required_reports_json.txt, and required_reports_searches.txt
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
required_reports_searches = open('required_reports_searches.txt', 'w')
required_reports = {}
dspace_url = os.getenv('DSPACE_URL')

with open(os.getenv('REQUIRED_REPORTS_PATH'), encoding='latin-1') as csv_file:
    csv_reader = csv.reader(csv_file, delimiter=',')
    next(csv_reader)
    # Add agencies with empty arrays
    for row in csv_reader:
        required_reports[str(row[1])] = []

    # Reset csv reader
    csv_file.seek(0)
    next(csv_reader)

    required_reports_pairs.write('<pair><displayed-value> </displayed-value><stored-value> </stored-value></pair>\n')
    for row in csv_reader:
        # For XML format
        required_reports_pairs.write('<pair><displayed-value>{0}</displayed-value><stored-value>{0}</stored-value></pair>\n'.format(str(row[2])))

        # For JSON format
        report = {"report_id": str(row[0]), "report_name": str(row[2])}
        required_reports[row[1]].append(report)

        # For searches format
        agency_encoded = urllib.parse.quote(str(row[1]))
        report_name_encoded = urllib.parse.quote(str(row[2]))
        url = '{0}/simple-search?location=&query=&filter_field_1=author&filter_type_1=equals&filter_value_1={1}&filtername=requiredReportName&filtertype=equals&filterquery={2}&rpp=10&sort_by=dc.date.issued_dt&order=desc'.format(dspace_url, agency_encoded, report_name_encoded)
        required_reports_searches.write(url + '\n')
    required_reports_pairs.close()
    required_reports_searches.close()

required_reports_json.write(json.dumps(required_reports))
required_reports_json.close()