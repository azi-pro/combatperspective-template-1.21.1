package com.aaa.combatperspective.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Boat.class)
public class BoatMixin {

    @Inject(method = "controlBoat", at = @At("TAIL"))
    private void syncYawToPlayer(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.getControllingPassenger() instanceof Player player) {
            // 最短路径旋转，避免跨越 ±180° 时抽搐
            float cur = self.getYRot();
            float target = player.getYRot();
            float diff = Mth.wrapDegrees(target - cur);
            self.setYRot(cur + diff);
        }
    }
}
