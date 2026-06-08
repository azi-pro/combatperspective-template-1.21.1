package com.aaa.combatperspective.mixin;

import com.aaa.combatperspective.data.CameraStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    /**
     * 方案二：疾跑时暂时摘下 sprint 标签，算完 FOV 修正再戴回去。
     * 只去掉疾跑对 FOV 的影响，保留药水/飞行等一切其他效果。
     */
    @Redirect(
            method = "tickFov",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getFieldOfViewModifier()F"
            )
    )
    private float removeSprintFromFov(AbstractClientPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (CameraStore.getFovMode() == 0) return player.getFieldOfViewModifier(); // 方案一

        boolean thirdPerson = mc.options != null
                && !mc.options.getCameraType().isFirstPerson()
                && !mc.options.getCameraType().isMirrored();

        if (thirdPerson && player.isSprinting()) {
            player.setSprinting(false);
            float fov = player.getFieldOfViewModifier();
            player.setSprinting(true);
            return fov;
        }
        return player.getFieldOfViewModifier();
    }
}
