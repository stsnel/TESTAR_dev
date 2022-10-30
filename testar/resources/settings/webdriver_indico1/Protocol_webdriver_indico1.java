import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONTokener;
import org.testar.CompoundTextActionSelector;
import org.testar.RandomActionSelector;
import org.testar.SutVisualization;
import org.testar.action.priorization.ActionTags;
import org.testar.instrumentation.InstrumentationWebUtils;
import org.testar.managers.InterestingStringsDataManager;
import org.testar.managers.InterestingStringsFilteringManager;
import org.testar.monkey.Assert;
import org.testar.monkey.alayer.Action;
import org.testar.monkey.alayer.Roles;
import org.testar.monkey.alayer.State;
import org.testar.monkey.alayer.SUT;
import org.testar.monkey.alayer.Tags;
import org.testar.monkey.alayer.Widget;
import org.testar.monkey.alayer.actions.AnnotatingActionCompiler;
import org.testar.monkey.alayer.actions.CompoundAction;
import org.testar.monkey.alayer.actions.KeyDown;
import org.testar.monkey.alayer.actions.StdActionCompiler;
import org.testar.monkey.alayer.actions.Type;
import org.testar.monkey.alayer.actions.WdHistoryBackAction;
import org.testar.monkey.alayer.devices.KBKeys;
import org.testar.monkey.alayer.exceptions.ActionBuildException;
import org.testar.monkey.alayer.exceptions.SystemStartException;
import org.testar.monkey.alayer.SUT;
import org.testar.monkey.alayer.Tags;
import static org.testar.monkey.alayer.Tags.Blocked;
import static org.testar.monkey.alayer.Tags.Enabled;
import org.testar.monkey.alayer.webdriver.enums.WdRoles;
import org.testar.monkey.alayer.webdriver.enums.WdTags;
import static org.testar.monkey.alayer.webdriver.Constants.scrollArrowSize;
import static org.testar.monkey.alayer.webdriver.Constants.scrollThick;
import org.testar.statemodel.AbstractState;
import org.testar.statemodel.ConcreteState;
import org.testar.statemodel.StateModelManager;
import org.testar.statemodel.sequence.Sequence;
import org.testar.monkey.ConfigTags;
import org.testar.monkey.Settings;
import org.testar.protocols.CodeAnalysisWebdriverProtocol;

/** Protocol for code analysis with Indico SUT */

public class Protocol_webdriver_indico1 extends CodeAnalysisWebdriverProtocol {

    protected String applicationUsername, applicationPassword, expCondition;
    protected boolean loggedIn=false;
	protected CompoundTextActionSelector selector;

   	/**
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 *
	 * @param settings the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings) {
		this.applicationUsername = settings.get(ConfigTags.ApplicationUsername);
        this.applicationPassword = settings.get(ConfigTags.ApplicationPassword);
		this.expCondition = settings.get(ConfigTags.ExpCondition);
		expConditionChecks(settings);

 		super.initialize(settings);

		if ( useCustomActionSelection() ) {
			selector = new CompoundTextActionSelector(
				settings.get(ConfigTags.CompoundTextActionInitialProbability),
				settings.get(ConfigTags.CompoundTextActionResetProbability),
				settings.get(ConfigTags.CompoundTextActionGrowthRate),
				settings.get(ConfigTags.CompoundTextActionLowPriorityInitialFactor),
				settings.get(ConfigTags.CompoundTextActionLowPriorityResetFactor),
				settings.get(ConfigTags.CompoundTextActionLowPriorityGrowthRate),
				settings.get(ConfigTags.CompoundTextActionHighPriorityInitialFactor),
				settings.get(ConfigTags.CompoundTextActionHighPriorityResetFactor),
				settings.get(ConfigTags.CompoundTextActionHighPriorityShrinkRate)
			);
		}
	}

	/**
	 * Verify that settings related to experiment condition are valid. Exit with error
	 * message if condition settings are incorrect
	 */
	protected void expConditionChecks(Settings settings) {

		boolean shouldSetLogContext=false,shouldProcessDataAfterAction=false, shouldUseCompoundTextAction=false;

		if ( expCondition.equals("control-defaultactionselection") ) {
			shouldSetLogContext = false;
			shouldProcessDataAfterAction = false;
			shouldUseCompoundTextAction = false;
		}
		else if ( expCondition.equals("control-customactionselection") ) {
			shouldSetLogContext = false;
			shouldProcessDataAfterAction = false;
			shouldUseCompoundTextAction = true;
		}
		else if ( expCondition.equals("experimental")) {
			shouldSetLogContext = true;
			shouldProcessDataAfterAction = true;
			shouldUseCompoundTextAction = true;
		}
		else {
			logger.error("ExpCondition does not have valid value.");
			System.exit(1);
		}

		boolean setLogContext = settings.get(ConfigTags.SetLogContext);
		if ( setLogContext != shouldSetLogContext ) {
			logger.error("Error: Experimental condition setting mismatch for SetLogContext");
			System.exit(1);
		}

		boolean processDataAfterAction = settings.get(ConfigTags.ProcessDataAfterAction);
		if ( shouldProcessDataAfterAction != processDataAfterAction ) {
			logger.error("Error: Experimental condition setting mismatch for ProcessDataAfterAction");
			System.exit(1);
		}

		boolean compoundTextActionLogicEnabled = settings.get(ConfigTags.CompoundTextActionLogicEnabled);
		if ( compoundTextActionLogicEnabled != shouldUseCompoundTextAction ) {
			logger.error("Error: Experimental condition setting mismatch for CompoundTextActionLogicEnabled");
			System.exit(1);
		}
	}

	protected boolean useCustomActionSelection () {
		return this.expCondition.equals("experimental") || this.expCondition.equals("control-customactionselection");
	}

    protected void initializeDataManager() {
        dataManager = new InterestingStringsDataManager(this.fullStringRate, this.maxInputStrings, this.typeMatchRate);
        dataManager.loadInputValues();
    }

    @Override
    protected void initializeFilteringManager() {
        filteringManager = new InterestingStringsFilteringManager((InterestingStringsDataManager)this.dataManager);
        filteringManager.loadFilters();
    }

    @Override
	protected SUT startSystem() throws SystemStartException {
        this.loggedIn=false;
        return super.startSystem();
    }

    @Override
    protected void waitForSUT() {
        if (codeAnalysisDebugMessages) {
            logger.info("Indico protocol before wait for SUT.");
        }

        if (! InstrumentationWebUtils.waitForURL(applicationBaseURL + "/bootstrap", 300, 10,  1, 200) )  {
            logger.info("Failed waiting for SUT to be ready ...");
        }

        if (codeAnalysisDebugMessages) {
            logger.info("Indico protocol after wait for SUT.");
        }
    }


    @Override
	protected void beginSequence(SUT system, State state) {
		logger.info("Begin sequence @ " + String.valueOf(System.currentTimeMillis()));
        super.beginSequence(system,state);
        if ( ! this.loggedIn ) {
            loginSUT(system,state);
            this.loggedIn=true;
        }
    }

    @Override
    protected void finishSequence(){
        super.finishSequence();
	logger.info("End sequence @ " + String.valueOf(System.currentTimeMillis()));
    }

    @Override
    protected void initTestSession() {
	logger.info("Begin experiment (TS) @ " + String.valueOf(System.currentTimeMillis()));
	super.initTestSession();
    }

    @Override
    protected void closeTestSession() {
 	super.closeTestSession();
	logger.info("End experiment (TS) @ " + String.valueOf(System.currentTimeMillis()));
    }

    protected void loginSUT(SUT system, State state) {
        if (! InstrumentationWebUtils.waitForURL(applicationBaseURL + "/testar-registertester", 60, 10,  1, 200) )  {
            logger.info("Register tester not ready ...");
        }

  	waitLeftClickAndTypeIntoWidgetWithMatchingTag("name","identifier", this.applicationUsername, state, system, 3, 1.0);
  	waitLeftClickAndTypeIntoWidgetWithMatchingTag("name","password", this.applicationPassword, state, system, 3, 1.0);
  	waitAndLeftClickWidgetWithMatchingTag("name", "Login with Indico", state, system, 3, 1.0);
    }


    @Override
    protected void processSUTDataAfterAction(JSONTokener tokener) {
        JSONArray root = new JSONArray(tokener);

        Set<Map<String,String>> output = new HashSet<>();

        for (int i = 0; i < root.length(); i++) {
            JSONArray inner = root.getJSONArray(i);
            String type = inner.getString(0);
            String value = inner.getString(1);
            logger.info("Extracted string " + type + " / " + value);
            Map<String, String> innerMap = new HashMap<>();
            innerMap.put("type", type);
            innerMap.put("value", value);
            output.add(innerMap);
        }

		AbstractState lastAbstractState = stateModelManager.getCurrentAbstractState();

		if ( lastAbstractState == null ) {
			logger.error("Indico protocol could not get last abstract state from state model manager.");
			logger.error("Unable to save text data in state model.");
		}
		else {
			logger.info("Indico protocol saved text data to abstract state.");
			stateModelManager.associateTextInputs(lastAbstractState, output);
		}
    }

    /**
     *
     * This method has been overridden for the Indico protocol to filter out clicks on links
     * to API endpoints, as well as logout actions.
     *
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
		// Load text inputs
		if (stateModelManager == null ) {
			logger.info("No state model manager, so not loading text data. This can happen in spy mode.");
		}
		else {
			AbstractState abstractState = stateModelManager.getCurrentAbstractState();
			Set<Map<String,String>> textInputs = null;
			if ( abstractState == null ) {
				logger.info("Abstract state is null. Passing empty text input set to data manager.");
				textInputs = new HashSet<Map<String,String>> ();
			}
			else {
				logger.info("Retrieved text inputs for action derivation from state model.");
				textInputs = stateModelManager.getTextInputs(abstractState);
			}
			((InterestingStringsDataManager)(dataManager)).loadInput(textInputs);
		}

        // Indico Customization: start with empty actions HashSet, so that we only rely
        // on this method definition for deriving actions.
		Set<Action> actions = new HashSet<>();
		Set<Action> filteredActions = new HashSet<>();
		boolean useCustomActionSelection = useCustomActionSelection();

		// create an action compiler, which helps us create actions
		// such as clicks, drag&drop, typing ...
		StdActionCompiler ac = new AnnotatingActionCompiler();

		// This variable is used when for building a compound text action
		List<Action> textActions = new ArrayList<>();

		// Check if forced actions are needed to stay within allowed domains
		Set<Action> forcedActions = detectForcedActions(state, ac);

		// iterate through all widgets
		for (Widget widget : state) {

            /* Indico Customization to skip various elements:
             * - Don't click on the Logout button, because there isn't much functionality we can test if we're
             *   not logged in anymore.
             * - Don't upload stuff, because of high risk that TESTAR gets stuck performing actions in the upload
             *   dialog for a long time.
	     * - Don't enter the timezone selector dialog, because TESTAR tends to get stuck in it for a while, and it's
	     *   not part of the main application language.
	     * - Contact page: it has no useful functionality
             */


            if( widget.get(WdTags.WebName,"").equals("Logout")
                || widget.get(WdTags.WebName,"").equals("Upload files")
                || widget.get(WdTags.WebName,"").equals("Contact")
		|| widget.get(WdTags.WebGenericTitle,"").equals("tz-selector-link")
                || widget.get(WdTags.WebCssClasses,"").contains("icon-file") ) {
                continue;
            }

			/** Indico customization to tag widgets that should be assigned
			 *  low probability after entering text datta.
			 *  This is currently only applied to clickable widgets.
			 */
			boolean isLowPriorityWidget = false;
			if ( widget.get(WdTags.WebName,"").equals("Datasets")
				|| widget.get(WdTags.WebName,"").equals("Create event")
				|| widget.get(WdTags.WebName,"").equals("Administration")
				|| widget.get(WdTags.WebName,"").equals("Home")
				|| widget.get(WdTags.WebName,"").equals("My profile")
				|| widget.get(WdTags.WebName,"").equals("Help")
				|| widget.get(WdTags.WebName,"").equals("Indico")
				|| widget.get(WdTags.WebGenericTitle,"").equals("protection-details-link")
				|| widget.get(WdTags.WebGenericTitle,"").equals("user-settings-link") ) {
				isLowPriorityWidget = true;
			}

			/** CKAN customization to tag widgets that should be assigned
			 *  high probability after entering text data.
			 *  This is currently only applied to clickable widgets.
			 */
			boolean isHighPriorityWidget = false;
			if ((! isLowPriorityWidget ) &&
				( widget.get(WdTags.WebValue,"").equals("Save")
				|| widget.get(WdTags.WebValue,"").equals("Add")
				|| widget.get(WdTags.WebType,"").equals("submit")
				|| widget.get(WdTags.WebCssClasses,"").contains("search")
				|| widget.get(WdTags.WebCssClasses,"").contains("primary")
				|| widget.get(WdTags.WebCssClasses,"").contains("i-button")
				|| widget.get(WdTags.WebCssClasses,"").contains("button") ) ) {
				isHighPriorityWidget = true;
			}

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

			// type into text boxes ( either as a single action, or as a compound action, depending on settings)
			if ( isAtBrowserCanvas(widget) && isTypeable(widget) ) {
				if(whiteListed(widget) || isUnfiltered(widget)){
					if ( settings.get(ConfigTags.CompoundTextActionLogicEnabled ) ) {
						textActions.add(ac.clickTypeInto(widget, this.getRandomText(widget), true));
					}
					else {
						actions.add(ac.clickTypeInto(widget, this.getRandomText(widget), true));
					}

				}else{
					// filtered and not white listed:
					filteredActions.add(ac.clickTypeInto(widget, this.getRandomText(widget), true));
				}
			}

			// left clicks, but ignore links outside domain
			if (isAtBrowserCanvas(widget) && isClickable(widget)) {
				Action clickAction = ac.leftClickAt(widget);
				if ( useCustomActionSelection && isLowPriorityWidget) {
					clickAction.set(ActionTags.CompoundTextLowPriorityWidget, true);
				}
				else if ( useCustomActionSelection && isHighPriorityWidget ) {
					clickAction.set(ActionTags.CompoundTextHighPriorityWidget, true);
					logger.info("Set high priority for widget ...");
				}

				if(whiteListed(widget) || isUnfiltered(widget)){
					if (!isLinkDenied(widget)) {
						actions.add(clickAction);
					}else{
						// link denied:
						filteredActions.add(clickAction);
					}
				}else{
					// filtered and not white listed:
					filteredActions.add(clickAction);
				}
			}
		}

		if ( settings.get(ConfigTags.CompoundTextActionLogicEnabled ) && textActions.size() > 0 ) {
			Action textAction = new CompoundAction(textActions);
			textAction.set(ActionTags.CompoundTextAction, true);
			textAction.set(Tags.Role, Roles.Text);
			textAction.set(Tags.Desc, "Compound text action to enter text into all text widgets");
			actions.add(textAction);
		}

		if(actions.isEmpty()) {
			return new HashSet<>(Collections.singletonList(new WdHistoryBackAction()));
		}

		// If we have forced actions, prioritize and filter the other ones
		if (forcedActions != null && forcedActions.size() > 0) {
			filteredActions = actions;
			actions = forcedActions;
		}

		//Showing the grey dots for filtered actions if visualization is on:
		if(visualizationOn || mode() == Modes.Spy) SutVisualization.visualizeFilteredActions(cv, state, filteredActions);

		return actions;
	}

	protected Action selectAction(State state, Set<Action> actions){
		Action selectedAction = null;
		Assert.isTrue(actions != null && !actions.isEmpty());
		if (codeAnalysisDebugMessages) {
            logger.info("Indico1 start selectAction.");
        }
        this.actionNumber++;
		if ( useCustomActionSelection()  ) {
			selectedAction = selector.selectAction(actions);
		}
		else {
			selectedAction = RandomActionSelector.selectAction(actions);
		}
		if (codeAnalysisDebugMessages) {
            logger.info("Indico1 end selectAction.");
        }
		return selectedAction;
	}

	@Override
    protected String getRandomText(Widget w){
		return filteringManager.getRandomText(w);
    }

}
