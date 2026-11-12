package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.plot2d.expr.NTxPlotNExprResolver;
import net.thevpc.ntexup.extension.plot2d.model.NTxFunctionPlotInfo;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.math.NDoubleFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class TestPlotExpressionCache {

    @BeforeAll
    static void initializeNuts() {
        Nuts.require();
    }

    @Test
    void cachesCompiledFunctionUntilExpressionChanges() {
        NTxFunctionPlotInfo plot = new NTxFunctionPlotInfo();
        plot.fexpr = NElement.ofString("sin(x)");
        plot.var1 = "x";
        plot.args = 1;
        NTxRendererContext rendererContext = (NTxRendererContext) Proxy.newProxyInstance(
                NTxRendererContext.class.getClassLoader(),
                new Class<?>[]{NTxRendererContext.class},
                (proxy, method, args) -> null);

        NDoubleFunction first = NTxPlotNExprResolver.compileFunctionX(plot, rendererContext);
        NDoubleFunction second = NTxPlotNExprResolver.compileFunctionX(plot, rendererContext);

        assertNotSame(first, second);
        NTxFunctionPlotInfo.CompiledExpression compiled =
                plot.getCompiledFunction(plot.fexpr);
        assertSame(compiled, plot.getCompiledFunction(plot.fexpr));
        assertEquals(Math.sin(1), first.apply(1), 1e-10);

        plot.fexpr = NElement.ofString("x*x");
        NDoubleFunction changed = NTxPlotNExprResolver.compileFunctionX(plot, rendererContext);

        assertNotSame(compiled, plot.getCompiledFunction(plot.fexpr));
        assertEquals(4, changed.apply(2), 1e-10);
    }
}
