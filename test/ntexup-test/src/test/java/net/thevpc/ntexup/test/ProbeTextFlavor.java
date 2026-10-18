package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavorParseContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextToken;

import java.awt.Paint;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * A test-only inline text flavor, registered through
 * {@code META-INF/services/net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor}.
 * It has the same shape as the LaTeX equation flavor ({@code [[eq: ...]]} plus the
 * {@code \(...\)} inline separators) but records every {@link #buildText} call instead of
 * rasterizing an image, so tests can assert that a flavor region is resolved and that the
 * surrounding markup options (bold/italic/color) are applied to it.
 */
public class ProbeTextFlavor implements NTxTextRendererFlavor {

    public static final List<Call> CALLS = new CopyOnWriteArrayList<>();

    @Override
    public String type() {
        return "probe";
    }

    @Override
    public List<String> parsePrefixes() {
        return Arrays.asList("[[probe:", "\\(");
    }

    @Override
    public String parseSuffix(String prefix) {
        return "]]";
    }

    @Override
    public List<NTxTextToken> parseTokens(NTxTextRendererFlavorParseContext ctx) {
        return ctx.parseDefault(new String[]{"probe"}, new String[]{"\\(", "\\)"}, s -> s);
    }

    @Override
    public void buildText(String text, NTxTextOptions options, NTxRendererContext ctx, NTxTextRendererBuilder builder) {
        CALLS.add(new Call(text, options.bold, options.italic, options.foregroundColor));
    }

    /** The single call that rendered {@code value}, or {@code null} if the flavor was not invoked for it. */
    public static Call call(String value) {
        return CALLS.stream().filter(c -> value.equals(c.value())).findFirst().orElse(null);
    }

    public static boolean hasCall(String value) {
        return CALLS.stream().anyMatch(c -> value.equals(c.value()));
    }

    @Override
    public String toString() {
        return "ProbeTextFlavor" + CALLS.stream().map(Call::toString).collect(Collectors.toList());
    }

    /** One {@code buildText} invocation of the probe flavor. */
    public record Call(String value, Boolean bold, Boolean italic, Paint foregroundColor) {

        @Override
        public String toString() {
            return "{" + value + " bold=" + bold + " italic=" + italic + " fg=" + foregroundColor + "}";
        }
    }
}
