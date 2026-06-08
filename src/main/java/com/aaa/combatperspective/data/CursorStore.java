package com.aaa.combatperspective.data;

/** 光标位置（保留，暂未使用） */
public class CursorStore {
    private static boolean enable = false;
    private static double x;
    private static double y;

    static boolean isEnable() { return enable; }
    static void setEnable(boolean flag) { enable = flag; }
    static double getX() { return x; }
    static double getY() { return y; }
    static void setPos(double px, double py) { x = px; y = py; }
}
