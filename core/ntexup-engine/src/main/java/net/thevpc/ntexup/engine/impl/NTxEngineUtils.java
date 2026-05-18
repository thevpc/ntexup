package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.nuts.io.NPath;

public class NTxEngineUtils {
    public static final String NTEXUP_EXT_NAME = NTxEngine.FILE_EXT;
    public static final String NTEXUP_EXT = NTxEngine.FILE_DOT_EXT;
    public static final String NTEXUP_EXT_STAR = "*."+NTxEngine.FILE_EXT;
    public static final String NTEXUP_EXT_STAR_STAR = "**/*."+NTxEngine.FILE_EXT;

    public static boolean isNTexupFile(NPath path) {
        return path != null && isNTexupFile(path.name());
    }

    public static boolean isNTexupFile(String name) {
        if (name != null) {
            return name.toLowerCase().endsWith(NTEXUP_EXT);
        }
        return false;
    }

    public static int comparePaths(NPath o1, NPath o2) {
        String a = o1.toString();
        String b = o2.toString();
        if (isNTexupFile(a) && isNTexupFile(b)) {
            a = a.substring(0, a.length() - NTEXUP_EXT.length());
            b = b.substring(0, b.length() - NTEXUP_EXT.length());
        }
        return a.compareTo(b);
    }
}
