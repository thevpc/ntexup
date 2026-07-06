package net.thevpc.ntexup.engine.util;

import net.thevpc.nuts.util.NStringUtils;

import java.util.ArrayList;
import java.util.List;

public class NTxUtilsText {
    public static String trimBloc(String code) {
        List<String> rows = new ArrayList<>();
        for (String string : code.split("[\n\r]")) {
            if (rows.isEmpty()) {
                if (string.trim().isEmpty()) {
                    //just ignore
                } else {
                    rows.add(string);
                }
            } else {
                rows.add(string);
            }
        }
        for (int i = rows.size() - 1; i >= 0; i--) {
            String r = rows.get(i);
            if (r.trim().isEmpty()) {
                rows.remove(i);
            } else {
                break;
            }
        }

        int startingSpaces = -1;
        for (int i = rows.size() - 1; i >= 0; i--) {
            String r = rows.get(i);
            if (r.trim().isEmpty()) {
                rows.set(i,"");
            } else {
                int s = computeStartingSpaces(r);
                if (startingSpaces < 0 || startingSpaces > s) {
                    startingSpaces = s;
                }
            }
        }
        if (startingSpaces > 0) {
            for (int i = 0; i < rows.size(); i++) {
                String r = rows.get(i);
                if(startingSpaces>=r.length()){

                }else {
                    rows.set(i, r.substring(startingSpaces));
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            String r = rows.get(i);
            sb.append(NStringUtils.stripRight(r));
            if (i + 1 < rows.size()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static int computeStartingSpaces(String code) {
        int x = 0;
        for (int i = 0; i < code.length(); i++) {
            char s = code.charAt(i);
            if (s == ' ') {
                x++;
            } else {
                break;
            }
        }
        return x;
    }

}
