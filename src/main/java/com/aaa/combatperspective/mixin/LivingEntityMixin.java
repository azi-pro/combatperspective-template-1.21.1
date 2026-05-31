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

/**
 * 修复疾跑跳跃：助推方向 + 疾跑条件均按 WASD 移动方向判定，不依赖 aiStep 时序。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Invoker("getJumpPower")
    abstract float invokeGetJumpPower();

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void fixSprintJumpDirection(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // 只对本地玩家 + 第三人称后视角处理
        Minecraft mc = Minecraft.getInstance();
        if (!(self instanceof LocalPlayer) || mc.options == null) return;
        if (mc.options.getCameraType().isFirstPerson()
                || mc.options.getCameraType().isMirrored()) return;

        // ===== 垂直跳跃 =====
        Vec3 vel = self.getDeltaMovement();
        self.setDeltaMovement(vel.x, invokeGetJumpPower(), vel.z);

        // ===== 疾跑助推：自己算角度，不依赖 isSprinting()  =====
        boolean w = mc.options.keyUp.isDown();
        boolean a = mc.options.keyLeft.isDown();
        boolean s = mc.options.keyDown.isDown();
        boolean d = mc.options.keyRight.isDown();

        if (!w && !a && !s && !d) {
            self.hasImpulse = true;
            ci.cancel();
            return;
        }

        // 世界移动方向（对齐 EntityMixin: W=北(-Z)）
        double moveX = (a ? -1 : 0) + (d ? 1 : 0);
        double moveZ = (w ? -1 : 0) + (s ? 1 : 0);
        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
        moveX /= len;
        moveZ /= len;

        // 移动方向角度 vs 玩家朝向夹角
        float moveYaw = (float) Math.toDegrees(Math.atan2(-moveX, moveZ));
        if (moveYaw < 0) moveYaw += 360;
        float playerYaw = self.getYRot() % 360;
        if (playerYaw < 0) playerYaw += 360;
        float diff = Math.abs(moveYaw - playerYaw) % 360;
        float angle = diff > 180 ? 360 - diff : diff;

        // 疾跑条件：夹角<45° + 饱食度>6
        boolean hasFood = self instanceof LocalPlayer lp
                && (lp.isPassenger() || (float) lp.getFoodData().getFoodLevel() > 6.0F || lp.getAbilities().mayfly);

        if (angle < 45 && hasFood) {
            float rad = moveYaw * (float) (Math.PI / 180.0);
            self.setDeltaMovement(self.getDeltaMovement().add(
                    -Mth.sin(rad) * 0.2,
                    0,
                    Mth.cos(rad) * 0.2
            ));
        }

        self.hasImpulse = true;
        ci.cancel();
    }
}
