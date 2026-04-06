/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.parser.NTxParseMode;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElementAnnotation;
import net.thevpc.nuts.util.NNameFormat;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.elem.NElement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.thevpc.nuts.util.NBlankable;

/**
 * @author vpc
 */
public class NTxParseHelper {

    public static String toString(NElement e, NTxParseMode mode) {
        if (e.isAnyString()) {
            return e.asStringValue().get();
        }
        switch (mode) {
            case ERROR: {
                throw new IllegalArgumentException("expected string. got " + e);
            }
            case BEST_ERROR: {
                return e.toString();
            }
            case NULL: {
                return null;
            }
        }
        throw new IllegalArgumentException("expected string. got " + e);
    }

    public static boolean fillAnnotations(NElement e, NTxNode p) {
        boolean some = false;
        Set<String> allClasses = new HashSet<>();
        for (NElementAnnotation a : e.annotations()) {
            String nn = a.name();
            if (NBlankable.isBlank(nn)) {
                // add classes as well
                List<NElement> params = a.params().orNull();
                if (params != null) {
                    for (NElement cls : params) {
                        NOptional<String[]> ss = NTxValue.of(cls).asStringArrayOrString();
                        if (ss.isPresent()) {
                            allClasses.addAll(Arrays.asList(ss.get()));
                        }
                    }
                }
            }else if(!a.isParametrized()){
                // name only
                allClasses.add(nn);
            }
        }
        if (!allClasses.isEmpty()) {
            p.addStyleClasses(allClasses.toArray(new String[0]));
            some = true;
        }
        return some;
    }
}
