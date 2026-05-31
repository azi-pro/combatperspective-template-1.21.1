package com.aaa.combatperspective.data;

public class CursorStore {
    private static boolean enable = false;
    private static double x;
    private static double y;
    private static float cameraYaw;              // 摄像机水平朝向
    private static float cameraPitch;
    private static Object cameraTarget;         // 摄像机指向的实体（null=未初始化）

    // 包访问权限，同包/子包可调用，无public
    static boolean isEnable() {
        return enable;
    }

    static void setEnable(boolean flag) {
        enable = flag;
    }

    static double getX() {
        return x;
    }

    static double getY() {
        return y;
    }

    static void setPos(double px, double py) {
        x = px;
        y = py;
    }

    /** 摄像机水平朝向（yaw），由 CameraMixin 每帧写入 */
    public static float getCameraYaw() {
        return cameraYaw;
    }

    public static void setCameraYaw(float yaw) {
        cameraYaw = yaw;
    }

    public static float getCameraPitch() { return cameraPitch;}

    public static void setCameraPitch(float pitch){
        cameraPitch = pitch;
    }
    /** 摄像机当前指向的实体，仅对本地玩家有效 */
    public static Object getCameraTarget() {
        return cameraTarget;
    }

    public static void setCameraTarget(Object target) {
        cameraTarget = target;
    }

    // ===== 射线命中信息（mouseLook 写入，CombatPerspectiveClient 渲染） =====
    private static net.minecraft.world.phys.Vec3 hitPos;
    private static net.minecraft.core.Direction hitDir;
    private static net.minecraft.core.BlockPos hitBlockPos;
    private static boolean hitBlock;

    public static void setHit(net.minecraft.world.phys.Vec3 pos,
                              net.minecraft.core.Direction dir,
                              net.minecraft.core.BlockPos blockPos,
                              boolean isBlock) {
        hitPos = pos;
        hitDir = dir;
        hitBlockPos = blockPos;
        hitBlock = isBlock;
    }

    public static net.minecraft.world.phys.Vec3 getHitPos()     { return hitPos; }
    public static net.minecraft.core.Direction getHitDir()      { return hitDir; }
    public static net.minecraft.core.BlockPos getHitBlockPos()  { return hitBlockPos; }
    public static boolean isHitBlock()                          { return hitBlock; }

}