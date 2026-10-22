package me.modmuss50.dg.neoforge.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

/** Maps exterior scenery around, rather than into, the independently built room. */
final class GlobeRenderTransform {
    private GlobeRenderTransform() {}

    static AABB renderBounds(BlockPos exit, boolean inner) {
        if (!inner) return new AABB(exit);
        Transform transform = forInterior();
        // Include scenery, snapshot entities (local 0..17), and the floor. Add
        // one scaled block as a margin for geometry extending past block faces.
        // NeoForge frustum-tests even the global/off-screen BER list, so the
        // default unit cube would make the scene vanish when looking away from
        // the floor exit, despite the camera still being inside the room.
        return new AABB(exit.getX() + transform.x(0), exit.getY() + transform.y(0),
                exit.getZ() + transform.z(0), exit.getX() + transform.x(17),
                exit.getY() + transform.y(17), exit.getZ() + transform.z(17)).inflate(transform.scale());
    }

    static Transform forInterior() {
        // Snapshot local (8,8,8) is the exterior globe block. Its lower corner
        // maps to room (0.5,0,0.5), relative to the floor exit at (8,0,8).
        // Each exterior block is 16 interior blocks wide. The central block is
        // omitted by the renderer, leaving the buildable volume unobstructed.
        return new Transform(16.0f, -135.5, -128.0, -135.5);
    }

    record Transform(float scale, double offsetX, double offsetY, double offsetZ) {
        double x(double sourceX) { return offsetX + scale * sourceX; }
        double y(double sourceY) { return offsetY + scale * sourceY; }
        double z(double sourceZ) { return offsetZ + scale * sourceZ; }
    }
}
