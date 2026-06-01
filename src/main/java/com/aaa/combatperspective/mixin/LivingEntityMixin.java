// =============================================================================
// LivingEntityMixin.java - 生物实体 Mixin（跳跃逻辑）
// =============================================================================

package com.aaa.combatperspective.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// =============================================================================
// 生物实体 Mixin：修改 LivingEntity 的跳跃行为
// 用于修复战斗视角模式下的疾跑跳跃方向
// 注意：@Mixin(LivingEntity.class) 会影响所有生物，但我们的逻辑只处理本地玩家
// =============================================================================
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // =========================================================================
    // 调用器：访问 getJumpPower() 私有方法
    // Invoker：生成一个调用目标类私有方法的方法
    // getJumpPower() 返回生物的跳跃速度
    // =========================================================================
    @Invoker("getJumpPower")
    abstract float invokeGetJumpPower();

    // =========================================================================
    // 修复疾跑跳跃方向方法
    // cancellable = true：允许取消原方法执行
    // jumpFromGround 是所有生物跳跃的入口点
    // =========================================================================
    @Inject(method = "jumpFromGround", at = At.HEAD, cancellable = true)
    private void fixSprintJumpDirection(CallbackInfo ci) {
        // 将 this 转换为 LivingEntity
        LivingEntity self = (LivingEntity) (Object) this;

        // =========================================================================
        // 安全检查：只对本地玩家 + 第三人称后视角处理
        // instanceof LocalPlayer：确保是玩家而非其他生物
        // mc.options != null：确保游戏选项可用
        // =========================================================================
        Minecraft mc = Minecraft.getInstance();
        if (!(self instanceof LocalPlayer) || mc.options == null) return;

        // 如果是第一人称或镜像视角，不处理
        if (mc.options.getCameraType().isFirstPerson()
                || mc.options.getCameraType().isMirrored()) return;

        // =========================================================================
        // 第一部分：垂直跳跃
        // 重置 Y 轴速度为标准跳跃力
        // 忽略原版的疾跑加成（会在后面自己计算）
        // =========================================================================
        Vec3 vel = self.getDeltaMovement();
        self.setDeltaMovement(vel.x, invokeGetJumpPower(), vel.z);

        // =========================================================================
        // 第二部分：疾跑助推
        // 自己计算疾跑条件，不依赖 isSprinting()（因为可能时序问题）
        // =========================================================================

        // 读取 WASD 按键状态
        boolean w = mc.options.keyUp.isDown();
        boolean a = mc.options.keyLeft.isDown();
        boolean s = mc.options.keyDown.isDown();
        boolean d = mc.options.keyRight.isDown();

        // 如果没有按方向键，只做普通跳跃
        if (!w && !a && !s && !d) {
            self.hasImpulse = true;
            ci.cancel();
            return;
        }

        // =========================================================================
        // 计算移动方向向量（与 LocalPlayerMixin 相同的逻辑）
        // =========================================================================
        double moveX = (a ? -1 : 0) + (d ? 1 : 0);
        double moveZ = (w ? -1 : 0) + (s ? 1 : 0);

        // 归一化移动向量（防止斜向移动速度更快）
        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
        moveX /= len;
        moveZ /= len;

        // =========================================================================
        // 计算移动方向角度
        // =========================================================================
        float moveYaw = (float) Math.toDegrees(Math.atan2(-moveX, moveZ));
        if (moveYaw < 0) moveYaw += 360;

        // =========================================================================
        // 获取玩家朝向
        // =========================================================================
        float playerYaw = self.getYRot() % 360;
        if (playerYaw < 0) playerYaw += 360;

        // =========================================================================
        // 计算夹角
        // =========================================================================
        float diff = Math.abs(moveYaw - playerYaw) % 360;
        float angle = diff > 180 ? 360 - diff : diff;

        // =========================================================================
        // 检查饱食度条件
        // 与 LocalPlayerMixin.overrideSprint 相同的逻辑
        // =========================================================================
        boolean hasFood = self instanceof LocalPlayer lp
                && (lp.isPassenger() || (float) lp.getFoodData().getFoodLevel() > 6.0F || lp.getAbilities().mayfly);

        // =========================================================================
        // 如果满足疾跑条件，应用疾跑助推
        // 疾跑跳跃应该给玩家一个向移动方向的速度加成
        // =========================================================================
        if (angle < 45 && hasFood) {
            // 将移动方向角度转换为弧度
            float rad = moveYaw * (float) (Math.PI / 180.0);

            // 应用助推速度：0.2 格/帧的额外速度
            self.setDeltaMovement(self.getDeltaMovement().add(
                    -Mth.sin(rad) * 0.2,
                    0,
                    Mth.cos(rad) * 0.2
            ));
        }

        // =========================================================================
        // 标记为有冲量，防止原方法再次触发
        // 取消原方法执行（我们已经手动完成了跳跃）
        // =========================================================================
        self.hasImpulse = true;
        ci.cancel();
    }
}