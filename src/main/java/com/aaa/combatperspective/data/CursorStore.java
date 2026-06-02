// =============================================================================
// CursorStore.java - 光标数据存储类（跨 Mixin 数据共享）
// =============================================================================
// 包声明
package com.aaa.combatperspective.data;

// =============================================================================
// CursorStore：光标数据存储类
// 用于在不同的 Mixin 之间共享数据
// 由于 Mixin 在编译时被注入到目标类，它们之间无法直接通信
// 因此使用静态变量作为数据传递的桥梁
// =============================================================================
public class CursorStore {
    // =========================================================================
    // 静态变量：启用标志
    // 标记战斗视角功能是否启用
    // =========================================================================
    private static boolean enable = false;

    // =========================================================================
    // 静态变量：鼠标坐标
    // 存储当前鼠标位置（屏幕坐标）
    // =========================================================================
    private static double x;
    private static double y;

    // =========================================================================
    // 静态变量：摄像机朝向角度
    // cameraYaw：水平旋转角度（yaw），0° = 南，90° = 西，180° = 北，270° = 东
    // cameraPitch：垂直旋转角度（pitch），负值 = 向上看，正值 = 向下看
    // =========================================================================
    private static float cameraYaw;              // 摄像机水平朝向
    private static float cameraPitch;

    // =========================================================================
    // 静态变量：摄像机目标实体
    // 存储摄像机当前跟随的实体引用
    // null 表示摄像机未指向任何实体（未激活战斗视角）
    // =========================================================================
    private static Object cameraTarget;         // 摄像机指向的实体（null=未初始化）

    // =========================================================================
    // getter 方法：检查功能是否启用
    // 包访问权限：同包内的类可以直接访问
    // return 是否启用
    // =========================================================================
    static boolean isEnable() {
        return enable;
    }

    // =========================================================================
    // setter 方法：设置启用状态
    // param flag 启用标志
    // =========================================================================
    static void setEnable(boolean flag) {
        enable = flag;
    }

    // =========================================================================
    // getter 方法：获取鼠标 X 坐标
    // return 鼠标 X 坐标
    // =========================================================================
    static double getX() {
        return x;
    }

    // =========================================================================
    // getter 方法：获取鼠标 Y 坐标
    // return 鼠标 Y 坐标
    // =========================================================================
    static double getY() {
        return y;
    }

    // =========================================================================
    // setter 方法：同时设置鼠标坐标
    // param px X 坐标
    // param py Y 坐标
    // =========================================================================
    static void setPos(double px, double py) {
        x = px;
        y = py;
    }

    // =========================================================================
    // getter 方法：获取摄像机水平朝向角度（yaw）
    // 由 CameraMixin 每帧写入，供其他 Mixin 读取
    // return yaw 角度（度数）
    // =========================================================================
    public static float getCameraYaw() {
        return cameraYaw;
    }

    // =========================================================================
    // setter 方法：设置摄像机水平朝向角度
    // param yaw 新的 yaw 角度
    // =========================================================================
    public static void setCameraYaw(float yaw) {
        cameraYaw = yaw;
    }

    // =========================================================================
    // getter 方法：获取摄像机垂直朝向角度（pitch）
    // return pitch 角度（度数）
    // =========================================================================
    public static float getCameraPitch() { return cameraPitch;}

    // =========================================================================
    // setter 方法：设置摄像机垂直朝向角度
    // param pitch 新的 pitch 角度
    // =========================================================================
    public static void setCameraPitch(float pitch){
        cameraPitch = pitch;
    }

    // =========================================================================
    // getter 方法：获取摄像机当前指向的实体
    // 仅对本地玩家有效，用于判断战斗视角是否激活
    // return 实体对象，未激活时为 null
    // =========================================================================
    public static Object getCameraTarget() {
        return cameraTarget;
    }

    // =========================================================================
    // setter 方法：设置摄像机目标实体
    // param target 目标实体
    // =========================================================================
    public static void setCameraTarget(Object target) {
        cameraTarget = target;
    }

    // =========================================================================
    // ===================== 射线命中信息 =====================
    // 以下变量由 LocalPlayerMixin.mouseLook 写入
    // 由 CombatPerspectiveClient 渲染层读取
    // =========================================================================

    // =========================================================================
    // 射线命中位置（世界坐标）
    // 表示鼠标射线击中的点（方块表面或实体位置）
    // =========================================================================
    private static net.minecraft.world.phys.Vec3 hitPos;

    // =========================================================================
    // 命中方块时的面方向
    // 如 NORTH、SOUTH、EAST、WEST、UP、DOWN
    // 命中实体时为 null
    // =========================================================================
    private static net.minecraft.core.Direction hitDir;

    // =========================================================================
    // 命中方块的方块位置
    // 用于绘制方块选中框
    // 命中实体时为 null
    // =========================================================================
    private static net.minecraft.core.BlockPos hitBlockPos;

    // =========================================================================
    // 命中类型标志
    // true = 命中了方块，false = 命中了实体或未命中
    // =========================================================================
    private static boolean hitBlock;

    // =========================================================================
    // 设置射线命中信息
    // 由 LocalPlayerMixin.mouseLook 调用，存储射线检测结果
    // param pos 命中位置
    // param dir 命中方块的面方向（可能为 null）
    // param blockPos 命中方块的位置（可能为 null）
    // param isBlock 是否命中方块
    // =========================================================================
    public static void setHit(net.minecraft.world.phys.Vec3 pos,
                              net.minecraft.core.Direction dir,
                              net.minecraft.core.BlockPos blockPos,
                              boolean isBlock) {
        hitPos = pos;        // 存储命中位置
        hitDir = dir;        // 存储面方向
        hitBlockPos = blockPos; // 存储方块位置
        hitBlock = isBlock; // 存储命中类型
    }

    // =========================================================================
    // 获取命中位置
    // return Vec3 世界坐标中的命中点
    // =========================================================================
    public static net.minecraft.world.phys.Vec3 getHitPos()     { return hitPos; }

    // =========================================================================
    // 获取命中面方向
    // return Direction 面方向枚举值，未命中方块时可能为 null
    // =========================================================================
    public static net.minecraft.core.Direction getHitDir()      { return hitDir; }

    // =========================================================================
    // 获取命中方块的位置
    // return BlockPos 方块坐标，未命中方块时可能为 null
    // =========================================================================
    public static net.minecraft.core.BlockPos getHitBlockPos()  { return hitBlockPos; }

    // =========================================================================
    // 检查是否命中方块
    // return true = 命中方块，false = 命中实体或其他
    // =========================================================================
    public static boolean isHitBlock()                          { return hitBlock; }

    // ===== 摄像机偏移（CameraMixin 读取，球坐标驱动） =====
    private static double deltaCameraX;
    private static double deltaCameraY = 6;
    private static double deltaCameraZ = 6;

    // 球坐标：绕玩家旋转的角度和距离（初值对齐 deltaY=6, deltaZ=6）
    private static double cameraSphYaw;         // 0°=南(+Z)
    private static double cameraSphPitch = 45;  // 45°=俯角，摄像头在上方
    private static double cameraSphDist = 8.49; // √(6²+6²)

    static {
        updateCartesian();
    }

    /** 球坐标 → 笛卡尔 */
    public static void updateCartesian() {
        double yr = Math.toRadians(cameraSphYaw);
        double pr = Math.toRadians(cameraSphPitch);
        double cp = Math.cos(pr);
        deltaCameraX = cameraSphDist * cp * Math.sin(yr);
        deltaCameraY = cameraSphDist * Math.sin(pr);
        deltaCameraZ = cameraSphDist * cp * Math.cos(yr);
    }

    public static double getCameraSphYaw()   { return cameraSphYaw; }
    public static double getCameraSphPitch() { return cameraSphPitch; }
    public static double getCameraSphDist()  { return cameraSphDist; }

    public static void setCameraSphYaw(double v)   { cameraSphYaw = v; updateCartesian(); }
    public static void setCameraSphPitch(double v) { cameraSphPitch = v; updateCartesian(); }
    public static void setCameraSphDist(double v)  { cameraSphDist = Math.max(1, v); updateCartesian(); }

    public static double getDeltaCameraX() { return deltaCameraX; }
    public static double getDeltaCameraY() { return deltaCameraY; }
    public static double getDeltaCameraZ() { return deltaCameraZ; }

    /** 配置文件直写笛卡尔值，同时同步回球坐标 */
    public static void setDeltaCameraX(double v) { deltaCameraX = v; syncSpherical(); }
    public static void setDeltaCameraY(double v) { deltaCameraY = v; syncSpherical(); }
    public static void setDeltaCameraZ(double v) { deltaCameraZ = v; syncSpherical(); }

    /** 笛卡尔 → 球坐标 */
    private static void syncSpherical() {
        double hDist = Math.sqrt(deltaCameraX * deltaCameraX + deltaCameraZ * deltaCameraZ);
        cameraSphDist = Math.sqrt(deltaCameraX * deltaCameraX + deltaCameraY * deltaCameraY + deltaCameraZ * deltaCameraZ);
        cameraSphPitch = Math.toDegrees(Math.atan2(deltaCameraY, hDist));
        cameraSphYaw   = Math.toDegrees(Math.atan2(deltaCameraX, deltaCameraZ));
    }

}