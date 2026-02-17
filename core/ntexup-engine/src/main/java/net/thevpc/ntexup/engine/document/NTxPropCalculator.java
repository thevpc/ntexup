package net.thevpc.ntexup.engine.document;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.*;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.util.NOptional;

import java.util.*;
import java.util.stream.Collectors;

public class NTxPropCalculator {


    public NOptional<NTxProp> computeProperty(NTxNode node, String[] propertyNames) {
        return computePropertyMagnitude(node, propertyNames).map(NTxStyleAndMagnitude::getStyle);
    }

    private static class HStyleRuleResult2 {
        NTxStyleRule rule;
        NTxProp property;
        int distance;

        public HStyleRuleResult2(NTxStyleRule rule, NTxProp property) {
            this.rule = rule;
            this.property = property;
        }
        public HStyleRuleResult2(NTxStyleRule rule, NTxProp property, int distance) {
            this.rule = rule;
            this.property = property;
            this.distance = distance;
        }
        public HStyleRuleResult2(HStyleRuleResult2 other,int distance) {
            this.rule = other.rule;
            this.property = other.property;
            this.distance = distance;
        }

        @Override
        public String toString() {
            return "HStyleRuleResult2{" +
                    "rule=" + rule +
                    ", property=" + property +
                    '}';
        }
    }

    private HStyleRuleResult2[] _HStyleRuleResult2s(NTxNode t, NTxNode p) {
        NTxStyleRule[] rules = p.rules();
        List<HStyleRuleResult2> rr = new ArrayList<>();
        for (NTxStyleRule rule : rules) {
            if (rule.acceptNode(t)) {
                for (NTxProp style : rule.styles().toList()) {
                    rr.add(new HStyleRuleResult2(rule, style));
                }
            }
        }
        return rr.toArray(new HStyleRuleResult2[0]);
    }

    private HStyleRuleResult2[] _HStyleRuleResult2(NTxNode t, NTxNode p, String[] propertyNames) {
        NTxStyleRule[] rules = p.rules();
        List<HStyleRuleResult2> rr = new ArrayList<>();
        for (NTxStyleRule rule : rules) {
            if (rule.acceptNode(t)) {
                NOptional<NTxProp> ok = rule.styles().get(propertyNames);
                if (ok.isPresent()) {
                    rr.add(new HStyleRuleResult2(rule, ok.get()));
//                    break;
                }
            }
        }
        return rr.toArray(new HStyleRuleResult2[0]);
    }

    public NOptional<NTxStyleAndMagnitude> computePropertyMagnitude(NTxNode node, String[] propertyNames) {
        propertyNames = NTxUtils.uids(propertyNames);
        NOptional<NTxProp> u = node.getProperty(propertyNames);
        if (u.isPresent()) {
            return NOptional.of(
                    new NTxStyleAndMagnitude(
                            u.get(),
                            new NTxStyleMagnitude(0, DefaultNTxNodeSelector.ofAny())
                    )
            );
        }
        NTxNode p = NTxUtils.firstNodeUp(node.parent());
        int distance = 1;
        NTxProp bestStyle = null;
        NTxStyleMagnitude bestMag = null;
        List<HStyleRuleResult2> acceptable=new ArrayList<>();
        while (p != null) {
            HStyleRuleResult2[] validRules = _HStyleRuleResult2(node, p, propertyNames);
            for (HStyleRuleResult2 rule : validRules) {
                NTxStyleMagnitude m2 = new NTxStyleMagnitude(distance, rule.rule.selector());
                acceptable.add(new HStyleRuleResult2(rule,distance));
                if (bestMag == null || m2.compareTo(bestMag) < 0) {
                    bestMag = m2;
                    bestStyle = rule.property;
                }
            }
            /*
            if (bestMag != null) {
                return NOptional.of(
                        new HStyleAndMagnitude(
                                bestStyle,
                                new HStyleMagnitude(distance, bestMag.getSelector())
                        )
                );
            }
            */
            distance++;
            p = NTxUtils.firstNodeUp(p.parent());
        }
        if (bestMag != null) {
            return NOptional.of(
                    new NTxStyleAndMagnitude(
                            bestStyle,
                            new NTxStyleMagnitude(distance, bestMag.getSelector())
                    )
            );
        }
        return NOptional.ofNamedEmpty("no style : " + Arrays.asList(propertyNames));
    }

    public List<NTxStyleRule> computeStyles(NTxNode node) {
        List<NTxStyleRule> found = new ArrayList<>();
        NTxNode p = NTxUtils.firstNodeUp(node.parent());
        while (p != null) {
            NTxStyleRule[] rules = p.rules();
            for (NTxStyleRule rule : rules) {
                if (rule.acceptNode(node)) {
                    found.add(rule);
                }
            }
            p = NTxUtils.firstNodeUp(p.parent());
        }
        return found;
    }

    public List<NTxStyleRule> computeDeclaredStyles(NTxNode node) {
        List<NTxStyleRule> found = new ArrayList<>();
        NTxNode p = NTxUtils.firstNodeUp(node.parent());
        while (p != null) {
            NTxStyleRule[] rules = p.rules();
            for (NTxStyleRule rule : rules) {
                found.add(rule);
            }
            p = NTxUtils.firstNodeUp(p.parent());
        }
        return found;
    }
    public List<NTxStyleAndMagnitude> computePropertiesMagnitude(NTxNode node) {
        Map<String, NTxStyleAndMagnitude> found = new LinkedHashMap<>();
        for (NTxProp property : node.getProperties()) {
            found.put(property.getName(),
                    new NTxStyleAndMagnitude(
                            property,
                            new NTxStyleMagnitude(0, DefaultNTxNodeSelector.ofAny())
                    )
            );
        }
        NTxNode p = NTxUtils.firstNodeUp(node.parent());
        int distance = 1;
        while (p != null) {
            HStyleRuleResult2[] validRules = _HStyleRuleResult2s(node, p);
            for (HStyleRuleResult2 rule : validRules) {
                NTxStyleAndMagnitude m2 = new NTxStyleAndMagnitude(rule.property, new NTxStyleMagnitude(distance, rule.rule.selector()));
                NTxStyleAndMagnitude hStyleAndMagnitude = found.get(rule.property.getName());
                if (hStyleAndMagnitude == null || m2.getMagnetude().compareTo(hStyleAndMagnitude.getMagnetude()) <= 0) {
                    found.put(rule.property.getName(), m2);
                }
            }
            distance++;
            p = NTxUtils.firstNodeUp(p.parent());
        }
        return new ArrayList<>(found.values());
    }

    public List<NTxProp> computeProperties(NTxNode node) {
        return computePropertiesMagnitude(node).stream().map(x -> x.getStyle()).collect(Collectors.toList());
    }

    public List<NTxProp> computeInheritedProperties(NTxNode node) {
        return computePropertiesMagnitude(node).stream()
                .filter(x -> x.getMagnetude().getDistance() > 0)
                .map(x -> x.getStyle()).collect(Collectors.toList());
    }

    public <T> NOptional<T> computePropertyValue(NTxNode t, String... s) {
        if (t != null) {
            return computeProperty(t, s).map(NTxProp::getValue).map(x -> {
                try {
                    return (T) x;
                } catch (ClassCastException e) {
                    return null;
                }
            }).filter(x -> x != null);
        }
        return NOptional.ofNamedEmpty("style " + Arrays.asList(s));
    }
}
