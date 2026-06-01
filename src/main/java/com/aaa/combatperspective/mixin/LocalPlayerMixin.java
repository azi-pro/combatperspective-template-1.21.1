// =============================================================================
// LocalPlayerMixin.java - 本地玩家 Mixin（玩家跟随鼠标）
// =============================================================================
// 包声明
package com.aaa.combatperspective.mixin;

// 导入数据存储类
import com.aaa.combatperspective.data.CursorStore;

// 导入相机类
import net.minecraft.client.Camera;

// 导入 Minecraft 主类
import net.minecraft.client.Minecraft;

// 导入本地玩家类
import net.minecraft.client.player.LocalPlayer;

// 导入实体锚点参数，用于指定看向位置
import net.minecraft.commands.arguments.EntityAnchorArgument;

// 导入数学工具类
import net.minecraft.util.Mth;

// 导入射线检测上下文
import net.minecraft.world.level.ClipContext;

// 导入命中结果类
import net.minecraft.world.phys.HitResult;

// 导入三维向量类
import net.minecraft.world.phys.Vec3;

// 导入 Mixin 注解
import org.spongepowered.asm.mixin.Mixin;

// 导入注入注解
import org.spongepowered.asm.mixin.injection.At;

// 导入注入注解
import org.spongepowered.asm.mixin.injection.Inject;

// 导入回调信息类
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// =============================================================================
// 本地玩家 Mixin：修改 LocalPlayer 的行为
// 实现鼠标控制玩家朝向的核心功能
// =============================================================================
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    // =========================================================================
    // 战斗视角守卫方法
    // 判断当前是否应该启用战斗视角模式
    // param mc Minecraft 实例
    // return true = 启用战斗视角，false = 不启用
    // =========================================================================
    private static boolean isThirdPersonback(Minecraft mc) {
        // 必须满足三个条件：
        // 1. 不是第一人称视角
        // 2. 不是镜像视角（第三人称前视角）
        // 3. 没有 GUI 打开（GUI 会干扰鼠标输入）
        return !mc.options.getCameraType().isFirstPerson()
                && !mc.options.getCameraType().isMirrored()
                && mc.screen == null;
    }

    // =========================================================================
    // 禁用侧移方法
    // 在战斗视角模式下，不允许玩家左右移动
    // Inject：在 LocalPlayer.tick() 方法开头注入
    // at = At.HEAD：在方法开始时执行
    // param ci 回调信息
    // =========================================================================
    @Inject(method = "tick", at = @At("HEAD"))
    private void turnThenMove(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        // 如果不是战斗视角模式，不处理
        if (!isThirdPersonback(mc)) return;

        // 将 this 转换为 LocalPlayer
        LocalPlayer self = (LocalPlayer) (Object) this;

        // 禁用左右移动输入
        // 这样玩家只能前进/后退/跳跃
        self.input.left = false;
        self.input.right = false;
    }

    // =========================================================================
    // 鼠标视觉方法：鼠标位置 → 世界射线 → 玩家看向交点
    // 这是战斗视角的核心逻辑
    // Inject：在 LocalPlayer.tick() 方法末尾注入
    // at = At.TAIL：在方法结束时执行
    // param ci 回调信息
    // =========================================================================
    @Inject(method = "tick", at = At.TAIL)
    private void mouseLook(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        // 如果不是战斗视角模式，不处理
        if (!isThirdPersonback(mc)) return;

        LocalPlayer self = (LocalPlayer) (Object) this;
        Camera cam = mc.gameRenderer.getMainCamera();

        // =========================================================================
        // 步骤1：鼠标屏幕位置 → (-1, 1) 归一化
        // 将鼠标坐标转换为相对屏幕中心的位置
        // =========================================================================

        // 获取鼠标当前像素位置（相对于窗口左上角）
        double mouseXpos = mc.mouseHandler.xpos();
        double mouseYpos = mc.mouseHandler.ypos();

        // 获取窗口尺寸（像素）
        int windowWidth = mc.getWindow().getWidth();
        int windowHeight = mc.getWindow().getHeight();

        // 将鼠标坐标归一化到 [-1, 1] 范围
        // xNorm = 0 表示屏幕中央，-1 表示最左，1 表示最右
        double xNorm = Mth.clamp((mouseXpos - windowWidth / 2.0) / (windowWidth / 2.0), -1.0, 1.0);
        double yNorm = Mth.clamp((mouseYpos - windowHeight / 2.0) / (windowHeight / 2.0), -1.0, 1.0);

        // =========================================================================
        // 步骤2：计算 FOV（视野角度）
        // FOV 影响鼠标移动到角度偏移的映射关系
        // =========================================================================

        // 获取玩家设置的垂直 FOV
        double fov = mc.options.fov().get().intValue();

        // 转换为弧度
        double vFov = Math.toRadians(fov);

        // 计算屏幕宽高比
        double aspect = (double) windowWidth / windowHeight;

        // 计算水平 FOV（根据宽高比）
        // 公式：水平 FOV = 2 * atan(tan(垂直FOV/2) * 宽高比)
        double hFov = 2 * Math.atan(Math.tan(vFov / 2) * aspect);

        // =========================================================================
        // 步骤3：获取摄像机基准方向
        // 摄像机的方向是固定的（由 CameraMixin 设置）
        // 鼠标偏移是相对于这个基准方向的
        // =========================================================================

        // 从 CursorStore 读取摄像机角度
        float camYaw = CursorStore.getCameraYaw();  // 180°（朝北）
        float camPitch = CursorStore.getCameraPitch(); // 45°

        // =========================================================================
        // 步骤4：鼠标偏移 → 角度偏移
        // 使用 atan 还原透视投影的非线性映射
        // 这样鼠标在屏幕边缘时视角偏移最大
        // =========================================================================

        // 计算 FOV 的一半的正切值
        double tanHalfH = Math.tan(hFov / 2);
        double tanHalfV = Math.tan(vFov / 2);

        // 计算鼠标位置对应的角度偏移
        // xNorm * tanHalfH：将鼠标 X 位置映射到 [-tan(FOV/2), tan(FOV/2)] 范围
        // atan：将线性映射转换为角度
        double yawOff   = Math.atan(xNorm * tanHalfH);
        double pitchOff = Math.atan(yNorm * tanHalfV);

        // 计算最终的射线方向角度
        float rayYaw   = (float) (camYaw   + Math.toDegrees(yawOff));
        float rayPitch = (float) (camPitch + Math.toDegrees(pitchOff));

        // 限制 pitch 在 [-90, 90] 度之间，防止翻转
        rayPitch = Mth.clamp(rayPitch, -90F, 90F);

        // =========================================================================
        // 步骤5：yaw/pitch → 世界方向向量
        // 将角度转换为单位方向向量
        // =========================================================================

        // 转换为弧度
        float yr = rayYaw   * (float) (Math.PI / 180.0);
        float pr = rayPitch * (float) (Math.PI / 180.0);

        // 球面坐标转笛卡尔坐标
        // X = -sin(yaw) * cos(pitch)
        // Y = -sin(pitch)
        // Z = cos(yaw) * cos(pitch)
        Vec3 dir = new Vec3(
                -Mth.sin(yr) * Mth.cos(pr),
                -Mth.sin(pr),
                 Mth.cos(yr) * Mth.cos(pr)
        );

        // =========================================================================
        // 步骤6：射线检测
        // 从摄像机位置发射射线，检测击中的方块或实体
        // =========================================================================

        // 射线起点 = 摄像机世界位置
        Vec3 origin = cam.getPosition();

        // 射线终点 = 起点 + 方向 * 256 格（最大检测距离）
        Vec3 end = origin.add(dir.scale(256.0));

        // 创建射线检测上下文
        ClipContext ctx = new ClipContext(
                origin, end,                          // 起点和终点
                ClipContext.Block.OUTLINE,            // 只检测方块轮廓
                ClipContext.Fluid.NONE,               // 不检测液体
                self                                   // 排除玩家自身
        );

        // 执行方块射线检测
        HitResult hit = self.level().clip(ctx);

        // 判断是否命中方块
        boolean isBlock = hit.getType() == HitResult.Type.BLOCK;

        // 如果命中方块，用命中位置；否则用射线终点
        Vec3 target = isBlock ? hit.getLocation() : end;

        // =========================================================================
        // 步骤7：导出命中信息供渲染层使用
        // 存储到 CursorStore，CombatPerspectiveClient 会读取并绘制标记
        // =========================================================================
        CursorStore.setHit(target,
                isBlock ? ((net.minecraft.world.phys.BlockHitResult) hit).getDirection() : null,
                isBlock ? ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos() : null,
                isBlock);

        // =========================================================================
        // 步骤8：设置玩家朝向
        // 让玩家的眼睛看向目标位置
        // EntityAnchorArgument.Anchor.EYES 表示使用玩家眼睛位置作为起点
        // =========================================================================
        self.lookAt(EntityAnchorArgument.Anchor.EYES, target);
    }

    // =========================================================================
    // 覆盖疾跑判定方法
    // 修改原版疾跑条件：从「玩家朝向与移动方向一致」改为「移动方向与固定朝向夹角 < 45°」
    // Inject：在 LocalPlayer.aiStep() 方法末尾注入
    // aiStep 是玩家物理模拟的核心方法
    // param ci 回调信息
    // =========================================================================
    @Inject(method = "aiStep", at = At.TAIL)
    private void overrideSprint(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        // 如果不是战斗视角模式，不处理
        if (!isThirdPersonback(mc)) return;

        LocalPlayer self = (LocalPlayer) (Object) this;

        // =========================================================================
        // 读取 WASD 按键状态
        // =========================================================================
        boolean w = mc.options.keyUp.isDown();       // W 键
        boolean a = mc.options.keyLeft.isDown();    // A 键
        boolean s = mc.options.keyDown.isDown();    // S 键
        boolean d = mc.options.keyRight.isDown();   // D 键
        boolean anyKey = w || a || s || d;          // 是否有任意方向键按下

        // =========================================================================
        // 如果没有按方向键，关闭疾跑
        // =========================================================================
        if (!anyKey) {
            self.setSprinting(false);
            return;
        }

        // =========================================================================
        // 检查疾跑障碍条件
        // 硬方块碰撞：玩家正在顶着方块（不能横向移动）
        // 水中停止：玩家在水中但不能正常游泳
        // =========================================================================
        boolean blockStop = self.horizontalCollision && !self.minorHorizontalCollision;
        boolean waterStop = self.isInWater() && !self.isUnderWater();
        if (blockStop || waterStop) {
            self.setSprinting(false);
            return;
        }

        // =========================================================================
        // 计算移动方向向量
        // Minecraft 的坐标系：W = -Z（北），S = +Z（南）
        // A = -X（西），D = +X（东）
        // =========================================================================
        double moveX = (a ? -1 : 0) + (d ? 1 : 0);
        double moveZ = (w ? -1 : 0) + (s ? 1 : 0);

        // =========================================================================
        // 计算移动方向角度
        // atan2(-moveX, moveZ)：计算相对于世界 Z 轴的角度
        // Minecraft 中 0° 是 +Z 方向，所以需要 -moveX
        // =========================================================================
        float moveYaw = (float) Math.toDegrees(Math.atan2(-moveX, moveZ));
        if (moveYaw < 0) moveYaw += 360; // 转换到 [0, 360) 范围

        // =========================================================================
        // 获取玩家当前朝向
        // =========================================================================
        float playerYaw = self.getYRot() % 360;
        if (playerYaw < 0) playerYaw += 360; // 转换到 [0, 360) 范围

        // =========================================================================
        // 计算两个角度之间的最小差值
        // diff > 180 时，取 360 - diff（因为 180° 是最大夹角）
        // =========================================================================
        float diff = Math.abs(moveYaw - playerYaw) % 360;
        float angle = diff > 180 ? 360 - diff : diff;

        // =========================================================================
        // 检查饱食度条件
        // 疾跑需要足够的饥饿值（> 6）或有特殊能力（飞行、骑乘）
        // =========================================================================
        boolean hasEnoughFood = self.isPassenger()
                || (float) self.getFoodData().getFoodLevel() > 6.0F
                || self.getAbilities().mayfly;

        // =========================================================================
        // 设置疾跑状态
        // 条件：夹角 < 45°（移动方向与固定朝向接近）
        //       且有足够的饱食度
        //       且没有使用物品
        // =========================================================================
        self.setSprinting(angle < 45 && hasEnoughFood && !self.isUsingItem());
    }
}