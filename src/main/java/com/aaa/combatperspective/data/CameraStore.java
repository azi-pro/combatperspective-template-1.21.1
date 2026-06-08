package com.aaa.combatperspective.data;

/** 摄像机状态：朝向、位置、球坐标 */
public class CameraStore {
    private static float cameraYaw;
    private static float cameraPitch;
    private static Object cameraTarget;

    private static double deltaCameraX;
    private static double deltaCameraY = 6;
    private static double deltaCameraZ = 6;

    private static double cameraSphYaw;
    private static double cameraSphPitch = 45;
    private static double cameraSphDist = 8.49;

    static { updateCartesian(); }

    // ---- yaw/pitch ----
    public static float getCameraYaw() { return cameraYaw; }
    public static void setCameraYaw(float v) { cameraYaw = v; }
    public static float getCameraPitch() { return cameraPitch; }
    public static void setCameraPitch(float v) { cameraPitch = v; }

    // ---- target ----
    public static Object getCameraTarget() { return cameraTarget; }
    public static void setCameraTarget(Object v) { cameraTarget = v; }

    // ---- delta ----
    public static double getDeltaCameraX() { return deltaCameraX; }
    public static double getDeltaCameraY() { return deltaCameraY; }
    public static double getDeltaCameraZ() { return deltaCameraZ; }
    public static void setDeltaCameraX(double v) { deltaCameraX = v; syncSpherical(); }
    public static void setDeltaCameraY(double v) { deltaCameraY = v; syncSpherical(); }
    public static void setDeltaCameraZ(double v) { deltaCameraZ = v; syncSpherical(); }

    // ---- spherical ----
    public static double getCameraSphYaw()   { return cameraSphYaw; }
    public static double getCameraSphPitch() { return cameraSphPitch; }
    public static double getCameraSphDist()  { return cameraSphDist; }
    public static void setCameraSphYaw(double v)   { cameraSphYaw = v; updateCartesian(); }
    public static void setCameraSphPitch(double v) { cameraSphPitch = v; updateCartesian(); }
    public static void setCameraSphDist(double v)  { cameraSphDist = Math.max(1, v); updateCartesian(); }

    // ---- 坐标转换 ----
    public static void updateCartesian() {
        double yr = Math.toRadians(cameraSphYaw);
        double pr = Math.toRadians(cameraSphPitch);
        double cp = Math.cos(pr);
        deltaCameraX = cameraSphDist * cp * Math.sin(yr);
        deltaCameraY = cameraSphDist * Math.sin(pr);
        deltaCameraZ = cameraSphDist * cp * Math.cos(yr);
    }

    private static void syncSpherical() {
        double h = Math.sqrt(deltaCameraX * deltaCameraX + deltaCameraZ * deltaCameraZ);
        cameraSphDist = Math.sqrt(deltaCameraX*deltaCameraX + deltaCameraY*deltaCameraY + deltaCameraZ*deltaCameraZ);
        cameraSphPitch = Math.toDegrees(Math.atan2(deltaCameraY, h));
        cameraSphYaw   = Math.toDegrees(Math.atan2(deltaCameraX, deltaCameraZ));
    }

    public static void syncDeltaToConfig() {
        com.aaa.combatperspective.Config.CAMERA_DELTA_X.set(deltaCameraX);
        com.aaa.combatperspective.Config.CAMERA_DELTA_Y.set(deltaCameraY);
        com.aaa.combatperspective.Config.CAMERA_DELTA_Z.set(deltaCameraZ);
    }

    // ---- FOV ----
    private static int fovMode = 1;
    public static int getFovMode() { return fovMode; }
    public static void setFovMode(int v) { fovMode = v; }
}
