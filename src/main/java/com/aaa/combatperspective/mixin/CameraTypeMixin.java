package com.aaa.combatperspective.mixin;

import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CameraType.class, priority = 200)//默认priority = 1000
public class CameraTypeMixin {
    // 修改视角切换：第一人称 ↔ 第三人称后视角（跳过镜像第三人称）
    @Inject(method = "cycle", at = @At("RETURN"), cancellable = true)
    private void modifyCycle(CallbackInfoReturnable<CameraType> ci) {
        if (ci.getReturnValue() == CameraType.THIRD_PERSON_FRONT) {
            ci.setReturnValue(CameraType.FIRST_PERSON);
        }
    }
}