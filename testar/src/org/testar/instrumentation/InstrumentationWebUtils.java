package org.testar.instrumentation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility methods for interacting with SUT through web interface (primarily for controlling instrumentation)
 */

public class InstrumentationWebUtils {

 /**
     * Waits for requests to a particular URL to return a particular status code, with multiple retries.
     *
     * @param url_string URL to send requests to
     * @param maxWaitTime Approximate maximum time to wait, in seconds
     * @param timeout Timeout for requests, in seconds
     * @param retryTime Approximate time between retries, in seconds
     * @param expectedStatusCode return
     * @return boolean value that shows whether the request returned the expected status code
     * @throws MalformedURLException
     * @throws IOException
     * @throws ProtocolException
     */
    public static boolean waitForURL(String urlString, int maxWaitTime, int timeout, int retryTime, int expectedStatusCode) {
        Logger logger = LogManager.getLogger();
        long beginTime = System.currentTimeMillis() / 1000L;
        long currentTime = beginTime;
        while ( ( currentTime = System.currentTimeMillis() / 1000L ) < ( beginTime + maxWaitTime) ) {
        try {
                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(timeout*1000);
                con.setReadTimeout(timeout*1000);
                int status = con.getResponseCode();
                logger.info("Status is " + status);
            if ( status == expectedStatusCode ) {
                logger.info("Waiting for " + urlString + " finished.");
                return true;
            }
            else
            {  logger.info("Info: unexpected status code " + Integer.toString(status) +
                    " while waiting for " + urlString);
            }
        }
        catch ( SocketTimeoutException ste) {
            logger.info("info: waiting for " + urlString + " ...");
            continue;
        }
        catch ( Exception e) {
            logger.info("info: generic exception while waiting for " + urlString +
                    ": " + e.toString() );
            logger.info(Long.toString(currentTime));
        }
        logger.info("info: sleeping between retries for " + urlString + " ...");
        try {
         Thread.sleep((long)retryTime*1000);
        }
        catch (InterruptedException ie) {
            logger.info("Sleep between retries for " + urlString + " was interrupted.");
        }
        }
        logger.info("info: max wait time expired while waiting for " + urlString + " ...");
        return false;
    }

    protected static StringBuffer getRequest(String urlString, int maxWaitTime, int timeout,  int retryTime, int expectedStatusCode) {
        long beginTime = System.currentTimeMillis() / 1000L;
        long currentTime = beginTime;
        Logger logger = LogManager.getLogger();
        while ( ( currentTime = System.currentTimeMillis() / 1000L ) < ( beginTime + maxWaitTime) ) {
        try {
                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(timeout*1000);
                con.setReadTimeout(timeout*1000);
                int status = con.getResponseCode();
                logger.info("Status is " + status);
                if ( status == expectedStatusCode ) {
                    BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
                    String line;
                    StringBuffer content = new StringBuffer();
                    while ((line = in.readLine()) != null) {
                        //logger.info("Read line from url " + url + ":" + line + "\n");
                        content.append(line+"\n");
                    }
                    in.close();
                    return content;
                }
                else
                {  logger.info("Info: unexpected status code " + Integer.toString(status) +
                        " while waiting for " + urlString);
                }
        }
        catch ( SocketTimeoutException ste) {
            logger.info("info: waiting for " + urlString + " ...");
            continue;
        }
        catch ( Exception e) {
            logger.info("info: generic exception while waiting for " + urlString +
                    ": " + e.toString() );
            logger.info(Long.toString(currentTime));
        }
        logger.info("info: sleeping between retries for " + urlString + " ...");
        try {
         Thread.sleep((long)retryTime * 1000);
        }
        catch (InterruptedException ie) {
            logger.info("Sleep between retries for " + urlString + " was interrupted.");
        }
        }
        logger.info("info: max wait time expired while waiting for " + urlString + " ...");
        return null;

    }

    public static boolean postRequest(String urlString, int maxWaitTime, int timeout, int retryTime, int expectedStatusCode, StringBuffer content) {

        long beginTime = System.currentTimeMillis() / 1000L;
        long currentTime = beginTime;
        Logger logger = LogManager.getLogger();
        while ( ( currentTime = System.currentTimeMillis() / 1000L ) < ( beginTime + maxWaitTime) ) {
        try {
                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setConnectTimeout(timeout*1000);
                con.setReadTimeout(timeout*1000);
                con.setDoOutput(true);
                con.setRequestProperty( "Content-Type", "text/plain" );
                con.setRequestProperty( "Content-Length", String.valueOf(content.length()));
                OutputStream os = con.getOutputStream();
                os.write(content.toString().getBytes());
                int status = con.getResponseCode();
                logger.info("Status is " + status);
                if ( status == expectedStatusCode ) {
                    return true;
                }
                else
                {  logger.info("Info: unexpected status code " + Integer.toString(status) +
                        " while waiting for " + urlString);
                }
        }
        catch ( SocketTimeoutException ste) {
            logger.info("info: waiting for " + urlString + " ...");
            continue;
        }
        catch ( Exception e) {
            logger.info("info: generic exception while waiting for " + urlString +
                    ": " + e.toString() );
            logger.info(Long.toString(currentTime));
        }
        logger.info("info: sleeping between retries for " + urlString + " ...");
        try {
         Thread.sleep((long)retryTime*1000);
        }
        catch (InterruptedException ie) {
            logger.info("Sleep between retries for " + urlString + " was interrupted.");
        }
        }
        logger.info("info: max wait time expired while waiting for " + urlString + " ...");
        return false;
    }

}