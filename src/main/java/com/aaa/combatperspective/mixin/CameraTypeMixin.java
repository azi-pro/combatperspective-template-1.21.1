package com.aaa.combatperspective.mixin;

import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CameraType.class, priority = 200)
public class CameraTypeMixin {

    // 映射原版私有字段
    @Final
    @Shadow
    private boolean firstPerson;

    /**
     * 修改第一人称判断逻辑
     */
    @Inject(method = "isFirstPerson", at = @At("RETURN"), cancellable = true)
    private void isFirstPerson(CallbackInfoReturnable<Boolean> ci) {
        // ================= 你可以在这里写你的视角反转逻辑 =================
        // 示例：
        // boolean inverted = 你的状态类.isPerspectiveInverted();
        // ci.setReturnValue(firstPerson ^ inverted);
        // ==============================================================
    }

    /**
     * 修改视角切换：第一人称 ↔ 第三人称后视角（跳过镜像第三人称）
     */
    @Inject(method = "cycle", at = @At("RETURN"), cancellable = true)
    private void modifyCycle(CallbackInfoReturnable<CameraType> ci) {
        if (ci.getReturnValue() == CameraType.THIRD_PERSON_FRONT) {
            ci.setReturnValue(CameraType.FIRST_PERSON);
        }
    }
}