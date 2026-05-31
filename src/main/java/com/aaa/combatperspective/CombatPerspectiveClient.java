package com.aaa.combatperspective;

import com.aaa.combatperspective.data.CursorStore;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

@Mod(value = CombatPerspective.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CombatPerspective.MOD_ID, value = Dist.CLIENT)
public class CombatPerspectiveClient {

    public CombatPerspectiveClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CombatPerspective.LOGGER.info("客户端加载成功");
    }

    private static volatile float currentFov = 70.0F;

    public static void init() {
        NeoForge.EVENT_BUS.register(CombatPerspectiveClient.class);
    }

    public static float getCurrentFov() {
        return currentFov;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFovCompute(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null || mc.options.fov() == null) return;
        currentFov = mc.options.fov().get().intValue() * event.getNewFovModifier();
    }

    // ========== 渲染命中标记 ==========

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Vec3 camHit = CursorStore.getHitPos();
        if (camHit == null) return;

        Minecraft mc = Minecraft.getInstance();
        assert mc.player != null;
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();

        var poseStack = event.getPoseStack();
        var bufferSource = mc.renderBuffers().bufferSource();

        // ---- 白色十字：摄像机射线命中点 ----
        RenderSystem.disableDepthTest();
        poseStack.pushPose();
        poseStack.translate(camHit.x - camPos.x, camHit.y - camPos.y, camHit.z - camPos.z);
        Matrix4f mat = poseStack.last().pose();
        renderCross(bufferSource, mat, camPos, camHit, 0xFFFFFFFF);
        poseStack.popPose();

        // ---- 实体/方块选中框：玩家视线射线 ----
        LocalPlayer player = mc.player;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(10.0));

        // 方块检测
        ClipContext blockCtx = new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult blockHit = player.level().clip(blockCtx);

        // 实体检测
        AABB sweepBox = player.getBoundingBox().expandTowards(look.scale(10.0)).inflate(1.0);
        EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player.level(), player, eye, end, sweepBox,
                e -> !e.isSpectator() && e.isPickable());

        // 比较距离，取最近的
        double blockDist = blockHit.getType() == HitResult.Type.BLOCK
                ? eye.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
        double entityDist = entityHit != null
                ? eye.distanceToSqr(entityHit.getLocation()) : Double.MAX_VALUE;

        if (blockDist < entityDist && blockHit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = blockHit.getLocation();
            double dist = Math.sqrt(blockDist);
            boolean inRange = dist <= player.blockInteractionRange();
            int color = inRange ? 0xFFFFFF00 : 0xFFFF0000;

            poseStack.pushPose();
            poseStack.translate(hitPos.x - camPos.x, hitPos.y - camPos.y, hitPos.z - camPos.z);
            Matrix4f mat2 = poseStack.last().pose();
            renderFaceOutline(bufferSource, mat2, blockHit.getDirection(), blockHit.getBlockPos(), hitPos, color);
            poseStack.popPose();
        } else if (entityHit != null) {
            Entity target = entityHit.getEntity();
            double dist = Math.sqrt(entityDist);
            boolean inRange = dist <= player.blockInteractionRange();
            int color = inRange ? 0xFFFFFF00 : 0xFFFF0000;

            poseStack.pushPose();
            renderEntityAABB(bufferSource, poseStack, target, camPos, color);
            poseStack.popPose();
        }

        RenderSystem.enableDepthTest();
    }

    /** 面朝摄像头的十字 */
    private static void renderCross(MultiBufferSource.BufferSource src, Matrix4f mat,
                                    Vec3 camPos, Vec3 hitPos, int color) {
        VertexConsumer buf = src.getBuffer(RenderType.LINES);
        float s = 0.1F;

        // 摄像头 → 命中点方向
        Vec3 fwd = camPos.subtract(hitPos).normalize();
        // 右向量 = fwd × 世界Y
        Vec3 right = fwd.cross(new Vec3(0, 1, 0)).normalize();
        // 上向量 = 右 × fwd
        Vec3 up = right.cross(fwd).normalize();

        // 水平线
        buf.addVertex(mat, (float)(-right.x * s), (float)(-right.y * s), (float)(-right.z * s))
                .setColor(color).setNormal(0, 1, 0);
        buf.addVertex(mat, (float)( right.x * s), (float)( right.y * s), (float)( right.z * s))
                .setColor(color).setNormal(0, 1, 0);

        // 竖直线
        buf.addVertex(mat, (float)(-up.x * s), (float)(-up.y * s), (float)(-up.z * s))
                .setColor(color).setNormal(0, 1, 0);
        buf.addVertex(mat, (float)( up.x * s), (float)( up.y * s), (float)( up.z * s))
                .setColor(color).setNormal(0, 1, 0);
    }

    /** 方块面边框 — LINE_STRIP，颜色由调用方指定 */
    private static void renderFaceOutline(MultiBufferSource.BufferSource src, Matrix4f mat,
                                          Direction dir, BlockPos bp, Vec3 hit, int color) {
        if (dir == null) return;
        float[] c = faceCorners(dir, bp, hit);
        VertexConsumer buf = src.getBuffer(RenderType.LINE_STRIP);

        buf.addVertex(mat, c[0], c[1], c[2]).setColor(color).setNormal(0, 1, 0);
        buf.addVertex(mat, c[3], c[4], c[5]).setColor(color).setNormal(0, 1, 0);
        buf.addVertex(mat, c[6], c[7], c[8]).setColor(color).setNormal(0, 1, 0);
        buf.addVertex(mat, c[9], c[10], c[11]).setColor(color).setNormal(0, 1, 0);
        buf.addVertex(mat, c[0], c[1], c[2]).setColor(color).setNormal(0, 1, 0); // 闭合
    }

    /** 实体碰撞箱线框 */
    private static void renderEntityAABB(MultiBufferSource.BufferSource src,
                                         PoseStack ps, Entity entity, Vec3 camPos, int color) {
        AABB box = entity.getBoundingBox();
        Vec3 pos = entity.position();

        ps.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
        Matrix4f mat = ps.last().pose();

        float x1 = (float)(box.minX - pos.x);
        float y1 = (float)(box.minY - pos.y);
        float z1 = (float)(box.minZ - pos.z);
        float x2 = (float)(box.maxX - pos.x);
        float y2 = (float)(box.maxY - pos.y);
        float z2 = (float)(box.maxZ - pos.z);

        VertexConsumer buf = src.getBuffer(RenderType.LINES);

        // 底面
        line(buf, mat, x1, y1, z1, x2, y1, z1, color);
        line(buf, mat, x2, y1, z1, x2, y1, z2, color);
        line(buf, mat, x2, y1, z2, x1, y1, z2, color);
        line(buf, mat, x1, y1, z2, x1, y1, z1, color);
        // 顶面
        line(buf, mat, x1, y2, z1, x2, y2, z1, color);
        line(buf, mat, x2, y2, z1, x2, y2, z2, color);
        line(buf, mat, x2, y2, z2, x1, y2, z2, color);
        line(buf, mat, x1, y2, z2, x1, y2, z1, color);
        // 竖边
        line(buf, mat, x1, y1, z1, x1, y2, z1, color);
        line(buf, mat, x2, y1, z1, x2, y2, z1, color);
        line(buf, mat, x2, y1, z2, x2, y2, z2, color);
        line(buf, mat, x1, y1, z2, x1, y2, z2, color);
    }

    private static void line(VertexConsumer buf, Matrix4f mat,
                             float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        buf.addVertex(mat, x1, y1, z1).setColor(color).setNormal(0, 1, 0);
        buf.addVertex(mat, x2, y2, z2).setColor(color).setNormal(0, 1, 0);
    }

    /** 计算方块被命中面的四个角坐标（相对 hitPos，贴在表面上） */
    private static float[] faceCorners(Direction dir, BlockPos bp, Vec3 hit) {
        float hs = 0.5F;

        Direction.Axis axis = dir.getAxis();

        // 面中心 = 方块角 + 半格 + 面部方向 × 半格
        float cx = bp.getX() + 0.5F + dir.getStepX() * 0.5F - (float) hit.x;
        float cy = bp.getY() + 0.5F + dir.getStepY() * 0.5F - (float) hit.y;
        float cz = bp.getZ() + 0.5F + dir.getStepZ() * 0.5F - (float) hit.z;

        float ux, uy, uz; // "上"方向（面内的上）
        float vx, vy, vz; // "右"方向（面内的右）

        if (axis == Direction.Axis.Y) {
            // 顶面/底面：上=南(Z+)，右=东(X+)
            ux = 0; uy = 0; uz = hs;
            vx = hs; vy = 0; vz = 0;
        } else if (axis == Direction.Axis.X) {
            // 东/西面：上=上(Y+)，右=南(Z+)
            ux = 0; uy = hs; uz = 0;
            vx = 0; vy = 0; vz = hs;
        } else { // Z (南/北)
            // 上=上(Y+)，右=东(X+)
            ux = 0; uy = hs; uz = 0;
            vx = hs; vy = 0; vz = 0;
        }

        // 四个角：中心 ± u ± v
        return new float[]{
                cx - ux - vx, cy - uy - vy, cz - uz - vz,  // 左下
                cx - ux + vx, cy - uy + vy, cz - uz + vz,  // 右下
                cx + ux + vx, cy + uy + vy, cz + uz + vz,  // 右上
                cx + ux - vx, cy + uy - vy, cz + uz - vz,  // 左上
        };
    }
}