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

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Invoker("getJumpPower")
    abstract float invokeGetJumpPower();

    @Inject(method = "jumpFromGround", at = At.HEAD, cancellable = true)
    private void fixSprintJumpDirection(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        Minecraft mc = Minecraft.getInstance();
        if (!(self instanceof LocalPlayer) || mc.options == null) return;

        if (mc.options.getCameraType().isFirstPerson()
                || mc.options.getCameraType().isMirrored()) return;

        Vec3 vel = self.getDeltaMovement();
        self.setDeltaMovement(vel.x, invokeGetJumpPower(), vel.z);

        boolean w = mc.options.keyUp.isDown();
        boolean a = mc.options.keyLeft.isDown();
        boolean s = mc.options.keyDown.isDown();
        boolean d = mc.options.keyRight.isDown();

        if (!w && !a && !s && !d) {
            self.hasImpulse = true;
            ci.cancel();
            return;
        }

        double moveX = (a ? -1 : 0) + (d ? 1 : 0);
        double moveZ = (w ? -1 : 0) + (s ? 1 : 0);

        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
        moveX /= len;
        moveZ /= len;

        float moveYaw = (float) Math.toDegrees(Math.atan2(-moveX, moveZ));
        if (moveYaw < 0) moveYaw += 360;

        float playerYaw = self.getYRot() % 360;
        if (playerYaw < 0) playerYaw += 360;

        float diff = Math.abs(moveYaw - playerYaw) % 360;
        float angle = diff > 180 ? 360 - diff : diff;

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
