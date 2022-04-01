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

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import org.testar.SutVisualization;
import org.testar.GenericProtocolUtils;
import org.testar.monkey.ConfigTags;
import org.testar.monkey.Pair;
import org.testar.monkey.alayer.*;
import org.testar.monkey.alayer.actions.AnnotatingActionCompiler;
import org.testar.monkey.alayer.actions.CompoundAction;
import org.testar.monkey.alayer.actions.KeyDown;
import org.testar.monkey.alayer.actions.StdActionCompiler;
import org.testar.monkey.alayer.actions.Type;
import org.testar.monkey.alayer.devices.KBKeys;
import org.testar.monkey.alayer.exceptions.ActionBuildException;
import org.testar.monkey.alayer.exceptions.StateBuildException;
import org.testar.monkey.alayer.exceptions.SystemStartException;
import org.testar.monkey.alayer.webdriver.WdElement;
import org.testar.monkey.alayer.webdriver.WdWidget;
import org.testar.monkey.alayer.webdriver.enums.WdRoles;
import org.testar.monkey.alayer.webdriver.enums.WdTags;
import org.testar.plugin.NativeLinker;
import org.testar.monkey.Settings;
import org.testar.protocols.WebdriverProtocol;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.testar.monkey.alayer.Tags.Blocked;
import static org.testar.monkey.alayer.Tags.Enabled;
import static org.testar.monkey.alayer.webdriver.Constants.scrollArrowSize;
import static org.testar.monkey.alayer.webdriver.Constants.scrollThick;


public class Protocol_webdriver_ckan1 extends WebdriverProtocol {

	private String sutUrl,applicationUsername,applicationPassword,applicationBaseURL, dockerComposeDirectory, logContextPrefix, coverageContext;
	private int sutPort;
	private int sequenceNumber = -1, actionNumber = -1;


	/**
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 *
	 * @param settings the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings) {
		super.initialize(settings);

		/*
		These settings are initialized in WebdriverProtocol:

		// Classes that are deemed clickable by the web framework
		// getting from the settings file:
		clickableClasses = settings.get(ConfigTags.ClickableClasses);

		// Disallow links and pages with these extensions
		// Set to null to ignore this feature
		// getting from the settings file:
		deniedExtensions = settings.get(ConfigTags.DeniedExtensions).contains("null") ? null : settings.get(ConfigTags.DeniedExtensions);

		// Define a whitelist of allowed domains for links and pages
		// An empty list will be filled with the domain from the sut connector
		// Set to null to ignore this feature
		// getting from the settings file:
		domainsAllowed = settings.get(ConfigTags.DomainsAllowed).contains("null") ? null : settings.get(ConfigTags.DomainsAllowed);

		// If true, follow links opened in new tabs
		// If false, stay with the original (ignore links opened in new tabs)
		// getting from the settings file:
		WdDriver.followLinks = settings.get(ConfigTags.FollowLinks);

		//Force the browser to run in full screen mode
		WdDriver.fullScreen = true;

		//Force webdriver to switch to a new tab if opened
		//This feature can block the correct display of select dropdown elements
		WdDriver.forceActivateTab = true;
		*/

		// URL + form name, username input id + value, password input id + value
		// Set login to null to disable this feature
		// TODO: getting from the settings file, not sure if this works:
		//login = Pair.from("https://login.awo.ou.nl/SSO/login", "OUinloggen");
		//username = Pair.from("username", "");
		//password = Pair.from("password", "");

		this.applicationUsername = settings.get(ConfigTags.ApplicationUsername);
		this.applicationPassword = settings.get(ConfigTags.ApplicationPassword);
		this.applicationBaseURL = settings.get(ConfigTags.ApplicationBaseURL);
		this.dockerComposeDirectory = settings.get(ConfigTags.DockerComposeDirectory);
		this.logContextPrefix = settings.get(ConfigTags.LogContextPrefix);
		this.coverageContext = settings.get(ConfigTags.CoverageContext);
		System.out.println("Application username is " + this.applicationUsername);
		System.out.println("Application password is " + this.applicationPassword);
		System.out.println("Application base URL is " + this.applicationBaseURL);
		System.out.println("Docker compose directory is " + this.dockerComposeDirectory);
		System.out.println("Log context prefix is " + this.logContextPrefix);
		System.out.println("Coverage context is  " + this.coverageContext);


		// List of attributes to identify and close policy popups
		// Set to null to disable this feature
		//TODO put into settings file
		policyAttributes = new HashMap<String, String>() {{
			put("class", "lfr-btn-label");
		}};

		// Pull latest versions of CKAN SUT images, and remove any previously saved data
		initializeCkanImages();
		fullResetCKAN();
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
		System.out.println("startSystem called ...");
		startCkan();

		SUT sut =  super.startSystem();

		return sut;
	}

	/** Initializes the CKAN SUT using Docker Compose */
    private void initializeCkanImages() {
		String[] pruneCommand = {"docker", "image", "prune", "-f"};
		runDockerCommand(pruneCommand);
		String[] pullCommand = {"docker-compose", "pull"};
		runDockerCommand(pullCommand);
    }

	/** Starts the CKAN SUT using Docker Compose */
    private void startCkan() {
		String[] command = {"docker-compose", "up", "--force-recreate", "--build", "-d"};
		runDockerCommand(command);
		if ( ! GenericProtocolUtils.waitForURL(applicationBaseURL, 300, 5, 200) ) {
			System.out.println("Error: did not succeed in bringing up SUT.");
		}
		else {
			setCoverageContext();
		}
	}

	private void setCoverageContext() {
		String setContextURL = applicationBaseURL + "/testar-covcontext/" + this.coverageContext;
		System.out.println("Setting coverage context ...");
		if (! GenericProtocolUtils.waitForURL(setContextURL, 60, 5, 200) )  {
			System.out.println("Error: did not succeed in setting coverage context.");
		}
	}

	private void setLogContext() {
		String context = this.logContextPrefix + "-" + Integer.toString(sequenceNumber) + "-" +
			Integer.toString(actionNumber);
		System.out.println("Setting log context to " + context + " ...");
		String setContextURL = applicationBaseURL + "/testar-logcontext/" + context;
		if (! GenericProtocolUtils.waitForURL(setContextURL, 60, 5, 200) )  {
			System.out.println("Error: did not succeed in setting log context for context "
				+ context + ".");
		}
	}

	private void extractStrings() {
		Vector<Map<String,String>> output = new Vector<>();
		String queryUrl = this.applicationBaseURL + "/testar-extractstrings/" +
			this.logContextPrefix + "-" + Integer.toString(sequenceNumber) + "-" +
			Integer.toString(actionNumber);

		try {
			URL url = new URL(queryUrl);
			JSONTokener tokener = new JSONTokener(url.openStream());
			JSONArray root = new JSONArray(tokener);

			for (int i = 0; i < root.length(); i++) {
				 JSONArray inner = root.getJSONArray(i);
				String type = inner.getString(0);
				String value = inner.getString(1);
				System.out.println("Extracted string " + type + " / " + value);
				Map<String, String> innerMap = new HashMap<>();
				innerMap.put("type", type);
				innerMap.put("value", value);
				output.add(innerMap);
			}
		}
		catch (Exception e) {
			System.out.println("Error during extracting strings: " + e.toString());
		}
		// TODO: put output in the state model. How?
	}

    /** Stops the CKAN SUT using Docker Compose */
	private void stopCkan() {
		String[] command = {"docker-compose", "down"};
		runDockerCommand(command);
	}

	/** Resets all state of the CKAN SUT (application state, string extractor / log state,
	 *  coverage state) */
	private void fullResetCKAN() {
		String[] fullStopCommand = { "docker-compose", "down", "-v"};
		runDockerCommand(fullStopCommand);
		String[] containerPruneCommand = {"docker", "container", "prune", "-f"};
		runDockerCommand(containerPruneCommand);
		String[] volumePruneCommand = {"docker", "volume", "prune", "-f"};
		runDockerCommand(volumePruneCommand);
	}

	/** Resets application state of CKAN, but not the coverage data. */
	private void resetBetweenSequencesCKAN() {
		stopCkan();
		removeCkanApplicationVolumes();
		startCkan();
	}

	/** Removes all application state Docker volumes */
	private void removeCkanApplicationVolumes() {
		String[] applicationVolumes = { "run_ckan_config", "run_ckan_home", "run_ckan_storage",
		 "runpg_data", "run_solr_data"};
		for (String volume : applicationVolumes ) {
			String[] command = {"docker", "volume", "rm", volume};
			runDockerCommand(command);
		}
	}

    /** Runs a docker command in the Docker Compose Directory */
	private void runDockerCommand(String[] command) {
		ProcessBuilder builder = new ProcessBuilder(command);
		System.out.println("Protocol executing command: " + String.join(" ", command));
		builder = builder.directory(new File(dockerComposeDirectory));
		builder.redirectErrorStream(true);
		try {
			synchronized(builder) {
				Process p = builder.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null)
					System.out.println("Command output: " + line);
				p.waitFor();
			}
		}
		catch (IOException ioe) {
			System.out.println("Exception when starting command" + String.join(" ", command)
				+ " " + ioe.toString());
		}
		catch (InterruptedException ie)  {
			System.out.println("Exception when waiting for command" + String.join(" ", command)
				+ " " + ie.toString());
		}
	}

	/**
	 * This method is invoked each time the TESTAR starts the SUT to generate a new sequence.
	 * This can be used for example for bypassing a login screen by filling the username and password
	 * or bringing the system into a specific start state which is identical on each start (e.g. one has to delete or restore
	 * the SUT's configuration files etc.)
	 */
	@Override
	protected void beginSequence(SUT system, State state) {
		this.sequenceNumber++;
		System.out.println("Begin sequence called for sequence " + Integer.toString(sequenceNumber));
		this.actionNumber = 0;
		super.beginSequence(system, state);

		/*
		waitLeftClickAndTypeIntoWidgetWithMatchingTag("name","login", this.applicationUsername, state, system, 1, 0.5);

		new CompoundAction.Builder ()
		. add (new KeyDown ( KBKeys . VK_TAB ) ,0.5) // Tab to next field, which should be password field
		. add (new Type (this.applicationPassword) ,0.1)
		. add (new KeyDown ( KBKeys . VK_TAB ) ,0.5)
		. add (new KeyDown ( KBKeys . VK_ENTER ) ,0.5). build()
		. run ( system , null , 0.1);
		*/

		/*
		System.out.println("Enter login " + this.applicationUsername);
		if (waitLeftClickAndTypeIntoWidgetWithMatchingTag("name","login", this.applicationUsername, state, system, 1, 0.5)) {
			System.out.println("Entering user name succeeded.");
		}
		else {
			System.out.println("Entering user name failed.");
		}
		if (waitLeftClickAndTypeIntoWidgetWithMatchingTag("name","password", this.applicationPassword, state, system, 1, 0.5) ) {
			System.out.println("Entering password succeeded.");
		}
		else {
			System.out.println("Entering password failed.");
		}
		if (waitAndLeftClickWidgetWithMatchingTag("class", "btn-primary", state, system, 1, 0.1) ) {
			System.out.println("Clicking login button succeeded.");
		}
		else {
			System.out.println("Clicking login button failed.");
		}
		*/
	}

	/**
	 * This method is called when TESTAR requests the state of the SUT.
	 * Here you can add additional information to the SUT's state or write your
	 * own state fetching routine. The state should have attached an oracle
	 * (TagName: <code>Tags.OracleVerdict</code>) which describes whether the
	 * state is erroneous and if so why.
	 *
	 * @return the current state of the SUT with attached oracle.
	 */
	@Override
	protected State getState(SUT system) throws StateBuildException {
		State state = super.getState(system);

		return state;
	}

	/**
	 * This is a helper method used by the default implementation of <code>buildState()</code>
	 * It examines the SUT's current state and returns an oracle verdict.
	 *
	 * @return oracle verdict, which determines whether the state is erroneous and why.
	 */
	@Override
	protected Verdict getVerdict(State state) {

		Verdict verdict = super.getVerdict(state);
		// system crashes, non-responsiveness and suspicious titles automatically detected!

		//-----------------------------------------------------------------------------
		// MORE SOPHISTICATED ORACLES CAN BE PROGRAMMED HERE (the sky is the limit ;-)
		//-----------------------------------------------------------------------------

		// ... YOU MAY WANT TO CHECK YOUR CUSTOM ORACLES HERE ...

		return verdict;
	}

	/**
	 * This method is used by TESTAR to determine the set of currently available actions.
	 * You can use the SUT's current state, analyze the widgets and their properties to create
	 * a set of sensible actions, such as: "Click every Button which is enabled" etc.
	 * The return value is supposed to be non-null. If the returned set is empty, TESTAR
	 * will stop generation of the current action and continue with the next one.
	 *
	 * @param system the SUT
	 * @param state  the SUT's current state
	 * @return a set of actions
	 */
	@Override
	protected Set<Action> deriveActions(SUT system, State state) throws ActionBuildException {
		System.out.println("deriveActions running ...");
		// Kill unwanted processes, force SUT to foreground
		Set<Action> actions = super.deriveActions(system, state);
		Set<Action> filteredActions = new HashSet<>();

		// create an action compiler, which helps us create actions
		// such as clicks, drag&drop, typing ...
		StdActionCompiler ac = new AnnotatingActionCompiler();

		// Check if forced actions are needed to stay within allowed domains
		Set<Action> forcedActions = detectForcedActions(state, ac);

		// iterate through all widgets
		for (Widget widget : state) {
			// only consider enabled and non-tabu widgets
			if (!widget.get(Enabled, true)) {
				continue;
			}
			// The blackListed widgets are those that have been filtered during the SPY mode with the
			//CAPS_LOCK + SHIFT + Click clickfilter functionality.
			if(blackListed(widget)){
				if(isTypeable(widget)){
					filteredActions.add(ac.clickTypeInto(widget, this.getRandomText(widget), true));
				} else {
					filteredActions.add(ac.leftClickAt(widget));
				}
				continue;
			}

			// slides can happen, even though the widget might be blocked
			addSlidingActions(actions, ac, scrollArrowSize, scrollThick, widget);

			// If the element is blocked, Testar can't click on or type in the widget
			if (widget.get(Blocked, false) && !widget.get(WdTags.WebIsShadow, false)) {
				continue;
			}

			// type into text boxes
			if (isAtBrowserCanvas(widget) && isTypeable(widget)) {
				if(whiteListed(widget) || isUnfiltered(widget)){
					actions.add(ac.clickTypeInto(widget, this.getRandomText(widget), true));
				}else{
					// filtered and not white listed:
					filteredActions.add(ac.clickTypeInto(widget, this.getRandomText(widget), true));
				}
			}

			// left clicks, but ignore links outside domain
			if (isAtBrowserCanvas(widget) && isClickable(widget)) {
				if(whiteListed(widget) || isUnfiltered(widget)){
					if (!isLinkDenied(widget)) {
						actions.add(ac.leftClickAt(widget));
					}else{
						// link denied:
						filteredActions.add(ac.leftClickAt(widget));
					}
				}else{
					// filtered and not white listed:
					filteredActions.add(ac.leftClickAt(widget));
				}
			}
		}

		//if(actions.isEmpty()) {
		//	return new HashSet<>(Collections.singletonList(new WdHistoryBackAction()));
		//}

		// If we have forced actions, prioritize and filter the other ones
		if (forcedActions != null && forcedActions.size() > 0) {
			filteredActions = actions;
			actions = forcedActions;
		}

		//Showing the grey dots for filtered actions if visualization is on:
		if(visualizationOn || mode() == Modes.Spy) SutVisualization.visualizeFilteredActions(cv, state, filteredActions);

		return actions;
	}

	@Override
	protected boolean isClickable(Widget widget) {
		Role role = widget.get(Tags.Role, Roles.Widget);
		if (Role.isOneOf(role, NativeLinker.getNativeClickableRoles())) {
			// Input type are special...
			if (role.equals(WdRoles.WdINPUT)) {
				String type = ((WdWidget) widget).element.type;
				return WdRoles.clickableInputTypes().contains(type);
			}
			return true;
		}

		WdElement element = ((WdWidget) widget).element;
		if (element.isClickable) {
			return true;
		}

		Set<String> clickSet = new HashSet<>(clickableClasses);
		clickSet.retainAll(element.cssClasses);
		return clickSet.size() > 0;
	}

	@Override
	protected boolean isTypeable(Widget widget) {
		Role role = widget.get(Tags.Role, Roles.Widget);
		if (Role.isOneOf(role, NativeLinker.getNativeTypeableRoles())) {
			// Input type are special...
			if (role.equals(WdRoles.WdINPUT)) {
				String type = ((WdWidget) widget).element.type;
				return WdRoles.typeableInputTypes().contains(type);
			}
			return true;
		}

		return false;
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
		this.actionNumber++;
		System.out.println("selectActions running for action " + Integer.toString(actionNumber));
		return super.selectAction(state, actions);
	}

	/**
	 * Execute the selected action.
	 *
	 * @param system the SUT
	 * @param state  the SUT's current state
	 * @param action the action to execute
	 * @return whether or not the execution succeeded
	 */
	@Override
	protected boolean executeAction(SUT system, State state, Action action) {
		System.out.println("selectActions running for action " + Integer.toString(actionNumber));
		setLogContext();
		boolean result =  super.executeAction(system, state, action);
		extractStrings();
		return result;
	}

	/**
	 * TESTAR uses this method to determine when to stop the generation of actions for the
	 * current sequence. You could stop the sequence's generation after a given amount of executed
	 * actions or after a specific time etc.
	 *
	 * @return if <code>true</code> continue generation, else stop
	 */
	@Override
	protected boolean moreActions(State state) {
		return super.moreActions(state);
	}

	/**
	 * This method is invoked each time after TESTAR finished the generation of a sequence.
	 */
	@Override
	protected void finishSequence() {
		System.out.println("Finish sequence called for sequence " + Integer.toString(sequenceNumber));
		super.finishSequence();
		resetBetweenSequencesCKAN();
		startCkan();
	}

	/**
	 * TESTAR uses this method to determine when to stop the entire test.
	 * You could stop the test after a given amount of generated sequences or
	 * after a specific time etc.
	 *
	 * @return if <code>true</code> continue test, else stop
	 */
	@Override
	protected boolean moreSequences() {
		return super.moreSequences();
	}

	@Override
	protected void initTestSession() {
		super.initTestSession();



	}
}
