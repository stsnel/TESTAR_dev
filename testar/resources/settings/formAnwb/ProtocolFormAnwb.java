package formAnwb;

import nl.ou.testar.subroutine.FormProtocol;

/**
 * Class responsible for executing a FormProtocol with
 * a subroutine for completing a form on the ANWB site.
 *
 * @author Conny Hageluken
 * @Date January 2019
 */
public class ProtocolFormAnwb
    extends FormProtocol {

  /**
   * Constructor.
   * Including settings for print facilities
   * - Print additional information on widgets
   * - Set number of editable widgets is used as a criterion to define a form
   * - Set maximum number of screens a form consists of.
   */
  public ProtocolFormAnwb() {
    setPrintWidgets(false);
    setPrintBuild(true);
    setMinimumNumberOfEditWidgets(2);
    setMaximumNumberOfScreens(4);
  }
}
