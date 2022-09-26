/**
 * Copyright (c) 2018 - 2021 Open Universiteit - www.ou.nl
 * Copyright (c) 2019 - 2021 Universitat Politecnica de Valencia - www.upv.es
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.testar.protocols;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputFilter.Config;
import java.io.OutputStream;
import java.io.Reader;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.json.JSONTokener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.testar.monkey.alayer.Action;
import org.testar.monkey.alayer.State;
import org.testar.monkey.alayer.SUT;
import org.testar.monkey.alayer.exceptions.SystemStartException;
import org.testar.monkey.ConfigTags;
import org.testar.monkey.Settings;
import org.testar.instrumentation.InstrumentationInterface;
import org.testar.instrumentation.InstrumentationWebUtils;
import org.testar.instrumentation.WebInstrumentationInterface;

/*
 * This protocol contains generic functionality for scriptless testing of web application SUTs
 * that are packaged using Docker Compose. The protocol optionally takes care of the following
 * tasks:
 *  - Setting the coverage context of the SUT instrumentation, so that collected coverage data can be
 *    related to the experiment.
 *  - Setting the log context of the SUT prior to each action, so that instrumentation data related to
 *    individual actions can be related to each action.
 *  - Retrieving action related data from the instrumented SUT and passing it to the application-level
 *    protocol for further processing.
 *
 * This protocol is based on DockerizedSUTWebdriverProtocol, which handles managing the SUT using Docker
 * Compose. See that class for additional information.
 *
 * Settings to configure:
 * - ApplicationBaseURL: base URL that the SUT will be running on, e.g. http://localhost:5000
 * - CoverageContext: label to attach to all collected coverage data in the instrumented SUT (string)
 * - SetCoverageContext: whether to set the coverage context (boolean)
 * - LogContextPrefix: prefix of the label to attach to all action-specific data collected by the SUT instrumentation.
 *   the context is generated based on this prefix, the sequence number, and action number.
 * - SetLogContext: whether to set the log context before each action (boolean)
 * - ProcessDataAfterAction: whether to collected action data after each action and pass it to the
 *   processActionData method in any class extending this class (boolean)
 * - ActionGetDataEndpoint: API endpoint in the web interface of the SUT for retrieving the action-related
 *   data. It is assumed the URL has the format http://sut/endpointname/logcontextname, and that this endpoint
 *   returns JSON data.
*/

public class CodeAnalysisWebdriverProtocol extends DockerizedSUTWebdriverProtocol {

	protected String coverageContext,logContextPrefix,actionGetDataEndpoint;
    protected boolean setCoverageContext=false,setLogContext=false,processDataAfterAction=false;
    protected boolean codeAnalysisDebugMessages=false;
    protected float fullStringRate=0.0f, typeMatchRate = 1.0f;
    protected int maxInputStrings = 1;
    protected int sequenceNumber = -1, actionNumber = -1;
    protected StringBuffer coverageData= null;
    protected InstrumentationInterface instrumentationInterface = null;

    // This boolean value can be used by the protocol to temporarily disable calls to the
    // instrumentation, e.g. during initial login actions.
    protected boolean instrumentationEnabled = true;

    /**
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 *
	 * @param settings the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings) {
        this.coverageContext = settings.get(ConfigTags.CoverageContext);
        this.logContextPrefix = settings.get(ConfigTags.LogContextPrefix);
        this.setLogContext = settings.get(ConfigTags.SetLogContext);
        this.setCoverageContext = settings.get(ConfigTags.SetCoverageContext);
        this.processDataAfterAction = settings.get(ConfigTags.ProcessDataAfterAction);
        this.actionGetDataEndpoint = settings.get(ConfigTags.ActionGetDataEndpoint);
        this.maxInputStrings = settings.get(ConfigTags.MaxInputStrings);
        this.fullStringRate = settings.get(ConfigTags.FullStringRate);
        this.typeMatchRate = settings.get(ConfigTags.TypeMatchRate);
        this.codeAnalysisDebugMessages = settings.get(ConfigTags.CodeAnalysisDebugMessages);

        if ( this.fullStringRate < 0.0 || this.fullStringRate > 1.0 ) {
            System.out.println("Invalid full string rate for in CodeAnalysisWebdriverProtocol.");
            System.exit(1);
        }

        if ( this.typeMatchRate < 0.0 || this.typeMatchRate > 1.0 ) {
            System.out.println("Invalid type match rate for in CodeAnalysisWebdriverProtocol.");
            System.exit(1);
        }

        if ( this.maxInputStrings < 2 ) {
            System.out.println("Invalid maxInputStrings for in CodeAnalysisWebdriverProtocol.");
            System.exit(1);
        }

        super.initialize(settings);

        this.instrumentationInterface = new WebInstrumentationInterface(applicationBaseURL, codeAnalysisDebugMessages);
	}

    @Override
	protected SUT startSystem() throws SystemStartException {
        SUT sut = super.startSystem();
        if ( this.instrumentationEnabled && this.setCoverageContext ) {
            instrumentationInterface.setCoverageContext(this.coverageContext);
        }
        else {
            waitForSUT();
        }
        return sut;
	}

    private void waitForSUT() {
        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis before wait for SUT.");
        }
		if (! InstrumentationWebUtils.waitForURL(applicationBaseURL, 60, 5,  1, 200) )  {
			logger.info("Error: did not succeed in waiting .");
		}
        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis after wait for SUT.");
        }
	}


    @Override
	protected boolean executeAction(SUT system, State state, Action action) {
        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis: start executeAction.");
        }
		if (this.instrumentationEnabled && this.setLogContext) {
            instrumentationInterface.setLogContext(getCurrentLogContext());
        }
        boolean result =  super.executeAction(system, state, action);
        if (this.instrumentationEnabled && this.processDataAfterAction ) {
            retrieveSUTDataAfterAction();
        }
        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis before end executeAction.");
        }
        return result;
    }

    protected String getCurrentLogContext() {
        return this.logContextPrefix + "-" + Integer.toString(sequenceNumber) + "-" +
            Integer.toString(actionNumber);
    }

    @Override
	protected void beginSequence(SUT system, State state) {
        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis start beginSequence.");
        }
        this.sequenceNumber++;
        this.actionNumber = 0;

        logger.info("Sequence " + String.valueOf(sequenceNumber) + " starting.");

        super.beginSequence(system, state);

        if ( settings.get(ConfigTags.CarryOverCoverage) ) {
            if ( coverageData == null ) {
                logger.info("No coverage data available. Not loading it into SUT.");
            }
            else {
                instrumentationInterface.importCoverage(coverageData);
            }
        }

        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis end beginSequence.");
        }
    }

    @Override
    protected void finishSequence(){

        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis begin finishSequence.");
        }

        logger.info("Sequence " + String.valueOf(sequenceNumber ) + " finishing.");

        if ( settings.get(ConfigTags.CarryOverCoverage) || settings.get(ConfigTags.ExportCoverage ) ) {
            coverageData = instrumentationInterface.exportCoverage();
            if (coverageData == null ) {
                logger.error("Failed to export coverage data at end of sequence.");
            }
        }

        if ( settings.get(ConfigTags.ExportCoverage ) ) {
            if ( coverageData == null ) {
                logger.error("Cannot export coverage data. No data available.");
            }
            else {
                try {
                    storeCoverageData();
                }
                catch (IOException e) {
                    logger.error("Unable to store coverage data. Exception: " + e.toString());
                }
            }
        }

        super.finishSequence();

        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis end finishSequence.");
        }
    }

    protected void storeCoverageData() throws IOException {
        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis before storing coverage data.");
        }
        String filename = settings.get(ConfigTags.CoverageExportDirectory ) + "/coverage-" +
            String.valueOf(sequenceNumber) + "-" + String.valueOf (System.currentTimeMillis()) + ".dump";
        BufferedWriter w = new BufferedWriter(new FileWriter(new File(filename)));
        w.write(coverageData.toString());
        w.flush();
        w.close();
        logger.info("Stored coverage data to " + filename + ".");
        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis after storing coverage data.");
        }
    }

    @Override
	protected Action selectAction(State state, Set<Action> actions) {
        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis start selectAction.");
        }
        this.actionNumber++;
	Action selectedAction = super.selectAction(state, actions);
        if (codeAnalysisDebugMessages) {
            logger.info("Code analysis end selectAction.");
        }
        return selectedAction;
    }

    protected void retrieveSUTDataAfterAction() {
        StringBuffer sutData = instrumentationInterface.extractStrings(getCurrentLogContext());
        JSONTokener tokener = new JSONTokener(sutData.toString());
        processSUTDataAfterAction(tokener);
    }

    protected void processSUTDataAfterAction(JSONTokener tokener) {
        // Left to be implemented in subclass, in case ProcessDataAfterAction
        // is set to true in the settings.
    }

}
