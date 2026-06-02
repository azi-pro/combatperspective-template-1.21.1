package com.aaa.combatperspective.mixin;

import com.aaa.combatperspective.data.CursorStore;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {

    @Inject(method = "setup", at = @At("HEAD"))
    private void resetCameraTarget(CallbackInfo ci) {
        CursorStore.setCameraTarget(null);
    }

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
        Minecraft mc = Minecraft.getInstance();

        if (mc.options.getCameraType().isFirstPerson()) {
            CursorStore.setCameraTarget(null);
            return;
        }

        if (!(entity instanceof Player player)) {
            CursorStore.setCameraTarget(null);
            return;
        }

        Camera camera = (Camera) (Object) this;

        Vec3 eye = player.getEyePosition(partialTick);
        double dx = CursorStore.getDeltaCameraX();
        double dy = CursorStore.getDeltaCameraY();
        double dz = CursorStore.getDeltaCameraZ();

        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float yaw   = (float) Math.toDegrees(Math.atan2(-dx, -dz));
        float pitch = (float) Math.toDegrees(Math.asin(dy / len));

        CursorStore.setCameraYaw(yaw);
        CursorStore.setCameraTarget(entity);

        ((CameraInvoker) camera).invokeSetPosition(new Vec3(eye.x + dx, eye.y + dy, eye.z + dz));
        ((CameraInvoker) camera).invokeSetRotation(yaw, pitch);

        ci.cancel();
    }
}