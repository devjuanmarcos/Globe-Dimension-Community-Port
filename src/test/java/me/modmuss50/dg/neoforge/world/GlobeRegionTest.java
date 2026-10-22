package me.modmuss50.dg.neoforge.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobeRegionTest {
    @Test
    void blockInteriorExcludesTheOneCellFrameAndIncludesBothInteriorEdges() {
        assertFalse(GlobeRegion.isInteriorBlock(0, 1, 1));
        assertTrue(GlobeRegion.isInteriorBlock(1, 1, 1));
        assertTrue(GlobeRegion.isInteriorBlock(15, 15, 15));
        assertFalse(GlobeRegion.isInteriorBlock(16, 15, 15));
    }

    @Test
    void entityCaptureIncludesTheFullSeventeenBlockFrame() {
        assertTrue(GlobeRegion.isInEntityCapture(0, 0, 0));
        assertTrue(GlobeRegion.isInEntityCapture(16.999, 16.999, 16.999));
        assertFalse(GlobeRegion.isInEntityCapture(17, 8, 8));
        assertFalse(GlobeRegion.isInEntityCapture(-0.001, 8, 8));
    }
}
