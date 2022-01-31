package nl.ou.testar.parser;

import org.fruit.Pair;
import org.fruit.alayer.Action;
import org.fruit.alayer.actions.Type;
import org.fruit.alayer.actions.Wait;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaitParser implements IActionParser {

    private final static String TEMPLATE = "Wait\\s+for\\s+(exactly\\s+)?([+-]?\\d*\\.?\\d+)\\s*(.*)";

    @Override
    public Pair<Action, String> parse(String src) {
        final Pattern pattern = Pattern.compile(TEMPLATE, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(src);
        int groupCount = matcher.groupCount();
        if (groupCount > 2) {
            boolean isExact = (groupCount > 3);
            final String secondsString = matcher.group(isExact ? 2 : 1);
            final String rest = matcher.group(isExact ? 3: 2);
            return new Pair<>(new Wait(Double.parseDouble(secondsString), isExact), rest);
        }
        return null;
    }
}
