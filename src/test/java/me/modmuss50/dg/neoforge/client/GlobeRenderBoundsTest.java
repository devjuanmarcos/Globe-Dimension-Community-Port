package me.modmuss50.dg.neoforge.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobeRenderBoundsTest {
    @Test
    void interiorRemainsVisibleAcrossCameraAnglesPositionsAndFovs() {
        for (int id : new int[]{0, 9, 18749}) {
            BlockPos exit = new BlockPos(8, 128, id * 1600 + 8);
            AABB bounds = GlobeRenderTransform.renderBounds(exit, true);
            for (int fov : new int[]{70, 110}) {
                for (int yaw = 0; yaw < 360; yaw += 15) {
                    for (int pitch : new int[]{-89, -45, 0, 45, 89}) {
                        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov),
                                16.0f / 9.0f, 0.05f, 512.0f);
                        Matrix4f view = new Matrix4f().rotateX((float) Math.toRadians(pitch))
                                .rotateY((float) Math.toRadians(yaw));
                        Frustum frustum = new Frustum(view, projection);
                        for (double[] position : new double[][]{
                                {0.5, 2.62, 0.5}, {-6.5, 2.62, -6.5}, {7.5, 2.62, 7.5},
                                {-6.5, 15.5, 7.5}, {7.5, 15.5, -6.5}}) {
                            frustum.prepare(exit.getX() + position[0], exit.getY() + position[1],
                                    exit.getZ() + position[2]);
                            assertTrue(frustum.isVisible(bounds), "Interior culled: globe=" + id
                                    + ", yaw=" + yaw + ", pitch=" + pitch + ", fov=" + fov);
                        }
                    }
                }
            }
        }
    }

    @Test
    void boundsEncloseGiantSceneryAndPedestal() {
        BlockPos exit = new BlockPos(8, 128, 14408);
        AABB bounds = GlobeRenderTransform.renderBounds(exit, true);
        var transform = GlobeRenderTransform.forInterior();
        for (int x : new int[]{0, 17}) {
            for (int y : new int[]{0, 17}) {
                for (int z : new int[]{0, 17}) {
                    assertTrue(bounds.contains(exit.getX() + transform.x(x),
                            exit.getY() + transform.y(y), exit.getZ() + transform.z(z)));
                }
            }
        }
        assertTrue(bounds.contains(exit.getX() - 8, exit.getY(), exit.getZ() - 8));
        assertTrue(bounds.contains(exit.getX() + 9, exit.getY() + 1, exit.getZ() + 9));
        assertFalse(bounds.isInfinite());
    }

    @Test
    void placedExteriorGlobeKeepsItsSingleBlockBounds() {
        BlockPos pos = new BlockPos(100, 80, -90);
        assertEquals(new AABB(pos), GlobeRenderTransform.renderBounds(pos, false));
    }
}
