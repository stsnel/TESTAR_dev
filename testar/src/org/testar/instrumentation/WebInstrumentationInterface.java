package org.testar.instrumentation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WebInstrumentationInterface implements InstrumentationInterface {

    String applicationBaseURL = null;
    boolean debugMessages = false;
    Logger logger = null;

    public WebInstrumentationInterface(String applicationBaseURL, boolean debugMessages) {
        this.applicationBaseURL = applicationBaseURL;
        this.debugMessages = debugMessages;
        this.logger = LogManager.getLogger();
    }

    public void setCoverageContext(String newContext) {
		String setContextURL = this.applicationBaseURL + "/testar-covcontext/" + newContext;

        if (debugMessages) {
            logger.info("Instrumentation interface before setting coverage context.");
        }

		if (! InstrumentationWebUtils.waitForURL(setContextURL, 60, 5, 1, 200) )  {
			logger.info("Error: did not succeed in setting coverage context.");
		}

        if (debugMessages) {
            logger.info("Instrumentation interface after setting coverage context.");
        }
	}

    public void setLogContext (String newContext) {
		if (debugMessages) {
                logger.info("Instrumentation interface before setting log context.");
        }
		String setContextURL = applicationBaseURL + "/testar-logcontext/" + newContext;

		if ( ! InstrumentationWebUtils.waitForURL(setContextURL, 60, 5, 1, 200) )  {
			logger.error("Error: did not succeed in setting log context for context URL "
				+ setContextURL + ".");
		}
        if (debugMessages) {
            logger.info("Instrumentation interface after setting log context.");
        }
	}

    public StringBuffer extractStrings(String context) {
        if (debugMessages) {
            logger.info("Instrumentation interface before ExtractStrings.");
        }

        StringBuffer result = InstrumentationWebUtils.getRequest( applicationBaseURL + "/testar-extractstrings/" + context, 300, 60, 5, 200);

        if (debugMessages) {
            logger.info("Instrumentation interface before ExtractStrings.");
        }

        return result;

    }

    public StringBuffer clearLogDataExportCoverage() {
        if (debugMessages) {
            logger.info("Instrumentation interface before exporting coverage data (and clearing log data).");
        }
        StringBuffer result = InstrumentationWebUtils.getRequest ( applicationBaseURL + "/testar-clearlog-exportdata", 300, 60, 5, 200);
        if (debugMessages) {
            logger.info("Instrumentation interface after exporting coverage data (and clearing log data).");
        }
        return result;
    }

    public StringBuffer exportCoverage() {
        if (debugMessages) {
            logger.info("Instrumentation interface before exporting coverage data.");
        }
        StringBuffer result = InstrumentationWebUtils.getRequest ( applicationBaseURL + "/testar-exportdata", 300, 60, 5, 200);
        if (debugMessages) {
            logger.info("Instrumentation interface after exporting coverage data.");
        }
        return result;
    }

    public void importCoverage(StringBuffer coverageData) {
        if (debugMessages) {
            logger.info("Instrumentation interface before importing coverage data.");
        }
        if ( InstrumentationWebUtils.postRequest( applicationBaseURL + "/testar-importdata", 600, 60, 5, 200, coverageData) ) {
            logger.info("Error: transmitted coverage data to SUT.");
        }
        else {
            logger.error("Error: did not succeed in loading coverage data.");
        }
        if (debugMessages) {
            logger.info("Instrumentation interface after importing coverage data.");
        }
    }



}