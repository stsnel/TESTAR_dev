package org.testar.instrumentation;

// Uses script to extract string data from SUT

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.testar.protocols.DockerizedSUTWebdriverProtocol;

public class TweakedWebInstrumentationInterface extends WebInstrumentationInterface {

    DockerizedSUTWebdriverProtocol protocol = null;
    String dockerComposeDirectory = null;

    public TweakedWebInstrumentationInterface(String applicationBaseURL, boolean debugMessages, String dockerComposeDirectory, DockerizedSUTWebdriverProtocol protocol) {
        super(applicationBaseURL, debugMessages);
        this.protocol = protocol;
        this.dockerComposeDirectory = dockerComposeDirectory;
    }

    public StringBuffer extractStrings(String context) {
        if (debugMessages) {
            logger.info("Instrumentation interface before ExtractStrings.");
        }

        StringBuffer   stringBuffer = new StringBuffer();

        String[] extractStringsCommand = {"./extract-strings.sh", context};
        protocol.runDockerCommand(extractStringsCommand);
        String[] downloadStringsCommands = { "./download-strings.sh"};
        protocol.runDockerCommand(downloadStringsCommands);

        // Code for loading file contents into buffer was adapted from
        // code example at https://stackoverflow.com/questions/326390/how-do-i-create-a-java-string-from-the-contents-of-a-file

        String         line = null;
        String         ls = System.getProperty("line.separator");

        try {
            BufferedReader reader = new BufferedReader(new FileReader (new File(dockerComposeDirectory+"/strings.json")));
            while((line = reader.readLine()) != null) {
                stringBuffer.append(line);
                stringBuffer.append(ls);
            }
            reader.close();

         } catch (IOException ioe) {
            System.out.println("Cannot read strings file " + ioe.toString());
         }

        if (debugMessages) {
            logger.info("Instrumentation interface after ExtractStrings.");
        }

        System.out.println("Extracted strings are " + stringBuffer.toString());

        return stringBuffer;

    }

}