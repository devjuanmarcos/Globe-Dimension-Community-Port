package me.modmuss50.dg.neoforge.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobeRenderTransformTest {
    @Test
    void exteriorGlobeBlockCorrespondsToTheWholeConstructionRoom() {
        var transform = GlobeRenderTransform.forInterior();
        // Exit is (8,0,8) relative to the physical room's origin.
        assertEquals(0.5, 8.0 + transform.x(8.0));
        assertEquals(0.0, transform.y(8.0));
        assertEquals(0.5, 8.0 + transform.z(8.0));
        assertEquals(16.5, 8.0 + transform.x(9.0));
        assertEquals(16.0, transform.y(9.0));
        assertEquals(16.5, 8.0 + transform.z(9.0));
    }

    @Test
    void neighboringExteriorBlocksStayBeyondTheRoomsBuildableBoundaries() {
        var transform = GlobeRenderTransform.forInterior();
        assertEquals(0.5, 8.0 + transform.x(7.0) + transform.scale());
        assertEquals(16.5, 8.0 + transform.x(9.0));
        assertEquals(0.0, transform.y(7.0) + transform.scale());
        assertEquals(16.0, transform.y(9.0));
        assertEquals(0.5, 8.0 + transform.z(7.0) + transform.scale());
        assertEquals(16.5, 8.0 + transform.z(9.0));
    }
}
