package com.aaa.combatperspective.mixin;

import com.aaa.combatperspective.CombatPerspectiveClient;
import com.aaa.combatperspective.data.CursorStore;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    /** 战斗视角守卫 */
    private static boolean isthirdPersonback(Minecraft mc) {
        return !mc.options.getCameraType().isFirstPerson()
                && !mc.options.getCameraType().isMirrored()
                && mc.screen == null;
    }

    /** 禁用侧移 */
    @Inject(method = "tick", at = @At("HEAD"))
    private void turnThenMove(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!isthirdPersonback(mc)) return;
        LocalPlayer self = (LocalPlayer) (Object) this;
        self.input.left = false;
        self.input.right = false;
    }

    /** 鼠标视觉：鼠标位置 → 世界射线 → 玩家看向交点 */
    @Inject(method = "tick", at = @At("TAIL"))
    private void mouseLook(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!isthirdPersonback(mc)) return;

        LocalPlayer self = (LocalPlayer) (Object) this;
        Camera cam = mc.gameRenderer.getMainCamera();

        // 1. 鼠标屏幕位置 → (-1,1) 归一化
        double mouseXpos = mc.mouseHandler.xpos();
        double mouseYpos = mc.mouseHandler.ypos();
        int windowWidth = mc.getWindow().getWidth();
        int windowHeight = mc.getWindow().getHeight();

        double xNorm = Mth.clamp((mouseXpos - windowWidth / 2.0) / (windowWidth / 2.0), -1.0, 1.0);
        double yNorm = Mth.clamp((mouseYpos - windowHeight / 2.0) / (windowHeight / 2.0), -1.0, 1.0);

        // 2. 用 Camera 自身的向量，不绕 yaw/pitch 角度
        Vec3 forward = new Vec3(cam.getLookVector());
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 camRight = forward.cross(worldUp).normalize();
        Vec3 camUp    = camRight.cross(forward).normalize();

        // 3. FOV：用实际渲染值（含疾跑修饰）
        double vFov = Math.toRadians(CombatPerspectiveClient.getCurrentFov());
        double aspect = (double) windowWidth / windowHeight;
        double tanHalfH = Math.tan(2 * Math.atan(Math.tan(vFov / 2) * aspect) / 2);
        double tanHalfV = Math.tan(vFov / 2);

        // 4. 屏幕 NDC → 相机空间方向 → 世界方向
        Vec3 dir = forward
                .add(camRight.scale(xNorm * tanHalfH))
                .add(camUp.scale(-yNorm * tanHalfV))
                .normalize();

        // 6. 射线：起点=摄像机世界位置，方向=dir
        Vec3 origin = cam.getPosition();
        Vec3 end = origin.add(dir.scale(256.0));

        ClipContext ctx = new ClipContext(
                origin, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                self
        );
        HitResult hit = self.level().clip(ctx);
        boolean isBlock = hit.getType() == HitResult.Type.BLOCK;

        Vec3 target = isBlock ? hit.getLocation() : end;

        // 导出命中信息供渲染使用
        CursorStore.setHit(target,
                isBlock ? ((net.minecraft.world.phys.BlockHitResult) hit).getDirection() : null,
                isBlock ? ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos() : null,
                isBlock);

        // 7. 玩家看向目标
        self.lookAt(EntityAnchorArgument.Anchor.EYES, target);
    }

    /**
     * 覆盖原版疾跑判定：去掉 forwardImpulse > 0.8 的条件，
     * 改为「按键方向与玩家视线夹角 < 45°」
     */
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void overrideSprint(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!isthirdPersonback(mc)) return;

        LocalPlayer self = (LocalPlayer) (Object) this;

        boolean w = mc.options.keyUp.isDown();
        boolean a = mc.options.keyLeft.isDown();
        boolean s = mc.options.keyDown.isDown();
        boolean d = mc.options.keyRight.isDown();
        boolean anyKey = w || a || s || d;

        if (!anyKey) {
            self.setSprinting(false);
            return;
        }

        boolean blockStop = self.horizontalCollision && !self.minorHorizontalCollision;
        boolean waterStop = self.isInWater() && !self.isUnderWater();
        if (blockStop || waterStop) {
            self.setSprinting(false);
            return;
        }

        double moveX = (a ? -1 : 0) + (d ? 1 : 0);
        double moveZ = (w ? -1 : 0) + (s ? 1 : 0);

        float moveYaw = (float) Math.toDegrees(Math.atan2(-moveX, moveZ));
        if (moveYaw < 0) moveYaw += 360;

        float playerYaw = self.getYRot() % 360;
        if (playerYaw < 0) playerYaw += 360;

        float diff = Math.abs(moveYaw - playerYaw) % 360;
        float angle = diff > 180 ? 360 - diff : diff;

        boolean hasEnoughFood = self.isPassenger()
                || (float) self.getFoodData().getFoodLevel() > 6.0F
                || self.getAbilities().mayfly;

        self.setSprinting(angle < 45 && hasEnoughFood && !self.isUsingItem());
    }
}