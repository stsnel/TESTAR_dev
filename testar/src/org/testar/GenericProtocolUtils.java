package org.testar;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.MalformedInputException;

import java.util.ArrayList;

public class GenericProtocolUtils {

    public int request(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        return con.getResponseCode();
    }

    /**
     * Waits for requests to a particular URL to return a particular status code, with multiple retries.
     *
     * @param url_string URL to send requests to
     * @param maxWaitTime Approximate maximum time to wait, in seconds
     * @param retryTime Approximate time between retries, in seconds
     * @param expectedStatusCode return
     * @return boolean value that shows whether the requests
     * @throws MalformedURLException
     * @throws IOException
     * @throws ProtocolException
     */
    public static boolean waitForURL(String url_string, int maxWaitTime, int retryTime, int expectedStatusCode) {
        long beginTime = System.currentTimeMillis() / 1000L;
        long currentTime = beginTime;
        while ( ( currentTime = System.currentTimeMillis() / 1000L ) < ( beginTime + maxWaitTime) ) {
        try {
                URL url = new URL(url_string);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(retryTime*1000);
                con.setReadTimeout(retryTime*1000);
                int status = con.getResponseCode();
                System.out.println("Status is " + status);
            if ( status == expectedStatusCode ) {
                System.out.println("Waiting for " + url_string + " finished.");
                return true;
            }
            else
            {  System.out.println("Info: unexpected status code " + Integer.toString(status) +
                    " while waiting for " + url_string);
            }
        }
        catch ( SocketTimeoutException ste) {
            System.out.println("info: waiting for " + url_string + " ...");
            continue;
        }
        catch ( Exception e) {
            System.out.println("info: generic exception while waiting for " + url_string +
                    ": " + e.toString() );
            System.out.println(Long.toString(currentTime));
        }
        System.out.println("info: sleeping between retries for " + url_string + " ...");
        try {
         Thread.sleep(retryTime*1000);
        }
        catch (InterruptedException ie) {
         System.out.println("Sleep between retries for " + url_string + " was interrupted.");
        }
        }
        System.out.println("info: max wait time expired while waiting for " + url_string + " ...");
        return false;
    }


    /** Executes a command in a particular directory.
     *
     * @param directory Working directory
     * @param command Command to execute
     * @return
     */
    private static int execCommand(String directory, ArrayList command) {

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(directory));
            Process pr = pb.start();

            try {
                  pr.waitFor();
            }
            catch (InterruptedException ie) {
                System.out.println("Command " + command + " interrupted.");
            }

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

            // Read the output from the command
            System.out.println("Standard output of the command:\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            // Read any errors from the attempted command
            System.out.println("Standard error output of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }

            return pr.exitValue();

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

    }

}
