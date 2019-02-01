package nl.ou.testar.subroutine;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.fruit.alayer.Action;
import org.fruit.alayer.SUT;
import org.fruit.alayer.State;
import org.fruit.alayer.Tags;
import org.fruit.alayer.Widget;
import org.fruit.alayer.actions.CompoundAction.Builder;
import org.fruit.alayer.devices.KBKeys;
import org.fruit.alayer.actions.StdActionCompiler;
import org.fruit.alayer.actions.CompoundAction;
import org.fruit.alayer.actions.KeyDown;
import org.fruit.alayer.actions.KeyUp;
import org.fruit.alayer.actions.UrlActionCompiler;
import org.fruit.alayer.exceptions.ActionBuildException;
import org.fruit.monkey.ConfigTags;
import org.fruit.monkey.Settings;

/**
 * Class responsible for executing a general form subroutine.
 *
 * @author Conny Hageluken
 * @Date January 2019
 */
public class FormProtocol
    extends SubroutineProtocol {

  /**
   * Constructor.
   * Including settings for print facilities
   * - Print additional information on widgets
   * - Print additional information on screen number where you want to start an action based on start state.
   * - Set number of editable widgets is used as a criterion to define a form
   * - Set maximum number of screens a form consists of.
   */
  public FormProtocol() {
    setPrintWidgets(false);
     setPrintBuild(true);
    setMinimumNumberOfEditWidgets(5);
  }

  // if true a subroutine can start
  private boolean startBoolean = false;

  /**
   * Compound action for widget on a form.
   */
  protected Action actionOnForm;

  /**
   * List of all created compound actions.
   */
  protected Set<Action> listOfActionsOnForm = new HashSet<Action>();

  /**
   * Data set with options for widget on a form.
   */
  protected HashMap<Integer,String[]> formData = new HashMap<Integer,String[]>();

  /**
   * Sequence number of previous action.
   */
  protected int previousSequence = 0;

  /**
   * Number of editable widgets is used as a criterion to define a form.
   * default value is 5
   */
  protected int minimumNumberOfEditWidgets = 5;

  /**
   *  Actual screen number.
   */
  protected int screenNumber = 0;

  /**
   * Form consists of maximumNumberOfScreens.
   * If
   * - screenNumber = 0                           form has not yet been completed in this sequence
   * - 0 < screenNumber < maximumNumberOfScreens  continue with next screen
   * - screenNumber >= maximumNumberOfScreens     form has been completed in this sequence
   */
  private Integer maximumNumberOfScreens = 1;

  // Print additional information on widgets
  private boolean printWidgets = false;

  // Print additional information on building the compound action
  private boolean printBuild = false;

  /**
   * Initialize input data file and input data set
   */
  protected void initInputData(Settings settings) {
    setDataInputFile(new File(settings.get(ConfigTags.FormData)));
    formData = readDataInputfile();
  }

  // Counting the number of editable widgets, Role = UIAEdit
  protected int countNumberOfEditWidgets(State state) {
    int numberOfEditWidgets = 0;
    int teller = 0;
    for (Widget widget: state) {
      teller++;
      String role = widget.get(Tags.Role, null).toString();
      String title = widget.get(Tags.Title, "");
      if ("UIAEdit".equalsIgnoreCase(role) && !title.isEmpty()) {
        numberOfEditWidgets++;
        if (isPrintWidgets()) {
          System.out.println("[FormProtocol] Editable widget no " + teller + " role: " + role + " title: " + title);
          System.out.println("[FormProtocol] Editable widgets " + numberOfEditWidgets);
        }
      }
      // optional: Print additional information on widgets
      if (isPrintWidgets() && !title.isEmpty()) {
        System.out.println("[FormProtocol] no " + teller + " role: " + role + " title: " + title);
      }
    }
    return numberOfEditWidgets;
  }

  /**
   * State is fulfilling criterion for running a form subroutine.
   * @param state     the SUT's current state
   * @return state    is ready for form subroutine action
   */
  @Override
  public boolean startState(State state) {

    // Set actual index of widget condition to 1 if you are using the Form Data tab on
    // the TESTAR panel
    setActualIndexSD(1);
    int numberOfEditWidgets = countNumberOfEditWidgets(state);

    // Not all screens of the form have been evaluated
    if (!isStartBoolean()) {

      setStartBoolean( (numberOfEditWidgets >= minimumNumberOfEditWidgets));
    }
    // Printing additional information, optional (setPrintScreenNumber(true))
    if (isPrintWidgets()) {
       System.out.print("[FP/startState] form available, create compound action\n");
       System.out.println("                number of editable widgets is "
                + numberOfEditWidgets + " (min = " + minimumNumberOfEditWidgets + ")");
    }
    return isStartBoolean();
  }

  /**
   * Create compound action on all available fields with same role using form data set.
   *
   * @param builder  used to build the compound action
   * @param role     specific widget role
   * @param ac       either an StdActionCompiler or UrlActioncompiler
   * @param state    the SUT's current state
   * @return builder subset for the specified rol
   */
  public Builder selectRoleSet(Builder builder, String role, StdActionCompiler ac,
      State state) {
    for (Integer index: formData.keySet()) {
      String searchRole = formData.get(index)[0];
      String searchLabel = formData.get(index)[1];
      String searchInput = formData.get(index)[2];
      for (Widget w: getTopWidgets(state)) {
        String textLabel = w.get(Tags.Title);
        String textRole = w.get(Tags.Role).toString();

        if (// item in form data set
            searchLabel.equals(textLabel) && searchRole.equals(textRole)
            // default role, input variable
            && searchRole.equals(role)) {

          // Printing additional information, optional (setPrintBuild(true))
          if (printBuild) {
            System.out.print("Compound action: " + searchRole + " => ");
          }

          // Role: UIAEdit or UIASpinner => clickTypeInto
          if ("UIAEdit".equals(role)
              || "UIASpinner".equals(role)) {
            if (!searchInput.isEmpty()) {

              // Printing additional information, optional (setPrintBuild(true))
              if (printBuild) {
                System.out.println("clickTypeInfo(" + searchLabel + ": " + searchInput + ")");
              }
              builder.add(ac.clickTypeInto(w, searchInput), 5);
            }
          }

          // Role: UIAButton or UIARadioButton => leftClickAt
          if ( ("UIAButton".equals(role)
              || "UIARadioButton".equals(role)
              || "UIACustomControl".equals(role)
              || "UIACheckBox".equals(role))
              && searchRole.equals(textRole)) {

            // Printing additional information, optional (setPrintBuild(true))
            if (printBuild) {
              System.out.println("leftClickAt(" + searchLabel + ")");
            }
            builder.add(ac.leftClickAt(w), 5);
          }
          break;
        }
      }
    }
    return builder;
  }

  public Builder AddScrollPage(Builder builder) {
    if (printBuild) {
      System.out.println("Compound action: => page down");
    }
   // page down (if more screens are available)
    return builder.add(new KeyDown(KBKeys.VK_PAGE_DOWN), 5).add(new KeyUp(KBKeys.VK_PAGE_DOWN), 5);
  }

  /**
   * This method is invoked each time TESTAR starts to generate a new form subroutine.
   * It creates a compound action based on the contents of formData
   * @param state the SUT's current state
   */
  @Override
  public void startSubroutine(State state) {
    UrlActionCompiler uc = new UrlActionCompiler();
    Builder builder = new CompoundAction.Builder();

    // add text fields using form data set
    builder = selectRoleSet(builder, "UIAEdit", uc, state);

    // add radio button using form data set
    builder = selectRoleSet(builder, "UIARadioButton", uc, state);

    // add check box using form data set
    builder = selectRoleSet(builder, "UIACheckBox", uc, state);

    // add text fields using form data set
    builder = selectRoleSet(builder, "UIASpinner", uc, state);

    // add custom control using form data set
    builder = selectRoleSet(builder, "UIACustomControl", uc, state);

    // add buttons using form data set
    builder = selectRoleSet(builder, "UIAButton", uc, state);

    if (maximumNumberOfScreens > 1) {
      // add page down
      AddScrollPage(builder);
    }

     // final compound action
    actionOnForm = builder.build();
  }

  @Override
  /**
   * This method is used by TESTAR to determine the set of
   * currently available actions.
   * Derivation of actions:
   * if startState() is true
   *   - create compound action based on form data file
   * if startState() is false
   *   - TESTAR remains in control
   * @param sut the system under test
   * @param state the SUT's current state
   * @return a set of actions
   * @throws ActionBuildException
   */
  protected Set<Action> deriveActions(SUT sut, State state) throws ActionBuildException {
    Set<Action> actions = new HashSet<Action>();
    if (startState(state)) {
      // First screen is available
      if (listOfActionsOnForm.size() == 0) {
        System.out.println("Start state form subroutine");
      }

      // Create compound action for current screen
      startSubroutine(state);

      // Add eacht unique compound action (i.e. screen) to a list.
      // As such count the number of screens that have been completed
      if (!listOfActionsOnForm.contains(actionOnForm)) {
        listOfActionsOnForm.add(actionOnForm);
      }

      // Last screen is available
      if (listOfActionsOnForm.size() == maximumNumberOfScreens) {
        System.out.println("Finish state form subroutine");
        setStartBoolean(false);
      }

      // Current action only contains the previously defined compound action
      actions.add(actionOnForm);
    } else {
      System.out.println("Continue TESTAR");
      actions = defaultDeriveActions(sut, state);
    }
    return actions;
  }

  protected boolean executeAction(SUT system, State state, Action action) {
    // Execute current action on form
    boolean actionExecuted = super.executeAction(system, state, action);
    if (actionExecuted) {
      // Start a new sequence
      if (previousSequence < sequenceCount()) {
        previousSequence = sequenceCount();
      }
      // Reset action on form
      actionOnForm = null;
    }
    return actionExecuted;
  }

  /**
   * @return the startBoolean
   */
  public boolean isStartBoolean() {
    return startBoolean;
  }

  /**
   * @param startBoolean the startBoolean to set
   */
  public void setStartBoolean(boolean startBoolean) {
    this.startBoolean = startBoolean;
  }

  /**
   * @param minimumNumberOfEditWidgets the minimumNumberOfEditWidgets to set
   */
  public void setMinimumNumberOfEditWidgets(int minimumNumberOfEditWidgets) {
    this.minimumNumberOfEditWidgets = minimumNumberOfEditWidgets;
  }

  /**
   * @param screenNumber the screenNumber to set
   */
  public void setScreenNumber(int screenNumber) {
    this.screenNumber = screenNumber;
  }

  /**
   * @param maximumNumberOfScreens the maximumNumberOfScreens to set
   */
  public void setMaximumNumberOfScreens(Integer maximumNumberOfScreens) {
    this.maximumNumberOfScreens = maximumNumberOfScreens;
  }

  /**
   * @param printWidgets the printWidgets to set
   */
  public void setPrintWidgets(boolean printWidgets) {
    this.printWidgets = printWidgets;
  }

  /**
   * @param printBuild the printBuild to set
   */
  public void setPrintBuild(boolean printBuild) {
    this.printBuild = printBuild;
  }

  /**
   * @return the printWidgets
   */
  public boolean isPrintWidgets() {
    return printWidgets;
  }
}
