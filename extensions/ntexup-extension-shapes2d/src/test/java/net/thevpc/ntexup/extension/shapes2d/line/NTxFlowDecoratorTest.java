package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NTxFlowDecoratorTest {

    @Test
    void staticPhaseSpacesDotsEvenly() {
        assertEquals(0.0, NTxFlowDecorator.phase(0, 4, 0, 3, false), 1e-9);
        assertEquals(1.0 / 3, NTxFlowDecorator.phase(0, 4, 1, 3, false), 1e-9);
        assertEquals(2.0 / 3, NTxFlowDecorator.phase(0, 4, 2, 3, false), 1e-9);
    }

    @Test
    void reverseMirrorsPositions() {
        assertEquals(1.0, NTxFlowDecorator.phase(0, 4, 0, 3, true), 1e-9);
        assertEquals(2.0 / 3, NTxFlowDecorator.phase(0, 4, 1, 3, true), 1e-9);
        assertEquals(1.0 / 3, NTxFlowDecorator.phase(0, 4, 2, 3, true), 1e-9);
    }

    @Test
    void animationWrapsWithinPeriod() {
        assertEquals(0.5, NTxFlowDecorator.phase(2, 4, 0, 2, false), 1e-9);
        // second dot: 0.5 + 0.5 = 1.0 -> wraps to 0
        assertEquals(0.0, NTxFlowDecorator.phase(2, 4, 1, 2, false), 1e-9);
        assertEquals(0.25, NTxFlowDecorator.phase(9, 4, 0, 4, false), 1e-9);
    }

    @Test
    void zeroPeriodAndCountAreSafe() {
        assertEquals(0.0, NTxFlowDecorator.phase(0, 0, 0, 0, false), 1e-9);
    }

    @Test
    void fadeVanishesAtTheAnchorsAndHoldsInBetween() {
        assertEquals(0.0, NTxFlowDecorator.fadeAlpha(0, NTxFlowDecorator.FADE_BAND), 1e-9);
        assertEquals(0.0, NTxFlowDecorator.fadeAlpha(1, NTxFlowDecorator.FADE_BAND), 1e-9);
        assertEquals(0.5, NTxFlowDecorator.fadeAlpha(NTxFlowDecorator.FADE_BAND / 2, NTxFlowDecorator.FADE_BAND), 1e-9);
        assertEquals(1.0, NTxFlowDecorator.fadeAlpha(0.5, NTxFlowDecorator.FADE_BAND), 1e-9);
        assertEquals(1.0, NTxFlowDecorator.fadeAlpha(1 - NTxFlowDecorator.FADE_BAND, NTxFlowDecorator.FADE_BAND), 1e-9);
    }

    @Test
    void zeroFadeBandKeepsDotsOpaque() {
        assertEquals(1.0, NTxFlowDecorator.fadeAlpha(0.0, 0), 1e-9);
        assertEquals(1.0, NTxFlowDecorator.fadeAlpha(0.5, -1), 1e-9);
    }

    @Test
    void periodWinsWhenNoSpeedGiven() {
        assertEquals(4.0, NTxFlowDecorator.traversalPeriod(4, 0, 1163), 1e-9);
    }

    @Test
    void speedMakesCrossingTimeGrowWithLength() {
        // a 25 unit connector at 20 units per second takes 1.25s, a 50 unit one 2.5s.
        // Same apparent rate on both, which is the point of the parameter.
        assertEquals(1.25, NTxFlowDecorator.traversalPeriod(4, 20, 25), 1e-9);
        assertEquals(2.50, NTxFlowDecorator.traversalPeriod(4, 20, 50), 1e-9);
    }

    @Test
    void speedNeverProducesZeroPeriod() {
        assertEquals(0.05, NTxFlowDecorator.traversalPeriod(4, 1e9, 1e-9), 1e-9);
    }

    @Test
    void emptyPathHasNoLengthInUnits() {
        // A zero-length route must not divide by the speed; period stays the fallback.
        assertEquals(0.0, NTxFlowDecorator.lengthInPageUnits(
                new NTxPolylineRoutedPath(java.util.List.of(new NTxPoint2D(3, 4))), 10, 5), 1e-9);
        assertEquals(4.0, NTxFlowDecorator.traversalPeriod(4, 20, 0), 1e-9);
    }

    @Test
    void anisotropicAxesConvertEachSegmentOnItsOwnScale() {
        // 20 px wide and 10 px tall over 10 units and 5 units: the axes differ 2:1, so a
        // single averaged rate would put a horizontal run at the wrong speed.
        NTxRoutedPath horizontal = new NTxPolylineRoutedPath(
                java.util.List.of(new NTxPoint2D(0, 0), new NTxPoint2D(20, 0)));
        assertEquals(1.0, NTxFlowDecorator.lengthInPageUnits(horizontal, 20, 10), 1e-9);
        NTxRoutedPath vertical = new NTxPolylineRoutedPath(
                java.util.List.of(new NTxPoint2D(0, 0), new NTxPoint2D(0, 10)));
        assertEquals(1.0, NTxFlowDecorator.lengthInPageUnits(vertical, 20, 10), 1e-9);
        NTxRoutedPath dogleg = new NTxPolylineRoutedPath(java.util.List.of(
                new NTxPoint2D(0, 0), new NTxPoint2D(20, 0), new NTxPoint2D(20, 10)));
        assertEquals(2.0, NTxFlowDecorator.lengthInPageUnits(dogleg, 20, 10), 1e-9);
    }

    @Test
    void trailStartsAtTheHeadAndEndsBlunt() {
        assertEquals(10.0, NTxFlowDecorator.trailHalfWidth(10, 0), 1e-9);
        assertEquals(10 * NTxFlowDecorator.TAIL_END_RATIO, NTxFlowDecorator.trailHalfWidth(10, 1), 1e-9);
        // convex taper: the midpoint is thinner than the linear mid-value
        assertTrue(NTxFlowDecorator.trailHalfWidth(10, 0.5) < 5.0);
    }

    @Test
    void taperIsMonotonic() {
        double previous = Double.POSITIVE_INFINITY;
        for (int k = 0; k <= NTxFlowDecorator.TAIL_STEPS; k++) {
            double w = NTxFlowDecorator.trailHalfWidth(10, (double) k / NTxFlowDecorator.TAIL_STEPS);
            assertTrue(w <= previous, "trail widens at step " + k);
            previous = w;
        }
    }
}
