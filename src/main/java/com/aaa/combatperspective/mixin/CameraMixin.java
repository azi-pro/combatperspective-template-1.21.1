// =============================================================================
// CameraMixin.java - 摄像机控制 Mixin
// =============================================================================
// 包声明
package com.aaa.combatperspective.mixin;

// 导入数据存储类，用于跨 Mixin 共享数据
import com.aaa.combatperspective.data.CursorStore;

// 导入 Minecraft 客户端相机类
import net.minecraft.client.Camera;

// 导入 Minecraft 主客户端类
import net.minecraft.client.Minecraft;

// 导入实体基类
import net.minecraft.world.entity.Entity;

// 导入玩家类
import net.minecraft.world.entity.player.Player;

// 导入方块世界读取接口（用于射线检测上下文）
import net.minecraft.world.level.BlockGetter;

// 导入三维向量类
import net.minecraft.world.phys.Vec3;

// 导入 Mixin 注解
import org.spongepowered.asm.mixin.Mixin;

// 导入 @Unique 注解，标记 Mixin 类中的私有字段
import org.spongepowered.asm.mixin.Unique;

// 导入注入注解，表示要拦截的方法位置
import org.spongepowered.asm.mixin.injection.At;

// 导入注入注解，用于修改方法行为
import org.spongepowered.asm.mixin.injection.Inject;

// 导入回调信息类，用于控制方法是否继续执行
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// =============================================================================
// 摄像机 Mixin：修改 Camera 类的行为
// Mixin(Camera.class)：表示此 Mixin 将被注入到 Minecraft 的 Camera 类中
// 用于控制第三人称视角下摄像机的位置和旋转
// =============================================================================
@Mixin(Camera.class)
public class CameraMixin {
    // =========================================================================
    // Mixin 类自身的私有字段
    // Unique：确保这些字段不会与目标类冲突
    // 这些字段用于存储摄像机的朝向角度
    // =========================================================================
    @Unique
    private float camerayaw;  // 摄像机水平角度（yaw），初始朝北
    private float camerapitch; // 摄像机垂直角度（pitch）

    // =========================================================================
    // 重置摄像机目标方法
    // Inject：在 Camera.setup() 方法开头注入
    // 每帧开头无条件清除摄像机目标，保证切回第一人称时立即复原
    // param ci 回调信息，用于控制方法是否继续执行
    // =========================================================================
    @Inject(method = "setup", at = @At("HEAD"))
    private void resetCameraTarget(CallbackInfo ci) {
        // 清除 CursorStore 中的摄像机目标
        // 这样当切换到第一人称视角时，其他 Mixin 知道战斗视角已停用
        CursorStore.setCameraTarget(null);
    }

    // =========================================================================
    // 第三人称固定相机方法
    // 这是核心的摄像机控制逻辑
    // Inject：在 Camera.move() 方法被调用时注入
    // cancellable = true：允许取消原方法的执行
    // =========================================================================
    @Inject(method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;move(FFF)V"),
            cancellable = true)
    private void onlyThirdPersonFixedCamera
            (BlockGetter level,
            Entity entity,
            boolean detached,
            boolean mirror,
            float partialTick,
            CallbackInfo ci) {
        // 获取 Minecraft 实例
        Minecraft mc = Minecraft.getInstance();

        // -------------------------------------------------------------------------
        // 条件1：第一人称视角
        // 如果是第一人称，清除摄像机状态，走原版移动逻辑
        // -------------------------------------------------------------------------
        if (mc.options.getCameraType().isFirstPerson()) {
            CursorStore.setCameraTarget(null);
            return; // 不取消，让原方法正常执行
        }

        // -------------------------------------------------------------------------
        // 条件2：不是玩家
        // 如果摄像机不是指向玩家，清除状态，走原版逻辑
        // 这处理了可能是其他实体（如马的相机）的情况
        // -------------------------------------------------------------------------
        if (!(entity instanceof Player player)) {
            CursorStore.setCameraTarget(null);
            return;
        }

        // -------------------------------------------------------------------------
        // 成功条件：第三人称后视角的本地玩家
        // 强制设置摄像机到固定位置和角度
        // -------------------------------------------------------------------------

        // 将 this 转换为 Camera 类型（Mixin 的 Object）
        Camera camera = (Camera) (Object) this;

        // 计算玩家眼睛位置（考虑动画插值）
        Vec3 eye = player.getEyePosition(partialTick);

        // 计算摄像机位置偏移
        // deltacameraY = 6：摄像机在玩家上方 6 格
        // deltacameraZ = 6：摄像机在玩家后方 6 格
        double deltacameraY = 6;
        double deltacameraZ = 6;
        double x = eye.x;
        double y = eye.y + deltacameraY;
        double z = eye.z + deltacameraZ;

        // 设置固定角度
        // camerapitch = 45：向下俯视 45 度
        // camerayaw = 180：朝向正北（MC 的 0° 通常是 +Z/南，180° 是 -Z/北）
        this.camerapitch = 45 ;
        this.camerayaw = 180;

        // -------------------------------------------------------------------------
        // 同步到 CursorStore
        // 其他 Mixin（LocalPlayerMixin）会读取这些值来决定玩家朝向
        // 这样做是因为 Mixin 之间无法直接调用对方的方法
        // -------------------------------------------------------------------------
        CursorStore.setCameraYaw(this.camerayaw);    // 存储摄像机 yaw
        CursorStore.setCameraPitch(this.camerapitch); // 存储摄像机 pitch
        CursorStore.setCameraTarget(entity);          // 标记摄像机指向此玩家

        // -------------------------------------------------------------------------
        // 强制设置摄像机位置和旋转
        // CameraInvoker 是另一个 Mixin，提供对 Camera 私有方法的访问
        // invokeSetPosition：直接设置摄像机位置
        // invokeSetRotation：直接设置摄像机旋转角度
        // -------------------------------------------------------------------------
        ((CameraInvoker) camera).invokeSetPosition(new Vec3(x, y, z));
        ((CameraInvoker) camera).invokeSetRotation(camerayaw, camerapitch);

        // -------------------------------------------------------------------------
        // 取消原方法执行
        // 这样 Camera.move() 就不会被调用
        // 摄像机会保持我们设置的位置和角度
        // -------------------------------------------------------------------------
        ci.cancel();
    }
}