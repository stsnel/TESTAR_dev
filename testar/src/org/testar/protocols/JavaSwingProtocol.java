/***************************************************************************************************
 *
 * Copyright (c) 2019 Universitat Politecnica de Valencia - www.upv.es
 * Copyright (c) 2019 Open Universiteit - www.ou.nl
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

import java.io.File;
import java.util.Random;
import java.util.Set;

import org.fruit.Assert;
import org.fruit.alayer.Action;
import org.fruit.alayer.Rect;
import org.fruit.alayer.SUT;
import org.fruit.alayer.State;
import org.fruit.alayer.Tags;
import org.fruit.alayer.Verdict;
import org.fruit.alayer.Widget;
import org.fruit.alayer.actions.AnnotatingActionCompiler;
import org.fruit.alayer.actions.StdActionCompiler;
import org.fruit.alayer.exceptions.ActionBuildException;
import org.fruit.alayer.exceptions.StateBuildException;
import org.fruit.alayer.windows.BuilderAccessBridge;
import org.fruit.alayer.windows.UIATags;
import org.fruit.monkey.ConfigTags;
import org.fruit.monkey.Settings;
import org.testar.OutputStructure;

import es.upv.staq.testar.protocols.ClickFilterLayerProtocol;
import nl.ou.testar.RandomActionSelector;
import nl.ou.testar.HtmlReporting.HtmlSequenceReport;

public class JavaSwingProtocol extends ClickFilterLayerProtocol {
	
	protected HtmlSequenceReport htmlReport;
    protected State latestState;

	/** 
	 * Called once during the life time of TESTAR
	 * This method can be used to perform initial setup work
	 * @param   settings   the current TESTAR settings as specified by the user.
	 */
	@Override
	protected void initialize(Settings settings){

		super.initialize(settings);

		BuilderAccessBridge.customJavaSwingButtons = settings.get(ConfigTags.CustomJavaSwingButtons);
		BuilderAccessBridge.searchNonVisibleJavaWindows = settings.get(ConfigTags.SearchNonVisibleJavaWindows);

		//Some options are only to SUT visualization
		if(settings.get(ConfigTags.Mode).toString().contains("Spy")) {
			//This allows to run some internal method to show the widgets of a Java Table
			BuilderAccessBridge.visualizeJavaTable = settings.get(ConfigTags.VisualizeJavaTable);
			BuilderAccessBridge.numberOfRowsToVisualizeJavaTable = settings.get(ConfigTags.NumberRowVisualizeJavaTable);
		}
		else {
			BuilderAccessBridge.visualizeJavaTable = false;
		}

	}
	
	/**
     * This methods is called before each test sequence, allowing for example using external profiling software on the SUT
     */
    @Override
    protected void preSequencePreparations() {
        //initializing the HTML sequence report:
        htmlReport = new HtmlSequenceReport();
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
        latestState = super.getState(system);
        //adding state to the HTML sequence report:
        htmlReport.addState(latestState);
        return latestState;
    }
    
    /**
     * Overwriting to add HTML report writing into it
     *
     * @param state
     * @param actions
     * @return
     */
    @Override
    protected Action preSelectAction(State state, Set<Action> actions){
        // adding available actions into the HTML report:
        htmlReport.addActions(actions);
        return(super.preSelectAction(state, actions));
    }


	/**
	 * Select one of the possible actions (e.g. at random)
	 * @param state the SUT's current state
	 * @param actions the set of available actions as computed by <code>buildActionsSet()</code>
	 * @return  the selected action (non-null!)
	 */
	@Override
	protected Action selectAction(State state, Set<Action> actions){ 
		Assert.isTrue(actions != null && !actions.isEmpty());

		int numberOfCellsJavaTable = 0;

		boolean executeTableAction = false;

		for(Widget w : state) {

			//If exist some table into the Java Swing SUT, read how many childs cells exist
			if(w.get(Tags.Role).toString().contains("Table")) {
				executeTableAction = true;
				numberOfCellsJavaTable = BuilderAccessBridge.childsOfJavaTable;
			}

			if(w.get(UIATags.UIAAutomationId,"").contains("dialog")) {
				executeTableAction = false;
				break;

			}

		}

		//Allow to the user define a maximum number of cells (By default could be high)
		if(numberOfCellsJavaTable > settings.get(ConfigTags.MaxJavaTableCellsToGenerate))
			numberOfCellsJavaTable = settings.get(ConfigTags.MaxJavaTableCellsToGenerate);

		Action a = preSelectAction(state, actions);
		if (a != null){
			return a;
		} else {

			//Coinflip using the number of cells of existing table
			int random = new Random().nextInt(actions.size() + numberOfCellsJavaTable);

			//Check if coinflip determines we are going to execute an action into the table
			if(random < numberOfCellsJavaTable && executeTableAction)
				BuilderAccessBridge.updateActionJavaTable = true;

			return RandomActionSelector.selectAction(actions);
		}

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

		if(BuilderAccessBridge.updateActionJavaTable) {

			//Coinflip determines that we are going to execute an action into the table
			//We have to update the state making an Access Bridge calls to obtain properly a cell position

			//Update state
			state = getState(system);

			//Read the actions of new State (we want to find a TableCell)
			Set<Action> actions = deriveTableActions(system, state);

			//Update to false, will be determined next action iteration
			BuilderAccessBridge.updateActionJavaTable = false;

			if(!actions.isEmpty()) {
				Action tableAction = RandomActionSelector.selectAction(actions);
				htmlReport.addSelectedAction(state, tableAction);
				return super.executeAction(system, state, tableAction);
			}
		}
		htmlReport.addSelectedAction(state, action);
		return super.executeAction(system, state, action);

	}

	/**
	 * This method is used by TESTAR to determine the set of currently available actions.
	 * You can use the SUT's current state, analyze the widgets and their properties to create
	 * a set of sensible actions, such as: "Click every Button which is enabled" etc.
	 * The return value is supposed to be non-null. If the returned set is empty, TESTAR
	 * will stop generation of the current action and continue with the next one.
	 * @param system the SUT
	 * @param state the SUT's current state
	 * @return  a set of actions
	 */
	protected Set<Action> deriveTableActions(SUT system, State state) throws ActionBuildException{

		Set<Action> actions = super.deriveActions(system,state);
		StdActionCompiler ac = new AnnotatingActionCompiler();

		// iterate through all widgets
		for(Widget w : state){
			//Save all new widgets created that represents the cells of the tables
			if(w.get(UIATags.UIAAutomationId,"").toString().contains("TableCell")) {
				actions.add(ac.leftDoubleClickAt(w));
				actions.add(ac.rightClickAt(w));
			}
		}

		return actions;

	}

	//Force actions on Tree widgets with a wrong accessibility
	public void forceActionsIntoChildsWidgetTree(Widget w, Set<Action> actions) {
		StdActionCompiler ac = new AnnotatingActionCompiler();

		if(widgetInsideBounds(w)) {
			actions.add(ac.leftClickAt(w));
			w.set(Tags.ActionSet, actions);
		}

		for(int i = 0; i<w.childCount(); i++) {
			forceActionsIntoChildsWidgetTree(w.child(i), actions);
		}
	}

	/**
	 * Use this customized method instead of GetVisibleChildren Access Bridge call
	 */
	public boolean widgetInsideBounds(Widget w) {
		try {
			int windowsContainerNumber = Integer.parseInt(w.get(Tags.Path).substring(1,2));

			Widget windowsContainer = w.root().child(windowsContainerNumber);

			Rect container = Rect.from(windowsContainer.get(Tags.Shape).x(),windowsContainer.get(Tags.Shape).y(),
					windowsContainer.get(Tags.Shape).width(),windowsContainer.get(Tags.Shape).height());

			if(w.get(Tags.Shape).y() > (container.y() + container.height()))
				return false;
			if(w.get(Tags.Shape).x() > (container.x() + container.width()))
				return false;
		}catch(Exception e) {}
		
		return true;
	}
	
	/**
     * This methods is called after each test sequence, allowing for example using external profiling software on the SUT
     */
    @Override
    protected void postSequenceProcessing() {
        htmlReport.addTestVerdict(getVerdict(latestState).join(processVerdict));
        
        String sequencesPath = getGeneratedSequenceName();
        try {
        	sequencesPath = new File(getGeneratedSequenceName()).getCanonicalPath();
        }catch (Exception e) {}
        		
        String status = (getVerdict(latestState).join(processVerdict)).verdictSeverityTitle();
		String statusInfo = (getVerdict(latestState).join(processVerdict)).info();
		
		statusInfo = statusInfo.replace("\n"+Verdict.OK.info(), "");
		
		//Timestamp(generated by logback.xml) SUTname Mode SequenceFileObject Status "StatusInfo"
		INDEXLOG.info(OutputStructure.executedSUTname
				+ " " + settings.get(ConfigTags.Mode, mode())
				+ " " + sequencesPath
				+ " " + status + " \"" + statusInfo + "\"" );
		
		//Print into command line the result of the execution, useful to work with CI and timestamps
		System.out.println(OutputStructure.executedSUTname
				+ " " + settings.get(ConfigTags.Mode, mode())
				+ " " + sequencesPath
				+ " " + status + " \"" + statusInfo + "\"" );
    }

}