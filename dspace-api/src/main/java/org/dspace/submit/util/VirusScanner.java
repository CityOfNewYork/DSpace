package org.dspace.submit.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class VirusScanner {

    private static final Logger log = Logger.getLogger(VirusScanner.class);

    private static ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final String profileName = configurationService.getProperty("icap.profile.name");
    private static final String serviceName = configurationService.getProperty("icap.service.name");

    private static final String ICAP_VERSION = configurationService.getProperty("icap.version");

    public static boolean scan(byte[] bytes, String host, int port, String clientHost) throws Exception {

        Socket socket = null;
        BufferedReader input = null;
        DataOutputStream output = null;
        String result;

        try {
            // connect to server
            socket = new Socket(host, port);

            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new DataOutputStream(socket.getOutputStream());

            //hard code filename as retainer
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String requestHeader = "GET http://" + clientHost + "/" + dateFormat.format(new Date())
                    + "/retainer HTTP/1.1\r\n" + "Host: " + clientHost + "\r\n\r\n";

            String responseHeader = "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n";

            int res_header = requestHeader.length();
            int res_body = res_header + (responseHeader.length());

            String icapRequest = "RESPMOD icap://" + host + ":" + port + "/" + serviceName + profileName + " " + ICAP_VERSION + "\r\n" + "Allow: 204\r\n"
                    + "Encapsulated: req-hdr=0" + " res-hdr=" + res_header + " res-body=" + res_body + "\r\n"
                    + "Host: " + host + "\r\n"
                    + "User-Agent: JavaICAPClient\r\n"
                    + "X-Client-IP: " + clientHost + "\r\n\r\n";

            output.writeBytes(icapRequest);
            output.flush();
            output.writeBytes(requestHeader);
            output.flush();
            output.writeBytes(responseHeader);
            output.flush();

            // send file
            String headerSeperator = Long.toHexString(bytes.length);
            headerSeperator = headerSeperator + "\r\n";
            output.writeBytes(headerSeperator);
            output.flush();

            output.write(bytes, 0, bytes.length);
            output.writeBytes("\r\n0\r\n\r\n");
            output.flush();

            // receive response
            result = input.readLine();
        } catch (Exception e) {
            log.error("Failed to connect to ICAP server", e);
            throw e;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (Exception ignored) {
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
            }
        }

        // parse response
        if (result.startsWith(ICAP_VERSION)) {
            String[] results = result.split(" ", 3);
            //NOTE: skipping results[0] because ICAP_VERSION is not necessary to store.
            int code = Integer.valueOf((results[1]));
            if (VirusScanCodes.INFECTED_AND_REPAIRABLE_VIRUS_SCAN_CODE.equals(code) || VirusScanCodes.INFECTED_AND_NOT_REPAIRABLE_VIRUS_SCAN_CODE.equals(code) || VirusScanCodes.INFECTED_VIRUS_SCAN_CODE.equals(code)) {
                return true;
            } else if (VirusScanCodes.NOT_INFECTED_VIRUS_SCAN_CODE.equals(code)) {
                return false;
            } else {
                throw new Exception(results[2]);
            }
        } else {
            throw new Exception("VirusScanResponse: ICAP response error");
        }

    }
}
