package nl.ou.testar.SimpleGuiStateGraph.strategy.actions;

import nl.ou.testar.SimpleGuiStateGraph.strategy.StrategyGuiState;
import nl.ou.testar.SimpleGuiStateGraph.strategy.StrategyNode;
import nl.ou.testar.SimpleGuiStateGraph.strategy.actionTypes.StrategyNodeAction;
import org.fruit.alayer.Action;
import org.fruit.alayer.actions.AnnotatingActionCompiler;
import org.fruit.alayer.devices.KBKeys;

import java.util.ArrayList;
import java.util.Optional;

public class SnEscape extends StrategyNodeAction {

    public SnEscape(ArrayList<StrategyNode> children) {
        super(children);
    }

    @Override
    public Optional<Action> getAction(final StrategyGuiState state) {
        return Optional.of(new AnnotatingActionCompiler().hitKey(KBKeys.VK_ESCAPE));
    }

}