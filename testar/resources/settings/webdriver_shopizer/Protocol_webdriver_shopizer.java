/**
 * Copyright (c) 2018 - 2022 Open Universiteit - www.ou.nl
 * Copyright (c) 2019 - 2022 Universitat Politecnica de Valencia - www.upv.es
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

import es.upv.staq.testar.CodingManager;
import es.upv.staq.testar.NativeLinker;
import org.fruit.Util;
import org.fruit.alayer.*;
import org.fruit.alayer.actions.*;
import org.fruit.alayer.devices.KBKeys;
import org.fruit.alayer.exceptions.ActionBuildException;
import org.fruit.alayer.exceptions.StateBuildException;
import org.fruit.alayer.webdriver.*;
import org.fruit.alayer.webdriver.enums.WdRoles;
import org.fruit.alayer.webdriver.enums.WdTags;
import org.fruit.monkey.Settings;
import org.testar.protocols.WebdriverProtocol;

import java.util.*;

import static org.fruit.alayer.Tags.Blocked;
import static org.fruit.alayer.Tags.Enabled;
import static org.fruit.alayer.webdriver.Constants.scrollArrowSize;
import static org.fruit.alayer.webdriver.Constants.scrollThick;

public class Protocol_webdriver_shopizer extends WebdriverProtocol {

	@Override
	protected void buildStateIdentifiers(State state) {
		CodingManager.buildIDs(state);
		// Reset widgets AbstractIDCustom identifier values to empty
		for(Widget w : state) { w.set(Tags.AbstractIDCustom, ""); }
		// Custom the State AbstractIDCustom identifier
		customBuildAbstractIDCustom(state);
	}

	private synchronized void customBuildAbstractIDCustom(Widget widget){
		if (widget.parent() != null) {

			// dropdown widgets that come from fa-angle-down do not have interesting properties that differentiate them from other dropdowns
			if (widget.get(WdTags.WebCssClasses, "").contains("fa-angle-down")) {
				// Create the default String hash code using the abstract tags selected from the settings file (gh23483ghhk)
				// All dropdown widgets will potentially have this same hash identifier (gh23483ghhk)
				String dropdownWidgetAbstractId = CodingManager.codify(widget, CodingManager.getCustomTagsForAbstractId());
				// Obtain the parent WebTextContent that will help to differentiate one dropdown from others (Products)
				// This WebTextContent will help to differentiate dropdowns (Products, Account, ShopCart, etc)
				String parentDescription = widget.parent() != null ? widget.parent().get(WdTags.WebTextContent,"") : "";
				// Create a new String that contains the widget abstract id and the parent WebTextContent (gh23483ghhkProducts)
				String mergedAbstractId = dropdownWidgetAbstractId + parentDescription;
				// Then calculate the new hash id using new unique string (gh23483ghhkProducts)
				widget.set(Tags.AbstractIDCustom, CodingManager.ID_PREFIX_WIDGET + CodingManager.ID_PREFIX_ABSTRACT_CUSTOM + CodingManager.lowCollisionID(mergedAbstractId));
				// Also set new description using parent description
				// TODO: Maybe only set a new description and use Tags.Desc in the settings file
				widget.set(Tags.Desc, widget.get(Tags.Desc,"") + parentDescription);
				return;
			}

			// Register web page shows information about name, surname, password, etc.. in the text content of the registrationError web widget
			// Because this web text content is dynamic, use only web id for the abstract id
			if(widget.get(WdTags.WebId, "").equals("registrationError")) {
				widget.set(Tags.AbstractIDCustom, CodingManager.ID_PREFIX_WIDGET + CodingManager.ID_PREFIX_ABSTRACT_CUSTOM + CodingManager.codify(widget, WdTags.WebId));
				return;
			}

			// Same for edit address form page http://localhost:8080/shop/customer/editAddress.html
			if(widget.get(WdTags.WebId, "").equals("formError")) {
				widget.set(Tags.AbstractIDCustom, CodingManager.ID_PREFIX_WIDGET + CodingManager.ID_PREFIX_ABSTRACT_CUSTOM + CodingManager.codify(widget, WdTags.WebId));
				return;
			}

			// For all widgets that are sons of My Account (customerAccount) widget
			// Use only the web id to build the AbstractIDCustom
			// TODO: Prob not a good idea because widgets from this dropdown are different, so this will provoke non determinism
//			if(isSonOfCustomerAccount(widget)) {
//				widget.set(Tags.AbstractIDCustom, CodingManager.ID_PREFIX_WIDGET + CodingManager.ID_PREFIX_ABSTRACT_CUSTOM + CodingManager.codify(widget, WdTags.WebId));
//				return;
//			}

			widget.set(Tags.AbstractIDCustom, CodingManager.ID_PREFIX_WIDGET + CodingManager.ID_PREFIX_ABSTRACT_CUSTOM + CodingManager.codify(widget, CodingManager.getCustomTagsForAbstractId()));

		} else if (widget instanceof State) {
			StringBuilder abstractIdCustom;
			abstractIdCustom = new StringBuilder();
			for (Widget childWidget : (State) widget) {
				if (childWidget != widget) {
					customBuildAbstractIDCustom(childWidget);
					abstractIdCustom.append(childWidget.get(Tags.AbstractIDCustom));
				}
			}
			widget.set(Tags.AbstractIDCustom, CodingManager.ID_PREFIX_STATE + CodingManager.ID_PREFIX_ABSTRACT_CUSTOM + CodingManager.lowCollisionID(abstractIdCustom.toString()));
		}
	}

	private boolean isSonOfCustomerAccount(Widget widget) {
		if(widget.parent() == null) return false;
		else if (widget.parent().get(WdTags.WebId, "").equals("customerAccount")) return true;
		else return isSonOfCustomerAccount(widget.parent());
	}

	// http://localhost:8080/shop/
	// http://localhost:8080/admin/
	// admin@shopizer.com
	// password
	/**
	 * This method is invoked each time the TESTAR starts the SUT to generate a new sequence.
	 * This can be used for example for bypassing a login screen by filling the username and password
	 * or bringing the system into a specific start state which is identical on each start (e.g. one has to delete or restore
	 * the SUT's configuration files etc.)
	 */
	@Override
	protected void beginSequence(SUT system, State state) {
		super.beginSequence(system, state);
		// Cookies button
		WdDriver.executeScript("document.getElementsByClassName('cc-btn cc-dismiss')[0].click();");
		Util.pause(1);

		// First time we load the page resources are not loaded correctly, we need to refresh the web page
		WdDriver.getRemoteWebDriver().navigate().refresh();
		Util.pause(1);

		// Navigate to Shopizer login web url
		WdDriver.getRemoteWebDriver().navigate().to("http://localhost:8080/shop/customer/customLogon.html");
		Util.pause(2);

		// Script login sequence
		WdDriver.executeScript("document.getElementById('signin_userName').setAttribute('value','testar@testar.com');");
		WdDriver.executeScript("document.getElementById('signin_password').setAttribute('value','testar');");
		WdDriver.executeScript("document.getElementById('genericLogin-button').click();");
		Util.pause(2);
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

		System.out.println("State AbstractIDCustom: " + state.get(Tags.AbstractIDCustom));

		for (Widget widget: state){
			if(widget.get(WdTags.WebTextContent, "").contains("HTTP Status 404")
					|| widget.get(WdTags.WebTextContent, "").contains("Estado HTTP 404")){
				WdDriver.executeScript("window.history.back();");
				Util.pause(1);
				state = super.getState(system);
			}
		}

		return state;
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
		// Kill unwanted processes, force SUT to foreground
		Set<Action> actions = super.deriveActions(system, state);

		// create an action compiler, which helps us create actions
		// such as clicks, drag&drop, typing ...
		StdActionCompiler ac = new AnnotatingActionCompiler();

		Set<Action> forcedActions = detectForcedActions(state, ac);
		if (forcedActions != null && forcedActions.size() > 0) {
			return forcedActions;
		}

		// TODO: Check how this affects the WdHistoryBackAction of empty states
		// TESTAR may find some empty states without actions when Shopizer is loading state content
		// Always create the possibility to execute a NOP action in case no other actions are available
		// Create this default NOP action also helps to deal with no determinism
		// in the distributed shared algorithm
//		Action nop = new NOP();
//		nop.set(Tags.OriginWidget, state);
//		nop.set(Tags.Desc, "NOP action to wait");
//		actions.add(nop);

		// iterate through all widgets
		for (Widget widget : state) {

			// dropdown widgets that come from fa-angle-down class need a mouse movement but not a click, 
			// this is because a click will close the dropdown
			if (widget.get(WdTags.WebCssClasses, "").contains("fa-angle-down")) {
				actions.add(ac.mouseMove(widget));
			}

			// We dont need to register if we force the login
//			if(widget.get(WdTags.WebId, "").contains("registrationForm")) {
//				actions.add(registrationFormFill(state, widget));
//			}
			// We dont need to login if we force the login in the beginsequence
//			if(widget.get(WdTags.WebId, "").contains("login-form")) {
//				actions.add(loginFormFill(state, widget));
//			}
			if(widget.get(WdTags.WebId, "").contains("searchField")) {
				actions.add(new CompoundAction.Builder()
						.add(ac.clickTypeInto(widget, "table", true), 10)
						.add(ac.hitKey(KBKeys.VK_ENTER), 10)
						.build(widget));
			}
			if(widget.get(WdTags.WebId, "").contains("contactForm")) {
				actions.add(contactUsFormFill(state, widget));
			}
			if(widget.get(WdTags.WebId, "").contains("changeAddressForm")) {
				actions.add(changeAddressFormFill(state, widget));
			}

			// only consider enabled and non-tabu widgets
			if (!widget.get(Enabled, true) || blackListed(widget)) {
				continue;
			}

			// slides can happen, even though the widget might be blocked
			//addSlidingActions(actions, ac, scrollArrowSize, scrollThick, widget);

			// If the element is blocked, TESTAR can't click on or type in the widget
			if (widget.get(Blocked, false)) {
				continue;
			}

			// Ignore link widgets that do not contains informative properties to identify them
			// English, My Account, Shopping cart
			if(widget.get(WdTags.Desc, "").equals("a")) {
				continue;
			}

			// Ignore widgets that contains a son dropdown widget
			// because we are deriving a mouse movement action in the dropdown
			if(widget.get(Tags.Role, Roles.Widget).equals(WdRoles.WdA) && containsDropdownSon(widget)) {
				continue;
			}

			// type into text boxes
			if (isAtBrowserCanvas(widget) && isTypeable(widget) && (whiteListed(widget) || isUnfiltered(widget)) ) {
				if(widget.get(WdTags.WebCssClasses,"").contains("tt-hint")) {
					// Ignore duplicated search bar text box
					continue;
				}
				actions.add(ac.clickTypeInto(widget, getRandomShopizerData(widget), true));
			}

			// left clicks, but ignore links outside domain
			if (isAtBrowserCanvas(widget) && isClickable(widget) && !isLinkDenied(widget) && (whiteListed(widget) || isUnfiltered(widget)) ) {
				// Click on select web items opens the menu but does not allow TESTAR to select an item,
				// thats why we need a custom action selection
				if(widget.get(Tags.Role, Roles.Widget).equals(WdRoles.WdSELECT)) {
					//actions.add(randomFromSelectList(widget));
				} else {
					actions.add(ac.leftClickAt(widget));
				}
			}
		}

		return actions;
	}

	/**
	 * Check if current web widget contains a son dropdown widget. 
	 * 
	 * @param widget
	 * @return
	 */
	private boolean containsDropdownSon(Widget widget) {
		if(widget.childCount() > 0) {
			for(int i = 0; i < widget.childCount(); i++){
				if(widget.child(i).get(WdTags.WebCssClasses, "").contains("fa-angle-down")){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Get specific text data for shopizer text fields
	 * 
	 * @param w
	 * @return
	 */
	private String getRandomShopizerData(Widget w) {
		String[] example = {"aaaa", "1234", "01-01-2021"};
		if(w.get(WdTags.WebId, "").toLowerCase().contains("example")) {
			return example[new Random().nextInt(example.length)];
		}
		return this.getRandomText(w);
	}

	/**
	 * Create a specific action to fill the register user form. 
	 * This only works if we are not logged all the sequence. 
	 * 
	 * @param state
	 * @return
	 */
	private Action registrationFormFill(State state, Widget widget) {
		// http://localhost:8080/shop/customer/registration.html
		StdActionCompiler ac = new AnnotatingActionCompiler();
		return new CompoundAction.Builder()
				.add(ac.clickTypeInto(getWidgetWithMatchingTag("name", "billing.firstName", state), "testar", true), 2)
				.add(ac.clickTypeInto(getWidgetWithMatchingTag("name", "billing.lastName", state), "testar", true), 2)
				// Ignore country and state, use default values
				.add(ac.pasteTextInto(getWidgetWithMatchingTag("name", "emailAddress", state), "testar@testar.com", true), 2)
				.add(ac.hitKey(KBKeys.VK_TAB), 1)
				.add(new Type("testar"), 2) // password may be out of the screen resolution
				.add(ac.hitKey(KBKeys.VK_TAB), 1)
				.add(new Type("testar"), 2) // checkPassword may be out of the screen resolution
				.add(ac.hitKey(KBKeys.VK_ENTER), 1)
				.build(widget);
	}
	private Action loginFormFill(State state, Widget widget) {
		// http://localhost:8080/shop/customer/customLogon.html
		StdActionCompiler ac = new AnnotatingActionCompiler();
		return new CompoundAction.Builder()
				.add(ac.pasteTextInto(getWidgetWithMatchingTag("name", "signin_userName", state), "testar@testar.com", true), 2)
				.add(ac.clickTypeInto(getWidgetWithMatchingTag("name", "signin_password", state), "testar", true), 2)
				.add(ac.leftClickAt(getWidgetWithMatchingTag("WebId", "genericLogin-button", state)), 2)
				.build(widget);
	}
	private Action contactUsFormFill(State state, Widget widget) {
		// http://localhost:8080/shop/store/contactus.html
		StdActionCompiler ac = new AnnotatingActionCompiler();
		return new CompoundAction.Builder()
				.add(ac.pasteTextInto(getWidgetWithMatchingTag("WebId", "name", state), "SomeName", true), 2)
				.add(ac.clickTypeInto(getWidgetWithMatchingTag("WebId", "email", state), "email@email.com", true), 2)
				.add(ac.clickTypeInto(getWidgetWithMatchingTag("WebId", "subject", state), "SomeSubject", true), 2)
				.add(ac.clickTypeInto(getWidgetWithMatchingTag("WebId", "comment", state), "blabla", true), 2)
				.add(ac.hitKey(KBKeys.VK_TAB), 1)
				.add(ac.hitKey(KBKeys.VK_ENTER), 1) // We are not a robot
				.add(ac.hitKey(KBKeys.VK_TAB), 1)
				.add(ac.hitKey(KBKeys.VK_TAB), 1)
				.add(ac.hitKey(KBKeys.VK_TAB), 1)
				.add(ac.hitKey(KBKeys.VK_ENTER), 1) // Send button using tabs and enter because may be out of screen resolution
				.build(widget);
	}
	private Action changeAddressFormFill(State state, Widget widget) {
		// http://localhost:8080/shop/customer/editAddress.html
		StdActionCompiler ac = new AnnotatingActionCompiler();
		return new CompoundAction.Builder()
				.add(ac.pasteTextInto(getWidgetWithMatchingTag("WebId", "firstName", state), "testar", true), 2)
				.add(ac.clickTypeInto(getWidgetWithMatchingTag("WebId", "lastName", state), "testar", true), 2)
				.add(ac.clickTypeInto(getWidgetWithMatchingTag("WebId", "address", state), "testar street", true), 2)
				.add(ac.clickTypeInto(getWidgetWithMatchingTag("WebId", "city", state), "testar city", true), 2)
				.add(ac.hitKey(KBKeys.VK_TAB), 1)
				.add(ac.hitKey(KBKeys.VK_TAB), 1)
				.add(new Type("testar prov"), 2)
				.add(ac.hitKey(KBKeys.VK_TAB), 1)
				.add(new Type("01234"), 2)
				.add(ac.hitKey(KBKeys.VK_TAB), 1)
				.add(new Type("0123456789"), 2)
				.add(ac.hitKey(KBKeys.VK_ENTER), 1) // Send button using tabs and enter because may be out of screen resolution
				.build(widget);
	}

	/**
	 * Randomly select one item from the select list widget. 
	 * 
	 * @param w
	 * @return
	 */
	private Action randomFromSelectList(Widget w) {
		int selectLength = 1;
		String elementId = w.get(WdTags.WebId, "");
		String elementName = w.get(WdTags.WebName, "");

		// Get the number of values of the specific select list item
		// Comment out because we are going to select always the first value of the select list
		/*try {
            String query = String.format("return document.getElementById('%s').length", elementId);
            Object response = WdDriver.executeScript(query);
            selectLength = ( response != null ? Integer.parseInt(response.toString()) : 1 );
        } catch (Exception e) {
            System.out.println("*** ACTION WARNING: problems trying to obtain select list length: " + elementId);
        }*/

		// Select one of the values randomly, or the first one if previous length failed
		if(!elementId.isEmpty()) {
			try {
				//String query = String.format("return document.getElementById('%s').item(%s).value", elementId, new Random().nextInt(selectLength));
				String query = String.format("return document.getElementById('%s').item(%s).value", elementId, selectLength);
				Object response = WdDriver.executeScript(query);
				return (response != null ?  new WdSelectListAction(elementId, response.toString(), w) : new AnnotatingActionCompiler().leftClickAt(w) );
			} catch (Exception e) {
				System.out.println("*** ACTION WARNING: problems trying randomly select a list value using WebId: " + elementId);
				e.printStackTrace();
			}
		} else if(!elementName.isEmpty()) {
			try {
				String query = String.format("return document.getElementsByName('%s')[0].item(%s).value", elementName, selectLength);
				Object response = WdDriver.executeScript(query);
				return (response != null ?  new WdSelectListAction(elementName, response.toString(), w) : new AnnotatingActionCompiler().leftClickAt(w) );
			} catch (Exception e) {
				System.out.println("*** ACTION WARNING: problems trying randomly select a list value using WebName: " + elementName);
				e.printStackTrace();
			}
		}

		return new AnnotatingActionCompiler().leftClickAt(w);
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
			// Specific class="input" for parasoft SUT
			if(widget.get(WdTags.WebCssClasses, "").contains("input")) {
				return true;
			}

			// Input type are special...
			if (role.equals(WdRoles.WdINPUT)) {
				String type = ((WdWidget) widget).element.type;
				return WdRoles.typeableInputTypes().contains(type);
			}
			return true;
		}

		return false;
	}
}