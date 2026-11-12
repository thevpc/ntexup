package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.document.style.DefaultNTxNodeSelector;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.document.style.NTxStyleRuleSelectorItem;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.Nuts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestStyleCascadeAllocations {

    @BeforeAll
    static void initializeNuts() {
        Nuts.require();
    }

    @Test
    void uidPreservesCanonicalNamesAndNormalizesOtherNames() throws IllegalAccessException {
        assertEquals("fill-color", NTxUtils.uid("Fill-Color"));
        assertEquals("fill-color", NTxUtils.uid("fill-color"));
        assertEquals(NTxUtils.uid("Fill-Color"), NTxUtils.uid(NTxUtils.uid("Fill-Color")));
        assertEquals("foo-bar", NTxUtils.uid(" foo_bar "));

        for (Field field : NTxPropName.class.getFields()) {
            if (field.getType() == String.class && Modifier.isStatic(field.getModifiers())) {
                String value = (String) field.get(null);
                assertEquals(value, NTxUtils.uid(value), field.getName());
            }
        }
    }

    @Test
    void uidsSingleNameMatchesCollectionBehavior() {
        assertEquals(List.of("fill-color"), List.of(NTxUtils.uids(new String[]{"Fill-Color"})));
        assertEquals(List.of("fill-color"), List.of(NTxUtils.uids(new String[]{"  Fill-Color  "})));
        assertEquals(List.of(), List.of(NTxUtils.uids(new String[]{"  "})));
        assertEquals(List.of(), List.of(NTxUtils.uids((String[]) null)));
    }

    @Test
    void propertyAliasesKeepLastMatchAndLazyMissMessage() {
        NTxProperties properties = new NTxProperties();
        properties.set(NTxProp.ofString("a", "first"));
        properties.set(NTxProp.ofString("b", "second"));

        assertEquals("second", properties.get("a", "b").get().getValue().asStringValue().get());
        assertEquals("first", properties.get("b", "a").get().getValue().asStringValue().get());
        assertTrue(properties.get("missing").isEmpty());
        assertEquals("style missing", properties.get("missing").message().get().toString());
        assertEquals("style [missing, absent]",
                properties.get("missing", "absent").message().get().toString());
    }

    @Test
    void classDefinitionNamesAreCachedAndImmutable() {
        NTxStyleRuleSelectorItem first = NTxStyleRuleSelectorItem.ofClassDef("first", List.of());
        NTxStyleRuleSelectorItem second = NTxStyleRuleSelectorItem.ofClassDef("second", List.of("base"));
        NTxStyleRuleSelectorItem type = NTxStyleRuleSelectorItem.of("text", null).get();
        DefaultNTxNodeSelector selector = DefaultNTxNodeSelector.of(first, second, type);

        Set<String> expected = new HashSet<>(Set.of("first", "second"));
        assertEquals(expected, selector.getClassDefNames());
        assertSame(selector.getClassDefNames(), selector.getClassDefNames());
        assertThrows(UnsupportedOperationException.class, () -> selector.getClassDefNames().add("other"));
        assertFalse(DefaultNTxNodeSelector.ofAny().getClassDefNames().contains("first"));
    }
}
