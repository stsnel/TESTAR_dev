package nl.ou.testar.parser;

import org.fruit.Pair;
import org.fruit.alayer.Action;
import org.fruit.alayer.actions.KeyDown;
import org.fruit.alayer.actions.MouseUp;
import org.fruit.alayer.devices.KBKeys;
import org.fruit.alayer.devices.MouseButtons;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeyDownParser implements IActionParser {

    private final static String TEMPLATE = "Press\\s+Key\\s+(\\w+)\\s*(.*)";

    @Override
    public Pair<Action, String> parse(String src) {
        final Pattern pattern = Pattern.compile(TEMPLATE, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(src);
        if (matcher.groupCount() > 2) {
            final String keyName = matcher.group(1);
            final String rest = matcher.group(2);
            return new Pair<>(new KeyDown(KBKeys.valueOf(keyName)), rest);
        }
        return null;
    }
}
