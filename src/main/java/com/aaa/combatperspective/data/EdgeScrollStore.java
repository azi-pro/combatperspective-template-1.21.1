package com.aaa.combatperspective.data;

/** 边缘滚动配置 */
public class EdgeScrollStore {
    private static boolean enabled = true;
    private static double marginX = 0.10;
    private static double marginY = 0.10;
    private static double yawSpeed = 2.0;
    private static double pitchSpeed = 2.0;

    public static boolean isEnabled()     { return enabled; }
    public static void setEnabled(boolean v) { enabled = v; }
    public static double getMarginX()     { return marginX; }
    public static double getMarginY()     { return marginY; }
    public static void setMarginX(double v) { marginX = v; }
    public static void setMarginY(double v) { marginY = v; }
    public static double getYawSpeed()    { return yawSpeed; }
    public static double getPitchSpeed()  { return pitchSpeed; }
    public static void setYawSpeed(double v)   { yawSpeed = v; }
    public static void setPitchSpeed(double v) { pitchSpeed = v; }
}
