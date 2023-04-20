/***************************************************************************************************
 *
 * Copyright (c) 2019 - 2023 Universitat Politecnica de Valencia - www.upv.es
 * Copyright (c) 2019 - 2023 Open Universiteit - www.ou.nl
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
 *******************************************************************************************************/


package org.testar.protocols;

import static org.testar.monkey.alayer.Tags.Blocked;
import static org.testar.monkey.alayer.Tags.Enabled;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.ArrayUtils;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testar.monkey.*;
import org.testar.monkey.alayer.*;
import org.testar.monkey.alayer.actions.*;
import org.testar.monkey.alayer.devices.KBKeys;
import org.testar.monkey.alayer.exceptions.StateBuildException;
import org.testar.monkey.alayer.exceptions.SystemStartException;
import org.testar.monkey.alayer.webdriver.WdDriver;
import org.testar.monkey.alayer.webdriver.WdElement;
import org.testar.monkey.alayer.webdriver.WdWidget;
import org.testar.monkey.alayer.webdriver.enums.WdRoles;
import org.testar.monkey.alayer.webdriver.enums.WdTags;
import org.testar.monkey.alayer.windows.WinProcess;
import org.testar.monkey.alayer.windows.Windows;
import org.testar.plugin.NativeLinker;
import org.testar.OutputStructure;
import org.testar.serialisation.LogSerialiser;
import org.testar.reporting.Reporting;

public class WebdriverProtocol extends GenericUtilsProtocol {
    //Attributes for adding slide actions
    protected static double SCROLL_ARROW_SIZE = 36; // sliding arrows
    protected static double SCROLL_THICK = 16; //scroll thickness
    protected Reporting htmlReport;
    protected State latestState;
    
    protected static Set<String> existingCssClasses = new HashSet<>();

	// WedDriver settings from file:
	protected List<String> clickableClasses, typeableClasses, deniedExtensions, domainsAllowed;

	// URL + form name, username input id + value, password input id + value
	// Set login to null to disable this feature
	//TODO web driver settings for login feature
	protected Pair<String, String> login = Pair.from("https://login.awo.ou.nl/SSO/login", "OUinloggen");
	protected Pair<String, String> username = Pair.from("username", "");
	protected Pair<String, String> password = Pair.from("password", "");

	// List of attributes to identify and close policy popups
	protected Multimap<String, String> policyAttributes = ArrayListMultimap.create();

	// Verdict obtained from messages coming from the web browser console
	protected Verdict webConsoleVerdict = Verdict.OK;

	/**
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 * @param   settings  the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings){
		// Indicate to TESTAR we want to use webdriver package implementation
		NativeLinker.addWdDriverOS();

		// reads the settings from file:
		super.initialize(settings);

		// Classes that are deemed clickable by the web framework
		clickableClasses = settings.get(ConfigTags.ClickableClasses);

		// Classes that are deemed typeable by the web framework
		typeableClasses = settings.get(ConfigTags.TypeableClasses);

		// Disallow links and pages with these extensions
		// Set to null to ignore this feature
		deniedExtensions = settings.get(ConfigTags.DeniedExtensions).contains("null") ? null : settings.get(ConfigTags.DeniedExtensions);

		// Define a whitelist of allowed domains for links and pages
		// An empty list will be filled with the domain from the sut connector
		// Set to null to ignore this feature
		domainsAllowed = settings.get(ConfigTags.DomainsAllowed).contains("null") ? null : settings.get(ConfigTags.DomainsAllowed);

		// If true, follow links opened in new tabs
		// If false, stay with the original (ignore links opened in new tabs)
		WdDriver.followLinks = settings.get(ConfigTags.FollowLinks);

		//Force the browser to run in full screen mode
		WdDriver.fullScreen = settings.get(ConfigTags.BrowserFullScreen);

		//Force webdriver to switch to a new tab if opened
		//This feature can block the correct display of select dropdown elements 
		WdDriver.forceActivateTab = settings.get(ConfigTags.SwitchNewTabs);
	}
	
	/**
	 * This methods is called before each test sequence, allowing for example using external profiling software on the SUT
	 */
	@Override
	protected void preSequencePreparations() {
		//initializing the HTML sequence report:
		htmlReport = getReporter();
		// reset web browser console verdict
		webConsoleVerdict = Verdict.OK;
	}
    
    /**
     * This method is called when TESTAR starts the System Under Test (SUT). The method should
     * take care of
     * 1) starting the SUT (you can use TESTAR's settings obtainable from <code>settings()</code> to find
     * out what executable to run)
     * 2) bringing the system into a specific start state which is identical on each start (e.g. one has to delete or restore
     * the SUT's configuratio files etc.)
     * 3) waiting until the system is fully loaded and ready to be tested (with large systems, you might have to wait several
     * seconds until they have finished loading)
     *
     * @return a started SUT, ready to be tested.
     */
    @Override
    protected SUT startSystem() throws SystemStartException {
    	SUT sut = super.startSystem();

    	// Add the domain from the SUTConnectorValue to domainsAllowed List
    	ensureDomainsAllowed();

    	// Check if TESTAR runs in Windows 10 to set webdriver browser handle identifier
    	setWindowHandleForWebdriverBrowser(sut);

    	double displayScale = getDisplayScale(sut);

    	// See remarks in WdMouse
    	mouse = sut.get(Tags.StandardMouse);
    	mouse.setCursorDisplayScale(displayScale);

    	return sut;
    }

    /**
     * A workaround to obtain the browsers window handle, ideally this information is acquired when starting the 
     * webdriver in the constructor of WdDriver. 
     * A possible solution could be creating a snapshot of the running browser processes before and after. 
     */
    private void setWindowHandleForWebdriverBrowser(SUT sut) {
    	try {
    		if(System.getProperty("os.name").contains("Windows 10") && sut.get(Tags.HWND, null) == null) {
    			// Note don't place a breakpoint here since the outcome of the function call will result in the IDE pid and
    			// window handle. The running browser needs to be in the foreground when we reach this part.
    			long hwnd = Windows.GetForegroundWindow();
    			long pid = Windows.GetWindowProcessId(Windows.GetForegroundWindow());
    			// Safe to set breakpoints again.
    			if (WinProcess.procName(pid).contains("chrome")) {
    				sut.set(Tags.HWND, hwnd);
    				sut.set(Tags.PID, pid);
    				System.out.printf("INFO System PID %d and window handle %d have been set\n", pid, hwnd);
    			}
    		}
    	} catch (ExceptionInInitializerError | NoClassDefFoundError e) {
    		System.out.println("INFO: We can not obtain the Windows 10 windows handle of WebDriver browser instance");
    	}
    }

	/**
	 * Returns the display scale based on the settings, if the user has set the override webdriver display scale
	 * we return the override value otherwise the display scale obtained from the system.
	 * @param sut The system under test
	 * @return The display scale.
	 */
	private double getDisplayScale(SUT sut) {
		// Call specific OS API to obtain the display scale value of the system
		// Ex: windows - org.testar.monkey.alayer.windows.Windows10 calls MonitorFromWindow native function
		// If something fails these specific getDisplayScale OS implementations must return a default value
		double displayScale = Environment.getInstance().getDisplayScale(sut.get(Tags.HWND, (long)0));

		// If the user has specified a scale override the display scale obtained from the system.
		String overrideDisplayScaleAsString = settings().get(ConfigTags.OverrideWebDriverDisplayScale, "");
		if (!overrideDisplayScaleAsString.isEmpty()) {
			try {
				double webDriverDisplayScaleOverride = Double.parseDouble(overrideDisplayScaleAsString);
				if (webDriverDisplayScaleOverride != 0) {
					displayScale = webDriverDisplayScaleOverride;
				}
			} catch (NumberFormatException nfe) {
				System.out.printf("WARNING Unable to convert display scale override to double: %s, will use %f\n", overrideDisplayScaleAsString, displayScale);
			}
		}
		return displayScale;
	}

	/**
     * This method is invoked each time the TESTAR starts the SUT to generate a new sequence.
     * This can be used for example for bypassing a login screen by filling the username and password
     * or bringing the system into a specific start state which is identical on each start (e.g. one has to delete or restore
     * the SUT's configuration files etc.)
     */
    @Override
    protected void beginSequence(SUT system, State state) {
    	super.beginSequence(system, state);
    }
    
    /**
     * This method is called when the TESTAR requests the state of the SUT.
     * Here you can add additional information to the SUT's state or write your
     * own state fetching routine. The state should have attached an oracle
     * (TagName: <code>Tags.OracleVerdict</code>) which describes whether the
     * state is erroneous and if so why.
     * @return  the current state of the SUT with attached oracle.
     */
    @Override
    protected State getState(SUT system) throws StateBuildException {
    	
    	try {
    		WdDriver.waitDocumentReady();
    	} catch(org.openqa.selenium.WebDriverException wde) {
    		LogSerialiser.log("WEBDRIVER ERROR: Selenium Chromedriver seems not to respond!\n", LogSerialiser.LogLevel.Critical);
    		System.out.println("******************************************************************");
    		System.out.println("** WEBDRIVER ERROR: Selenium Chromedriver seems not to respond! **");
    		System.out.println("******************************************************************");
    		System.out.println(wde.getMessage());
    		system.set(Tags.IsRunning, false);
    	}

    	updateCssClassesFromTestSettingsFile();

    	State state = super.getState(system);

    	if(settings.get(ConfigTags.ForceForeground)
    			&& System.getProperty("os.name").contains("Windows 10")
    			&& system.get(Tags.IsRunning, false) && !system.get(Tags.NotResponding, false)
    			&& system.get(Tags.PID, (long)-1) != (long)-1 
    			&& WinProcess.procName(system.get(Tags.PID)).contains("chrome") 
    			&& !WinProcess.isForeground(system.get(Tags.PID))){
    		
    		WinProcess.politelyToForeground(system.get(Tags.HWND));
    		LogSerialiser.log("Trying to set Chrome Browser to Foreground... " 
    		+ WinProcess.procName(system.get(Tags.PID)) + "\n");
    		
    	}

    	latestState = state;
    	
    	//Spy mode didn't use the html report
    	if(settings.get(ConfigTags.Mode) == Modes.Spy) {

    		for(Widget w : state) {
    			WdElement element = ((WdWidget) w).element;
    			for(String s : element.cssClasses) {
    				existingCssClasses.add(s);
    			}
    		}
    		
        	return state;
    	}
    	
        //adding state to the HTML sequence report:
        htmlReport.addState(latestState);
        return latestState;
    }

    /**
     * The getVerdict methods implements the online state oracles that
     * examine the SUT's current state and returns an oracle verdict.
     *
     * @return oracle verdict, which determines whether the state is erroneous and why.
     */
    @Override
    protected Verdict getVerdict(State state) {
    	Verdict stateVerdict = super.getVerdict(state);

    	// If Web Console Error Oracle is enabled and we have some pattern to match
    	if(settings.get(ConfigTags.WebConsoleErrorOracle, false) && !settings.get(ConfigTags.WebConsoleErrorPattern, "").isEmpty()) {
    		// Load the web console error pattern
    		Pattern errorPattern = Pattern.compile(settings.get(ConfigTags.WebConsoleErrorPattern), Pattern.UNICODE_CHARACTER_CLASS);
    		// Check Severe messages in the WebDriver logs
    		RemoteWebDriver driver = WdDriver.getRemoteWebDriver();
    		LogEntries logEntries = driver.manage().logs().get(LogType.BROWSER);
    		for(LogEntry logEntry : logEntries) {
    			if(logEntry.getLevel().equals(Level.SEVERE)) {
    				// Check if the severe error message matches with the web console error pattern
    				String consoleErrorMsg = logEntry.getMessage();
    				Matcher matcherError = errorPattern.matcher(consoleErrorMsg);
    				if(matcherError.matches()) {
    					webConsoleVerdict = new Verdict(Verdict.SEVERITY_SUSPICIOUS_TAG, "Web Browser Console Error: " + consoleErrorMsg);
    				}
    			}
    		}
    		// Join GUI verdict with WebDriver console verdict
    		stateVerdict = stateVerdict.join(webConsoleVerdict);
    	}

    	// If Web Console Warning Oracle is enabled and we have some pattern to match
    	if(settings.get(ConfigTags.WebConsoleWarningOracle, false) && !settings.get(ConfigTags.WebConsoleWarningPattern, "").isEmpty()) {
    		// Load the web console warning pattern
    		Pattern warningPattern = Pattern.compile(settings.get(ConfigTags.WebConsoleWarningPattern), Pattern.UNICODE_CHARACTER_CLASS);
    		// Check Warning messages in the WebDriver logs
    		RemoteWebDriver driver = WdDriver.getRemoteWebDriver();
    		LogEntries logEntries = driver.manage().logs().get(LogType.BROWSER);
    		for(LogEntry logEntry : logEntries) {
    			if(logEntry.getLevel().equals(Level.WARNING)) {
    				// Check if the warning message matches with the web console error pattern
    				String consoleWarningMsg = logEntry.getMessage();
    				Matcher matcherWarning = warningPattern.matcher(consoleWarningMsg);
    				if(matcherWarning.matches()) {
    					webConsoleVerdict = new Verdict(Verdict.SEVERITY_SUSPICIOUS_TAG, "Web Browser Console Warning: " + consoleWarningMsg);
    				}
    			}
    		}
    		// Join GUI verdict with WebDriver console verdict
    		stateVerdict = stateVerdict.join(webConsoleVerdict);
    	}

    	return stateVerdict;
    }

    /**
     * Overwriting to add HTML report writing into it
     *
     * @param state
     * @param actions
     * @return
     */
    @Override
    protected Set<Action> preSelectAction(SUT system, State state, Set<Action> actions){
    	// Derive actions didn't find any action, inform the user and force WdHistoryBackAction
    	if(actions == null || actions.isEmpty()) {
    		System.out.println(String.format("** WEBDRIVER WARNING: In Action number %s the State seems to have no interactive widgets", actionCount()));
    		System.out.println(String.format("** URL: %s", WdDriver.getCurrentUrl()));
    		System.out.println("** Please try to navigate with SPY mode and configure clickableClasses inside Java protocol");
    		// Create and build the id of the HistoryBackAction
    		Action histBackAction = new WdHistoryBackAction();
    		buildEnvironmentActionIdentifiers(state, histBackAction);
    		actions = new HashSet<>(Collections.singletonList(histBackAction));
    	}
    	// super preSelectAction will not derive ESC action
    	actions = super.preSelectAction(system, state, actions);
    	// adding available actions into the HTML report:
    	htmlReport.addActions(actions);
    	return actions;
    }

    /**
     * Select one of the possible actions (e.g. at random)
     *
     * @param state   the SUT's current state
     * @param actions the set of available actions as computed by <code>buildActionsSet()</code>
     * @return the selected action (non-null!)
     */
    @Override
    protected Action selectAction(State state, Set<Action> actions) {
    	return super.selectAction(state, actions);
    }

    /**
     * Execute the selected action.
     * @param system the SUT
     * @param state the SUT's current state
     * @param action the action to execute
     * @return whether or not the execution succeeded
     */
    @Override
    protected boolean executeAction(SUT system, State state, Action action){
        // adding the action that is going to be executed into HTML report:
        htmlReport.addSelectedAction(state, action);
        return super.executeAction(system, state, action);
    }

    /**
     * Replay the saved action
     */
    @Override
    protected boolean replayAction(SUT system, State state, Action action, double actionWaitTime, double actionDuration){
        // adding the action that is going to be executed into HTML report:
        htmlReport.addSelectedAction(state, action);
        return super.replayAction(system, state, action, actionWaitTime, actionDuration);
    }

    /**
     * This methods is called after each test sequence, allowing for example using external profiling software on the SUT
     */
    @Override
    protected void postSequenceProcessing() {
        String status = "";
        String statusInfo = "";

        if(mode() == Modes.Replay) {
            htmlReport.addTestVerdict(getReplayVerdict().join(processVerdict));
            status = (getReplayVerdict().join(processVerdict)).verdictSeverityTitle();
            statusInfo = (getReplayVerdict().join(processVerdict)).info();
        }
        else {
            htmlReport.addTestVerdict(getVerdict(latestState).join(processVerdict));
            status = (getVerdict(latestState).join(processVerdict)).verdictSeverityTitle();
            statusInfo = (getVerdict(latestState).join(processVerdict)).info();
        }

        String sequencesPath = getGeneratedSequenceName();
        try {
            sequencesPath = new File(getGeneratedSequenceName()).getCanonicalPath();
        } catch (IOException e) {
			e.printStackTrace();
		}

		statusInfo = statusInfo.replace("\n"+Verdict.OK.info(), "");

        //Timestamp(generated by logger) SUTname Mode SequenceFileObject Status "StatusInfo"
        INDEXLOG.info(OutputStructure.executedSUTname
                + " " + settings.get(ConfigTags.Mode, mode())
                + " " + sequencesPath
                + " " + status + " \"" + statusInfo + "\"" );

        htmlReport.close();
    }
    
    @Override
	protected void finishSequence(){
		//With webdriver version we don't use the call SystemProcessHandling.killTestLaunchedProcesses
	}

    @Override
    protected void stopSystem(SUT system) {
        if(settings.get(ConfigTags.Mode) == Modes.Spy) {

            try {
                if(Settings.getSettingsPath() != null) {
                    File folder = new File(Settings.getSettingsPath());
                    File file = new File(folder, "existingCssClasses.txt");
                    if(!file.exists())
                        file.createNewFile();

                    Stream<String> stream = Files.lines(Paths.get(file.getCanonicalPath()));
                    stream.forEach(line -> existingCssClasses.add(line));
                    stream.close();

                    PrintWriter write = new PrintWriter(new FileWriter(file.getCanonicalPath()));
                    for(String s : existingCssClasses)
                        write.println(s);
                    write.close();
                }

            } catch (IOException e) {
				e.printStackTrace();
			}
		}

        super.stopSystem(system);
    }
    
    @Override
	protected void closeTestSession() {
		super.closeTestSession();
		NativeLinker.cleanWdDriverOS();
	}

	/*
	 * Check the state if we need to force an action
	 */
	protected Set<Action> detectForcedActions(State state, StdActionCompiler ac) {
		Set<Action> actions = detectForcedDeniedUrl();
		if (actions != null && actions.size() > 0) {
			return actions;
		}

		actions = detectForcedLogin(state);
		if (actions != null && actions.size() > 0) {
			return actions;
		}

		actions = detectForcedPopupClick(state, ac);
		if (actions != null && actions.size() > 0) {
			return actions;
		}

		return new HashSet<Action>();
	}

	/*
	 * Detect and perform login if defined
	 */
	protected Set<Action> detectForcedLogin(State state) {
		if (login == null || username == null || password == null) {
			return new HashSet<Action>();
		}

		// Check if the current page is a login page
		String currentUrl = WdDriver.getCurrentUrl();
		if (currentUrl.startsWith(login.left())) {
			CompoundAction.Builder builder = new CompoundAction.Builder();
			// Set username and password
			for (Widget widget : state) {
				WdWidget wdWidget = (WdWidget) widget;
				// Only enabled, visible widgets
				if (!widget.get(Enabled, true) || widget.get(Blocked, false)) {
					continue;
				}

				if (username.left().equals(wdWidget.getAttribute("id"))) {
					builder.add(new WdAttributeAction(
							username.left(), "value", username.right()), 1);
				}
				else if (password.left().equals(wdWidget.getAttribute("id"))) {
					builder.add(new WdAttributeAction(
							password.left(), "value", password.right()), 1);
				}
			}
			// Submit form, but only if user and pass are filled
			builder.add(new WdSubmitAction(login.right()), 2);
			CompoundAction actions = builder.build();
			if (actions.getActions().size() >= 3) {
				return new HashSet<>(Collections.singletonList(actions));
			}
		}

		return new HashSet<Action>();
	}

	/*
	 * Force closing of Policies Popup
	 */
	protected Set<Action> detectForcedPopupClick(State state, StdActionCompiler ac) {
		Set<Action> popupClickActions = new HashSet<Action>();

		if (policyAttributes == null || policyAttributes.size() == 0) {
			return popupClickActions;
		}

		for (Widget widget : state) {
			// If not visible widget, ignore
			if (!isAtBrowserCanvas(widget) || widget.get(WdTags.WebAttributeMap, null) == null) {
				continue;
			}

			// If some of the attributed matches, add the possible click action
			boolean popupMatches = false;
			for (String key : policyAttributes.keySet()) {
				String attribute = widget.get(WdTags.WebAttributeMap).get(key);
				for (String entryValue: policyAttributes.get(key)) {
					popupMatches |= entryValue.equals(attribute);
				}
			}
			if (popupMatches) {
				popupClickActions.add(ac.leftClickAt(widget));
			}
		}

		return popupClickActions;
	}

	/*
	 * Force back action due to disallowed domain or extension
	 */
	protected Set<Action> detectForcedDeniedUrl() {
		String currentUrl = WdDriver.getCurrentUrl();

		// Don't get caught in PDFs etc. and non-whitelisted domains
		if (isUrlDenied(currentUrl) || isExtensionDenied(currentUrl)) {
			// If opened in new tab, close it
			if (WdDriver.getWindowHandles().size() > 1) {
				return new HashSet<>(Collections.singletonList(new WdCloseTabAction()));
			}
			// Single tab, go back to previous page
			else {
				return new HashSet<>(Collections.singletonList(new WdHistoryBackAction()));
			}
		}

		return new HashSet<Action>();
	}

	/*
	 * Check if the current address has a denied extension (PDF etc.)
	 */
	protected boolean isExtensionDenied(String currentUrl) {
		// If the current page doesn't have an extension, always allow
		if (!currentUrl.contains(".")) {
			return false;
		}

		if (deniedExtensions == null || deniedExtensions.size() == 0) {
			return false;
		}

		// Deny if the extension is in the list
		String ext = currentUrl.substring(currentUrl.lastIndexOf(".") + 1);
		ext = ext.replace("/", "").toLowerCase();
		return deniedExtensions.contains(ext);
	}

	/*
	 * Check if the URL is denied
	 */
	protected boolean isUrlDenied(String currentUrl) {
		if (currentUrl.startsWith("mailto:")) {
			return true;
		}

		// Always allow local file
		if (currentUrl.startsWith("file:///")) {
			return false;
		}

		// User wants to allow all
		if (domainsAllowed == null) {
			return false;
		}

		// Only allow pre-approved domains
		String domain = getDomain(currentUrl);
		return !domainsAllowed.contains(domain);
	}

	/*
	 * Check if the widget has a denied URL as hyperlink
	 */
	protected boolean isLinkDenied(Widget widget) {
		String linkUrl = widget.get(Tags.ValuePattern, "");

		// Not a link or local file, allow
		if (linkUrl == null || linkUrl.startsWith("file:///")) {
			return false;
		}

		// Deny the link based on extension
		if (isExtensionDenied(linkUrl)) {
			return true;
		}

		// Mail link, deny
		if (linkUrl.startsWith("mailto:")) {
			return true;
		}

		// Not a web link (or link to the same domain), allow
		if (!(linkUrl.startsWith("https://") || linkUrl.startsWith("http://"))) {
			return false;
		}

		// User wants to allow all
		if (domainsAllowed == null) {
			return false;
		}

		// Only allow pre-approved domains if
		String domain = getDomain(linkUrl);
		return !domainsAllowed.contains(domain);
	}

	/*
	 * Get the domain from a full URL
	 */
	protected String getDomain(String url) {
		if (url == null) {
			return null;
		}

		// When serving from file, 'domain' is filesystem
		if (url.startsWith("file://")) {
			return "file://";
		}

		url = url.replace("https://", "").replace("http://", "").replace("file://", "");
		return (url.split("/")[0]).split("\\?")[0];
	}

	/*
	 * If domainsAllowed from SUTConnectorValue is not set, include it in the domainsAllowed
	 */
	protected void ensureDomainsAllowed() {
		try{
			// Adding default domain from SUTConnectorValue if is not included in the domainsAllowed list
			//TODO try-catch for nullpointer if sut connector missing
			String[] parts = settings().get(ConfigTags.SUTConnectorValue).split(" ");
			String sutConnectorUrl = parts[parts.length - 1].replace("\"", "");

			if(domainsAllowed != null && !domainsAllowed.contains(getDomain(sutConnectorUrl))) {
				System.out.println(String.format("WEBDRIVER INFO: Automatically adding %s SUT Connector domain to domainsAllowed List", getDomain(sutConnectorUrl)));
				String[] newDomainsAllowed = domainsAllowed.stream().toArray(String[]::new);
				domainsAllowed = Arrays.asList(ArrayUtils.insert(newDomainsAllowed.length, newDomainsAllowed, getDomain(sutConnectorUrl)));
				System.out.println(String.format("domainsAllowed: %s", String.join(",", domainsAllowed)));
			}

			// Also add the default starting domain of the SUT if is not included in the domainsAllowed list
			String initialUrl = WdDriver.getCurrentUrl();

			if(domainsAllowed != null && !domainsAllowed.contains(getDomain(initialUrl))) {
				System.out.println(String.format("WEBDRIVER INFO: Automatically adding initial %s Web domain to domainsAllowed List", getDomain(initialUrl)));
				String[] newDomainsAllowed = domainsAllowed.stream().toArray(String[]::new);
				domainsAllowed = Arrays.asList(ArrayUtils.insert(newDomainsAllowed.length, newDomainsAllowed, getDomain(initialUrl)));
				System.out.println(String.format("domainsAllowed: %s", String.join(",", domainsAllowed)));
			}
		} catch(Exception e) { //TODO check what kind of exception can happen
			System.out.println("WEBDRIVER ERROR: Trying to add the startup domain to domainsAllowed List");
			System.out.println("Please review domainsAllowed List inside Webdriver Java Protocol");
		}
	}

	/*
	 * We need to check if click position is within the canvas
	 */
	protected boolean isAtBrowserCanvas(Widget widget) {
		Shape shape = widget.get(Tags.Shape, null);
		if (shape == null) {
			return false;
		}

		// Widget must be completely visible on viewport for screenshots
		return widget.get(WdTags.WebIsFullOnScreen, false);
	}

	protected boolean isForm(Widget widget) {
	    Role role = widget.get(Tags.Role, Roles.Widget);
	    return role.equals(WdRoles.WdFORM);
	}

	/*
	Check whether the widget's web element contains at least one of the accepted css classes
	 */
	protected boolean containCSSClasses(Widget widget, List<String> cssClasses){
		WdElement element = ((WdWidget) widget).element;
		Set<String> cssSet = new HashSet<>(cssClasses);
		cssSet.retainAll(element.cssClasses);
		return cssSet.size() > 0;
	}

	@Override
	protected boolean isTypeable(Widget widget) {
		if (containCSSClasses(widget, typeableClasses)) return true;

		Role role = widget.get(Tags.Role, Roles.Widget);
		if (Role.isOneOf(role, NativeLinker.getNativeTypeableRoles())) {
			// Input type are special...
			if (role.equals(WdRoles.WdINPUT)) {
				String type = ((WdWidget) widget).element.type;
				return WdRoles.typeableInputTypes().contains(type.toLowerCase());
			}
			return true;
		}
		return false;
	}

	@Override
	protected boolean isClickable(Widget widget) {
		WdElement element = ((WdWidget) widget).element;
		if (element.isClickable || containCSSClasses(widget, clickableClasses)) {
			return true;
		}

		Role role = widget.get(Tags.Role, Roles.Widget);
		if (Role.isOneOf(role, NativeLinker.getNativeClickableRoles())) {
			// Input type are special...
			if (role.equals(WdRoles.WdINPUT)) {
				String type = element.type;
				return WdRoles.clickableInputTypes().contains(type);
			}
			return true;
		}
		return false;
	}

	/**
	 * Read the ClickableClasses property from test.settings file 
	 * to update the clickableClasses while TESTAR is running in Spy mode. 
	 */
	private void updateCssClassesFromTestSettingsFile() {
		// Feature only for Spy mode
		if(settings.get(ConfigTags.Mode) != Modes.Spy) {
			return;
		}

		try {
			try(BufferedReader br = new BufferedReader(new FileReader(Main.getTestSettingsFile()))) {
				for(String line; (line = br.readLine()) != null;) {
					if(line.contains(ConfigTags.ClickableClasses.name())){
						List<String> fileClickableClasses = Arrays.asList(line.split("=")[1].trim().split(";"));
						// Check if user added new CSS Classes from test settings file to update the clickableClasses
						for(String webClass : fileClickableClasses) {
							if(!webClass.isEmpty() && !clickableClasses.contains(webClass)) {
								System.out.println("Adding new clickable class from settings file: " + webClass);
								clickableClasses.add(webClass);
								settings.set(ConfigTags.ClickableClasses, clickableClasses);
							}
						}
						// Check if user removed CSS Classes from test settings file to update the clickableClasses
						for(String clickClass : clickableClasses) {
							if(!clickClass.isEmpty() && !fileClickableClasses.contains(clickClass)) {
								System.out.println("Removing the clickable class: " + clickClass);
								clickableClasses.remove(clickClass);
								settings.set(ConfigTags.ClickableClasses, clickableClasses);
							}
						}
					}
				}
			}
		} catch (Exception e) {}
	}

	/**
	 * Add additional TESTAR keyboard shortcuts in SPY mode to enable the CSS clickable customization of widgets.
	 * @param key
	 */
	@Override
	public void keyDown(KBKeys key) {
		super.keyDown(key);
		if (mode() == Modes.Spy){
			if (key == KBKeys.VK_RIGHT) {
				try {
					// Obtain the widget aimed with the mouse cursor
					Widget w = Util.widgetFromPoint(latestState, mouse.cursor().x(), mouse.cursor().y());
					// Add the widget web CSS class property as clickable
					WdElement element = ((WdWidget) w).element;
					for(String s : element.cssClasses)
						if(s!=null && !s.isEmpty())
							clickableClasses.add(s);
					// And save the new CSS class property in the test.setting file
					Util.saveToFile(settings.toFileString(), Main.getTestSettingsFile());
				} catch(Exception e) {
					System.out.println("ERROR adding the widget from point: " + "x(" + mouse.cursor().x() + "), y("+ mouse.cursor().y() +")");
				}
			}

			if (key == KBKeys.VK_LEFT) {
				try {
					// Obtain the widget aimed with the mouse cursor
					Widget w = Util.widgetFromPoint(latestState, mouse.cursor().x(), mouse.cursor().y());
					// Remove the widget web CSS class property from all clickables
					WdElement element = ((WdWidget) w).element;
					for(String s : element.cssClasses)
						if(s!=null && !s.isEmpty())
							clickableClasses.remove(s);
					// And save the new CSS class property in the test.setting file
					Util.saveToFile(settings.toFileString(), Main.getTestSettingsFile());
				} catch(Exception e) {
					System.out.println("ERROR removing the widget from point: " + "x(" + mouse.cursor().x() + "), y("+ mouse.cursor().y() +")");
				}
			}
		}
	}
}
