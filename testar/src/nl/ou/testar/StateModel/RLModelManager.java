package nl.ou.testar.StateModel;

import nl.ou.testar.ReinforcementLearning.QFunctions.QFunction;
import nl.ou.testar.ReinforcementLearning.RLTags;
import nl.ou.testar.ReinforcementLearning.RewardFunctions.RewardFunction;
import nl.ou.testar.StateModel.ActionSelection.ActionSelector;
import nl.ou.testar.StateModel.Exception.ActionNotFoundException;
import nl.ou.testar.StateModel.Persistence.PersistenceManager;
import nl.ou.testar.StateModel.Sequence.SequenceManager;
import org.fruit.alayer.Action;
import org.fruit.alayer.State;
import org.fruit.alayer.Tag;
import org.fruit.alayer.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the {@link StateModelManager} for use of Sarsa.
 * Sarsa is a reinforcement learning (Artificial Intelligence) algorithm
 * for (sequential) action selection.
 */
public class RLModelManager extends ModelManager implements StateModelManager {

    protected static final Logger logger = LoggerFactory.getLogger(RLModelManager.class);

    /** The previously executed {@link AbstractAction} */
    protected AbstractAction previouslyExecutedAbstractAction = null;
    protected Action previouslyExecutedTestarAction = null;

    /**  The {@Link RewardFunction} determines the reward or penalty for executing an {@link AbstractAction}
    *  The reward is used in the {@link QFunction}
    */
    protected final RewardFunction rewardFunction;

    /**
     * The {@link QFunction} or Quality function determines the desirability of an {@link AbstractAction}
     */
    protected final QFunction qFunction;

    protected State state = null;

    protected final Tag<?> tag;
    
    protected AbstractState previousAbstractState = null;
    
    protected Set<Action> previousTestarActions;

    //*** FOR DEBUGGING PURPOSES
    List<Float> qValuesList = new ArrayList<Float>();
    //*** FOR DEBUGGING PURPOSES

    /**
     * Constructor
     *
     */
    public RLModelManager(final AbstractStateModel abstractStateModel,
                             final ActionSelector actionSelector,
                             final PersistenceManager persistenceManager,
                             final Set<Tag<?>> concreteStateTags,
                             final SequenceManager sequenceManager,
                             final boolean storeWidgets,
                             final RewardFunction rewardFunction,
                             final QFunction qFunction,
                             final Tag<?> tag) {
        super(abstractStateModel, actionSelector, persistenceManager, concreteStateTags, sequenceManager, storeWidgets);
        this.rewardFunction = rewardFunction;
        this.qFunction = qFunction;
        this.tag = tag;
    }

    @Override
    public void notifyNewStateReached(final State newState, final Set<Action> actions) {
        super.notifyNewStateReached(newState, actions);
        state = newState;

    }

    /**
     * Gets an {@link Action} to execute and updates the Q-value of the previously executed {@link Action}
     */
    @Override
    public Action getAbstractActionToExecute(final Set<Action> actions) {
        logger.info("Number of actions available:{}", actions.size());
        final Action selectedAction = super.getAbstractActionToExecute(actions);
        logger.info("Action selected:{}", selectedAction == null ? null :selectedAction.toShortString());
        return selectedAction;
    }

    /**
     * Update the Q-value for an {@link Action}
     *
     * @param selectedAbstractAction, can be null
     */
    protected void updateQValue(final AbstractAction selectedAbstractAction, final Set<Action> actions) {    
        // get reward and Q-value
        System.out.println("UpdateQValue RLModelManager");
        float reward = rewardFunction.getReward(state, getCurrentConcreteState(), currentAbstractState, previouslyExecutedTestarAction, previouslyExecutedAbstractAction, selectedAbstractAction, actions);
        System.out.println("REWARD: " + Float.toString(reward));
        final float qValue = qFunction.getQValue((Tag<Float>)this.tag, previouslyExecutedAbstractAction, selectedAbstractAction, reward, currentAbstractState, actions);

        // set attribute for saving in the graph database
        if(previouslyExecutedAbstractAction != null) {
            previouslyExecutedAbstractAction.addAttribute(tag, qValue);
            System.out.println("qFunction.getClass().getName(): " + qFunction.getClass().getName());
            if (qFunction.getClass().getName() == "QBorjaFunction2") equalizeQValues(qValue);

            //*** FOR DEBUGGING PURPOSES
            float lastQValue = previouslyExecutedAbstractAction.getAttributes().get((Tag<Float>) this.tag);
            qValuesList.add(lastQValue);
            System.out.println("qValuesList: " + qValuesList);
            //*** FOR DEBUGGING PURPOSES

        }
        
        //*** FOR DEBUGGING PURPOSES
        if (previousAbstractState != null) {
			System.out.println(". . . CURRENT ACTIONS:");
			for (Action a : previousTestarActions) {
				AbstractAction absAction;
				try {
					absAction = previousAbstractState.getAction(a.get(Tags.AbstractIDCustom, ""));
					System.out.println(a.get(Tags.OriginWidget).get(Tags.Desc) + ". QValue: "
							+ absAction.getAttributes().get(RLTags.QBorja, 0f));
				} catch (ActionNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
        }
        //*** FOR DEBUGGING PURPOSES

    }

    @Override
    public void notifyTestSequenceStopped() {
        super.notifyTestSequenceStopped();
        rewardFunction.reset();
    }
    
    public void equalizeQValues(float qValue) {
		// Update Q-value of actions of the same type and depth, with the new calculated
		// Q-value
		Action previousAction = null;
		
		for (Action a : previousTestarActions) {
			if (a.get(Tags.AbstractIDCustom).equals(previouslyExecutedAbstractAction.getActionId())) {
				previousAction = a; // Get the action to access the Role and ZIndex
				break;
			}
		}
		
		if(previousAction == null || previousAbstractState == null) {return;}
    	
		String previousActionType = previousAction.get(Tags.OriginWidget).get(Tags.Role).toString();
		double previousActionDepth = previousAction.get(Tags.OriginWidget).get(Tags.ZIndex);
		
		for(Action a : previousTestarActions) {
			String aType = a.get(Tags.OriginWidget).get(Tags.Role).toString();
			double aDepth = a.get(Tags.OriginWidget).get(Tags.ZIndex);
			if((previousActionType == aType) && (previousActionDepth == aDepth)) {
	            //a.set(RLTags.QBorja, qValue);
				try {
                    AbstractAction abstractAction = previousAbstractState.getAction(a.get(Tags.AbstractIDCustom, "Nothing"));
                    abstractAction.addAttribute(RLTags.QBorja, qValue);
                } catch (ActionNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
			}
		}
    }
}
