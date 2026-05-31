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
    @Unique
    private float camerayaw;  // 世界水平角度固定(朝北
    private float camerapitch;

    /** 每帧开头无条件清除，保证切回第一人称时立即复原 */
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

        // 第一人称 → 清除摄像机状态，走原版移动
        if (mc.options.getCameraType().isFirstPerson()) {
            CursorStore.setCameraTarget(null);
            return;
        }

        // 不指向玩家 → 清除，走原版
        if (!(entity instanceof Player player)) {
            CursorStore.setCameraTarget(null);
            return;
        }

        Camera camera = (Camera) (Object) this;

        Vec3 eye = player.getEyePosition(partialTick);
        double deltacameraY = 6;
        double deltacameraZ = 6;
        double x = eye.x;
        double y = eye.y + deltacameraY;
        double z = eye.z + deltacameraZ;

        this.camerapitch = 45 ;
        this.camerayaw = 180;

        // 同步到静态变量，EntityMixin 只对这个 entity 生效
        CursorStore.setCameraYaw(this.camerayaw);
        CursorStore.setCameraPitch(this.camerapitch);
        CursorStore.setCameraTarget(entity);

        ((CameraInvoker) camera).invokeSetPosition(new Vec3(x, y, z));
        ((CameraInvoker) camera).invokeSetRotation(camerayaw, camerapitch);

        ci.cancel();
    }
}