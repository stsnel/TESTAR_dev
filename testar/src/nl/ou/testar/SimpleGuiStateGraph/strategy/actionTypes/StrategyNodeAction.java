package nl.ou.testar.SimpleGuiStateGraph.strategy.actionTypes;

import nl.ou.testar.SimpleGuiStateGraph.strategy.StrategyGuiState;
import nl.ou.testar.SimpleGuiStateGraph.strategy.StrategyNode;
import org.fruit.alayer.Action;

import java.util.ArrayList;
import java.util.Optional;


public abstract class StrategyNodeAction extends StrategyNode {
    public StrategyNodeAction(ArrayList<StrategyNode> children) {
        super(children);

    }

    public abstract Optional<Action> getAction(final StrategyGuiState state);
}