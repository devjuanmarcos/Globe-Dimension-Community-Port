package me.modmuss50.dg.neoforge.world;

/** Coordinate rules for the legacy globe frame and its captured contents. */
public final class GlobeRegion {
    public static final int FRAME_SIZE = 17;
    public static final int BLOCK_INTERIOR_MIN = 1;
    public static final int BLOCK_INTERIOR_MAX = FRAME_SIZE - 2;

    private GlobeRegion() {}

    public static boolean isInteriorBlock(int x, int y, int z) {
        return isInteriorBlockCoordinate(x)
                && isInteriorBlockCoordinate(y)
                && isInteriorBlockCoordinate(z);
    }

    public static boolean isInEntityCapture(double x, double y, double z) {
        return isInEntityCaptureCoordinate(x)
                && isInEntityCaptureCoordinate(y)
                && isInEntityCaptureCoordinate(z);
    }

    private static boolean isInteriorBlockCoordinate(int coordinate) {
        return coordinate >= BLOCK_INTERIOR_MIN && coordinate <= BLOCK_INTERIOR_MAX;
    }

    private static boolean isInEntityCaptureCoordinate(double coordinate) {
        return coordinate >= 0 && coordinate < FRAME_SIZE;
    }
}
