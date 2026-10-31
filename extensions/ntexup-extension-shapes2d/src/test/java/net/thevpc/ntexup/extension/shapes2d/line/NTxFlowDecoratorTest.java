package net.thevpc.ntexup.extension.shapes2d.line;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
