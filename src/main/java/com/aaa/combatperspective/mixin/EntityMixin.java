package com.aaa.combatperspective.mixin;

import com.aaa.combatperspective.data.CameraStore;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截 moveRelative，把里面的 this.getYRot() 换成摄像机朝向，
 * 使得 WASD 按世界方向移动（摄像机前方=北=-Z）。
 */
@Mixin(value = Entity.class, priority = 200)
public class EntityMixin {

    /**
     * 完全复制原版 moveRelative + getInputVector 的逻辑，
     * 唯一区别：facing 参数从 this.getYRot() 改为 camerayaw。
     */
    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
    private void moveRelativeWithCameraYaw(float amount, Vec3 relative, CallbackInfo ci) {
        // 只对摄像机指向的实体生效，其他实体（僵尸、箭矢等）走原版
        if (this != CameraStore.getCameraTarget()) return;

        float yaw = CameraStore.getCameraYaw();

        // ===== 原版 getInputVector(relative, amount, facing) =====
        double lenSqr = relative.lengthSqr();
        if (lenSqr < 1.0E-7) {
            ci.cancel(); // 无输入，跳过
            return;
        }

        Vec3 scaled = (lenSqr > 1.0 ? relative.normalize() : relative)
                .scale((double) amount);

        float sin = Mth.sin(yaw * (float) (Math.PI / 180.0));
        float cos = Mth.cos(yaw * (float) (Math.PI / 180.0));

        Vec3 result = new Vec3(
                scaled.x * (double) cos - scaled.z * (double) sin,
                scaled.y,
                scaled.z * (double) cos + scaled.x * (double) sin
        );

        // ===== 原版 moveRelative 的后半段 =====
        Entity self = (Entity) (Object) this;
        self.setDeltaMovement(self.getDeltaMovement().add(result));
        ci.cancel();
    }
}
