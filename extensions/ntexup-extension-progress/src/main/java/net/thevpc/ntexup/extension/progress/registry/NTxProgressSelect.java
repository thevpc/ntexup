package net.thevpc.ntexup.extension.progress.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Filter for selecting bindings from the registry.
 */
public class NTxProgressSelect {

    public enum Mode {
        ALL,
        NAMES,
        PATTERN
    }

    private final Mode mode;
    private final List<String> names;
    private final Pattern pattern;

    private NTxProgressSelect(Mode mode, List<String> names, Pattern pattern) {
        this.mode = mode;
        this.names = names;
        this.pattern = pattern;
    }

    public static NTxProgressSelect all() {
        return new NTxProgressSelect(Mode.ALL, null, null);
    }

    public static NTxProgressSelect names(List<String> names) {
        return new NTxProgressSelect(Mode.NAMES, new ArrayList<>(names), null);
    }

    public static NTxProgressSelect pattern(String regex) {
        return new NTxProgressSelect(Mode.PATTERN, null, Pattern.compile(regex));
    }

    public Mode mode() {
        return mode;
    }

    public List<String> names() {
        return names;
    }

    public Pattern pattern() {
        return pattern;
    }

    public boolean matches(String bindingName) {
        switch (mode) {
            case ALL:
                return true;
            case NAMES:
                return names.contains(bindingName);
            case PATTERN:
                return pattern.matcher(bindingName).matches();
            default:
                return false;
        }
    }
}
