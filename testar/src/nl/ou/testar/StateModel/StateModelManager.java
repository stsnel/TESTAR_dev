package nl.ou.testar.StateModel;

import org.fruit.alayer.Action;
import org.fruit.alayer.State;

import java.util.Set;

public interface StateModelManager {
    void notifyNewStateReached(State newState, Set<Action> actions);

    void notifyActionExecution(Action action);

    void notifySequenceEnded();

    Action getAbstractActionToExecute(Set<Action> actions);

    void notifyTestSequencedStarted();

    void notifyTestSequenceStopped();
}