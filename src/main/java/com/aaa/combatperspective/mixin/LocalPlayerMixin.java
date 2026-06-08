package com.aaa.combatperspective.mixin;

import com.aaa.combatperspective.CombatPerspectiveClient;
import com.aaa.combatperspective.data.CameraStore;
import com.aaa.combatperspective.data.EdgeScrollStore;
import com.aaa.combatperspective.data.HitStore;
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

    /** 禁用侧移 + 边缘旋转 */
    @Inject(method = "tick", at = @At("HEAD"))
    private void turnThenMove(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!isthirdPersonback(mc)) return;
        LocalPlayer self = (LocalPlayer) (Object) this;
        self.input.left = false;
        self.input.right = false;
        edgeScroll(mc);
    }

    /** 鼠标在屏幕边缘 10% → 自动旋转摄像机，越靠边越快 */
    private static void edgeScroll(Minecraft mc) {
        if (!EdgeScrollStore.isEnabled()) return;

        double mx = mc.mouseHandler.xpos();
        double my = mc.mouseHandler.ypos();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        double marginX = w * EdgeScrollStore.getMarginX();
        double marginY = h * EdgeScrollStore.getMarginY();

        double yawBase   = EdgeScrollStore.getYawSpeed()   / 20; // 度/tick
        double pitchBase = EdgeScrollStore.getPitchSpeed() / 20;

        // 左边缘：越靠近左边越快
        if (mx < marginX) {
            int zone = (int)((1 - mx / marginX) * 5); // 0~4
            double mult = Math.pow(2, zone);
            CameraStore.setCameraSphYaw(CameraStore.getCameraSphYaw() + yawBase * mult);
        }
        // 右边缘
        if (mx > w - marginX) {
            int zone = (int)(((mx - (w - marginX)) / marginX) * 5);
            double mult = Math.pow(2, zone);
            CameraStore.setCameraSphYaw(CameraStore.getCameraSphYaw() - yawBase * mult);
        }
        // 上边缘 → 摄像头降低（pitch 减小）
        if (my < marginY) {
            int zone = (int)((1 - my / marginY) * 5);
            double mult = Math.pow(2, zone);
            CameraStore.setCameraSphPitch(
                    Mth.clamp(CameraStore.getCameraSphPitch() - pitchBase * mult, -89, 89));
        }
        // 下边缘 → 摄像头升高（pitch 增大）
        if (my > h - marginY) {
            int zone = (int)(((my - (h - marginY)) / marginY) * 5);
            double mult = Math.pow(2, zone);
            CameraStore.setCameraSphPitch(
                    Mth.clamp(CameraStore.getCameraSphPitch() + pitchBase * mult, -89, 89));
        }
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

        // 方块检测
        ClipContext ctx = new ClipContext(
                origin, end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                self
        );
        HitResult blockHit = self.level().clip(ctx);
        boolean isBlock = blockHit.getType() == HitResult.Type.BLOCK;

        // 实体检测（细线）
        net.minecraft.world.phys.AABB sweepBox =
                new net.minecraft.world.phys.AABB(origin, origin)
                        .expandTowards(dir.scale(256.0))
                        .inflate(0.1);
        net.minecraft.world.phys.EntityHitResult entityHit =
                net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                        self.level(), self, origin, end, sweepBox,
                        e -> !e.isSpectator() && e.isPickable());

        // 比较距离，取最近的
        double blockDist = isBlock
                ? origin.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
        double entityDist = entityHit != null
                ? origin.distanceToSqr(entityHit.getLocation()) : Double.MAX_VALUE;

        Vec3 target;
        if (entityDist < blockDist && entityHit != null) {
            target = entityHit.getEntity().getEyePosition();
        } else if (isBlock) {
            target = ((net.minecraft.world.phys.BlockHitResult) blockHit).getLocation();
        } else {
            target = end;
        }

        // 导出命中信息供渲染使用
        HitStore.set(target,
                isBlock && blockDist < entityDist
                        ? ((net.minecraft.world.phys.BlockHitResult) blockHit).getDirection() : null,
                isBlock && blockDist < entityDist
                        ? ((net.minecraft.world.phys.BlockHitResult) blockHit).getBlockPos() : null,
                isBlock && blockDist < entityDist);

        // 7. 玩家看向目标（但拉弓时不强制看向，避免限制视角）
        if (!self.isUsingItem()) {
            self.lookAt(EntityAnchorArgument.Anchor.EYES, target);
        }
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

        // 按键 → 世界方向（与 EntityMixin 的 moveRelative 一致，都用 cameraYaw）
        float forwardImp = (w ? 1 : 0) + (s ? -1 : 0);
        float leftImp    = (a ? 1 : 0) + (d ? -1 : 0);
        float yr = CameraStore.getCameraYaw() * (float) (Math.PI / 180.0);
        double worldX = leftImp * Math.cos(yr) - forwardImp * Math.sin(yr);
        double worldZ = forwardImp * Math.cos(yr) + leftImp * Math.sin(yr);

        float moveYaw = (float) Math.toDegrees(Math.atan2(-worldX, worldZ));
        if (moveYaw < 0) moveYaw += 360;

        float playerYaw = self.getYRot() % 360;
        if (playerYaw < 0) playerYaw += 360;

        float diff = Math.abs(moveYaw - playerYaw) % 360;
        float angle = diff > 180 ? 360 - diff : diff;

        boolean hasEnoughFood = self.isPassenger()
                || (float) self.getFoodData().getFoodLevel() > 6.0F
                || self.mayFly();

        self.setSprinting(angle < 45 && hasEnoughFood && !self.isUsingItem());
    }
}