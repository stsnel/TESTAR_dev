package nl.ou.testar.temporal.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

import static com.google.common.net.HttpHeaders.USER_AGENT;

public class Common {
    public static String toWSLPath(String windowsFilePath) {
        Path winpath = Paths.get(windowsFilePath);
        StringBuilder wslpath = new StringBuilder();
        wslpath.append("/mnt/");
        wslpath.append(winpath.getRoot().toString().split(":")[0].toLowerCase());  // convert C:\\ --> c
        for (int i = 0; i < winpath.getNameCount(); i++) {
            wslpath.append("/").append(winpath.getName(i));
        }
        return wslpath.toString();
    }

    public static void HTTPGet(String url) {
        // HTTP GET request from https://www.mkyong.com/java/how-to-send-http-request-getpost-in-java/
        URL obj = null;
        try {
            obj = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection con ;
        try {
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");// optional default is GET
            con.setRequestProperty("User-Agent", USER_AGENT);//add request header
            con.getResponseCode(); //consume and discard responsecode
            //int responseCode = con.getResponseCode(); //consume and discard responsecode

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();               //consume and discard result
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    public static void RunOSChildProcess(String command) {
        RunOSChildProcess(command, "");
    }
    public static void RunOSChildProcess(String command, String resultsFileName) {

        Process theProcess = null;
        BufferedReader inStream;
        BufferedReader errStream;
        String response;
        String errorresponse = null;
        File captureFile = null;
        BufferedWriter captureWriter = null;


        // call the external program
        try {
            theProcess = Runtime.getRuntime().exec(command);
        }
        catch (IOException e) {
            System.err.println("Error on exec() method");
            e.printStackTrace();
        }

        // read from the called program's standard output stream
        try {
            if (!resultsFileName.equals("")){
            captureFile = new File(resultsFileName);
            if (captureFile.exists() || captureFile.createNewFile()) {
                captureWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(captureFile.getAbsolutePath()), StandardCharsets.UTF_8));
            }
        }
            inStream = new BufferedReader(new InputStreamReader(theProcess.getInputStream()));
            errStream = new BufferedReader(new InputStreamReader(theProcess.getErrorStream()));
            while ((response = inStream.readLine()) != null || (errorresponse = errStream.readLine()) != null) {
                if (response != null) {
                    if (captureFile ==null){ System.out.println("response: " + response); }
                    else{ captureWriter.append(response+"\n"); }
                }
                if (errorresponse != null) {
                    if (captureFile ==null){ System.out.println("error response: " + errorresponse); }
                    else{ captureWriter.append(errorresponse+"\n"); }
                }
            }
            if (captureFile !=null) {
                captureWriter.close();
            }
        } catch (IOException e) {
            System.err.println("Error on input stream from child process");
            e.printStackTrace();
        }
    }

    public static String CurrentDateToFolder() {
        Date aDate = Calendar.getInstance().getTime();
        return DateToFolder(aDate);
    }

    public static String DateToFolder(Date aDate) {
        // inspired from https://alvinalexander.com/java/simpledateformat-convert-date-to-string-formatted-parse
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return formatter.format(aDate);
    }
    public static String prettyCurrentTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss,SSS");
        return LocalTime.now().format(dtf);
    }
}