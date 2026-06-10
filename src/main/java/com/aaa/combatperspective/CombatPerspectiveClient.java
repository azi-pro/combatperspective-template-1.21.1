// =============================================================================
// CombatPerspectiveClient.java - 客户端模组主类（客户端入口 + 渲染）
// =============================================================================
// 包声明
package com.aaa.combatperspective;

// 导入 CursorStore 数据存储类，用于在 Mixin 之间共享数据
import com.aaa.combatperspective.data.HitStore;


// 导入 Blaze3D 渲染系统，用于控制深度测试等渲染状态
import com.mojang.blaze3d.systems.RenderSystem;

// 导入 Blaze3D 顶点数据类，用于构建几何图形
import com.mojang.blaze3d.vertex.*;

// 导入 Minecraft 客户端相机类，用于获取相机位置和状态
import net.minecraft.client.Camera;

// 导入 Minecraft 客户端主类，用于访问游戏实例
import net.minecraft.client.Minecraft;

// 导入本地玩家类，表示当前客户端的玩家
import net.minecraft.client.player.LocalPlayer;

// 导入弓物品类，用于判断拉弓状态
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;

// 导入多缓冲源类，用于渲染几何体到屏幕
import net.minecraft.client.renderer.MultiBufferSource;

// 导入渲染类型类，定义不同类型的渲染方式
import net.minecraft.client.renderer.RenderType;

// 导入方块位置类，表示三维坐标（整数）
import net.minecraft.core.BlockPos;

// 导入方向枚举，表示六个面方向（上/下/东/西/南/北）
import net.minecraft.core.Direction;

// 导入实体类，表示游戏中的实体（如生物、物品等）
import net.minecraft.world.entity.Entity;

// 导入射线检测上下文类，用于执行射线与方块的碰撞检测
import net.minecraft.world.level.ClipContext;

// 导入轴对齐包围盒类，用于实体碰撞检测
import net.minecraft.world.phys.AABB;

// 导入方块命中结果类，包含命中位置和方向信息
import net.minecraft.world.phys.BlockHitResult;

// 导入实体命中结果类，包含命中的实体信息
import net.minecraft.world.phys.EntityHitResult;

// 导入命中结果类型枚举（MISS/BLOCK/ENTITY）
import net.minecraft.world.phys.HitResult;

// 导入三维向量类，用于表示位置和方向
import net.minecraft.world.phys.Vec3;

// 导入分布标记注解，用于区分客户端/服务端代码
import net.neoforged.api.distmarker.Dist;

// 导入事件优先级常量
import net.neoforged.bus.api.EventPriority;

// 导入事件订阅注解
import net.neoforged.bus.api.SubscribeEvent;

// 导入模组容器类
import net.neoforged.fml.ModContainer;

// 导入事件总线订阅者注解，用于自动注册事件监听
import net.neoforged.fml.common.EventBusSubscriber;

// 导入 Mod 注解
import net.neoforged.fml.common.Mod;

// 导入客户端初始化事件
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

// 导入 FOV 计算事件，用于修改视野范围
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

// 导入渲染层级事件，用于在特定阶段绘制额外内容
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

// 导入配置屏幕类，用于显示游戏内配置界面
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

// 导入配置屏幕工厂接口
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// 导入 NeoForge 事件总线
import net.neoforged.neoforge.common.NeoForge;

// 导入 JOML 矩阵类，用于 3D 变换
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

// =============================================================================
// 客户端专用模组主类
// Mod 注解：dist = Dist.CLIENT 表示这仅在客户端加载
// EventBusSubscriber：自动将此类注册为事件监听器，value = Dist.CLIENT 表示仅客户端监听
// =============================================================================
@Mod(value = CombatPerspective.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CombatPerspective.MOD_ID, value = Dist.CLIENT)
public class CombatPerspectiveClient {

    // =========================================================================
    // 构造函数：客户端模组初始化入口
    // 用于注册配置屏幕扩展点
    // param container 模组容器
    // =========================================================================
    public CombatPerspectiveClient(ModContainer container) {
        // -------------------------------------------------------------------------
        // 注册配置屏幕扩展点
        // registerExtensionPoint 将配置界面注册到游戏中
        // IConfigScreenFactory 是配置屏幕工厂接口
        // ConfigurationScreen::new 是方法引用，创建默认的配置屏幕
        // 这样在游戏中按 ESC 打开菜单时可以看到模组的配置选项
        // -------------------------------------------------------------------------
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    // =========================================================================
    // 客户端设置事件处理方法
    // SubscribeEvent：订阅 FMLClientSetupEvent，在客户端初始化时调用
    // static 方法：事件监听器通常是静态的，以便被事件总线正确调用
    // =========================================================================
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CombatPerspective.LOGGER.info("客户端加载成功");
    }

    @SubscribeEvent
    static void registerKeys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(ADJUST_CAMERA_KEY);
    }

    // =========================================================================
    // 当前 FOV 缓存变量
    // volatile 关键字：保证多线程间的可见性，防止缓存问题
    // 用于存储当前的视野角度值
    // =========================================================================
    // ===== 自定义按键：按住调整摄像机 =====
    public static final net.minecraft.client.KeyMapping ADJUST_CAMERA_KEY = new net.minecraft.client.KeyMapping(
            "key.combatperspective.adjust_camera",
            org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.combatperspective"
    );

    private static volatile float currentFov = 70.0F;

    public static void init() {
        NeoForge.EVENT_BUS.register(CombatPerspectiveClient.class);
    }

    // =========================================================================
    // 获取当前 FOV 的 getter 方法
    // 供其他类调用以获取视野角度值
    // return 当前 FOV 值
    // =========================================================================
    public static float getCurrentFov() {
        return currentFov;
    }

    // =========================================================================
    // FOV 计算事件处理方法
    // SubscribeEvent(priority = EventPriority.LOWEST)：以最低优先级监听
    // 这样可以在其他修改 FOV 的代码之后执行
    // param event FOV 计算事件
    // =========================================================================
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFovCompute(ComputeFovModifierEvent event) {
        // 获取 Minecraft 实例
        Minecraft mc = Minecraft.getInstance();

        // 安全检查：确保 options 和 fov 不为空
        if (mc.options == null || mc.options.fov() == null) return;

        // 计算新的 FOV 值
        // mc.options.fov().get().intValue() 获取玩家设置的 FOV 值（整数）
        // event.getNewFovModifier() 获取已计算的 FOV 修正值
        // 两者相乘得到最终 FOV
        currentFov = mc.options.fov().get().intValue() * event.getNewFovModifier();
    }

    // =========================================================================
    // ===================== 渲染命中标记 =====================
    // 在游戏渲染世界的特定阶段绘制额外的视觉标记
    // =========================================================================

    // =========================================================================
    // 渲染层级事件处理方法
    // 在渲染世界的特定阶段被调用，用于绘制额外的 UI 元素
    // param event 渲染层级事件
    // =========================================================================
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // -------------------------------------------------------------------------
        // 阶段过滤：只处理实体渲染之后
        // AFTER_ENTITIES 阶段适合绘制覆盖在游戏世界上的元素
        // 这样标记会显示在实体和方块之上
        // -------------------------------------------------------------------------
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        // -------------------------------------------------------------------------
        // 从 CursorStore 获取鼠标射线命中位置
        // 如果没有命中（为 null），则不绘制
        // -------------------------------------------------------------------------
        Vec3 camHit = HitStore.getPos();
        if (camHit == null) return;

        // 获取 Minecraft 实例和玩家引用
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;


        // 获取主相机，用于计算相对坐标
        Camera cam = mc.gameRenderer.getMainCamera();

        // 获取相机在世界中的位置
        Vec3 camPos = cam.getPosition();

        // -------------------------------------------------------------------------
        // 获取渲染上下文
        // poseStack：坐标变换栈，用于设置绘制位置
        // bufferSource：顶点缓冲源，用于提交几何数据
        // -------------------------------------------------------------------------
        var poseStack = event.getPoseStack();
        var bufferSource = mc.renderBuffers().bufferSource();

        // -------------------------------------------------------------------------
        // ---- 第二部分：命中检测与渲染 ----
        // -------------------------------------------------------------------------

        // 获取当前玩家
        LocalPlayer player = mc.player;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);

        // ---- 弹射物轨迹检测（弓/弩/三叉戟/末影珍珠/雪球/鸡蛋/风弹） ----
        ProjectileParams params = getTrajectoryParams(player);

        if (params != null) {
            List<Vec3> traj = simulateTrajectory(player, params.v0, params.gravity, params.drag);
            renderEntitiesOnTrajectory(bufferSource, poseStack, camPos, player, traj);
            renderBowTrajectory(bufferSource, poseStack, camPos, player, traj);
        } else {
            // ---- 普通模式：10格射线 → 方块/实体命中框 ----
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
                boolean inRange = dist <= player.entityInteractionRange();
                int color = inRange ? 0xFFFFFF00 : 0xFFFF0000;
                poseStack.pushPose();
                renderEntityAABB(bufferSource, poseStack, target, camPos, color);
                poseStack.popPose();
            }
        }

        // 重新启用深度测试，恢复正常渲染
        RenderSystem.enableDepthTest();
    }

    // ==================== 弓轨迹预测线 ====================

    /** 弹射物物理参数 */
    private record ProjectileParams(double v0, double gravity, double drag) {}

    /**
     * 根据玩家手持/使用物品返回弹射物参数，没有匹配则返回 null。
     */
    private static ProjectileParams getTrajectoryParams(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack useItem = player.getUseItem();
        Item mainItem = mainHand.getItem();

        // ---- 使用中显示 ----
        if (player.isUsingItem()) {
            if (useItem.getItem() instanceof BowItem) {
                float power = BowItem.getPowerForTime(player.getTicksUsingItem());
                return new ProjectileParams(power * 3.0, 0.05, 0.99);
            }
            if (useItem.getItem() instanceof TridentItem) {
                // 有激流附魔时不投掷三叉戟
                var enchants = useItem.getEnchantments();
                var riptide = player.level().registryAccess()
                        .registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(Enchantments.RIPTIDE);
                if (enchants.getLevel(riptide) > 0)
                    return null;
                return new ProjectileParams(2.5, 0.05, 0.99);
            }
        }

        // ---- 手持时显示 ----
        if (mainItem instanceof CrossbowItem && CrossbowItem.isCharged(mainHand)) {
            return new ProjectileParams(3.15, 0.05, 0.99);
        }
        if (mainItem instanceof EnderpearlItem) {
            return new ProjectileParams(1.5, 0.03, 0.99);
        }
        if (mainItem instanceof SnowballItem) {
            return new ProjectileParams(1.5, 0.05, 0.99);
        }
        if (mainItem instanceof EggItem) {
            return new ProjectileParams(1.5, 0.05, 0.99);
        }
        if (mainItem instanceof WindChargeItem) {
            return new ProjectileParams(1.5, 0.0, 1.0); // 无重力无阻力 → 直线
        }

        return null;
    }

    /**
     * 模拟弹射物飞行轨迹，返回世界坐标点列表。
     * @param v0      初速 (block/tick)
     * @param gravity 重力 (block/tick²)，0 = 无重力直线
     * @param drag    空气阻力系数 (1.0 = 无阻力)
     */
    private static List<Vec3> simulateTrajectory(LocalPlayer player, double v0, double gravity, double drag) {
        List<Vec3> points = new ArrayList<>();
        if (v0 < 0.01) return points;

        double yr = Math.toRadians(player.getYRot());
        double pr = Math.toRadians(player.getXRot());
        double vx = -Math.sin(yr) * Math.cos(pr) * v0;
        double vy = -Math.sin(pr) * v0;
        double vz = Math.cos(yr) * Math.cos(pr) * v0;

        double px = player.getEyePosition().x;
        double py = player.getEyePosition().y;
        double pz = player.getEyePosition().z;
        points.add(new Vec3(px, py, pz));

        int maxTicks = (gravity == 0.0 && drag >= 1.0) ? 50 : 200; // 直线模式缩短

        for (int t = 0; t < maxTicks; t++) {
            vx *= drag;
            vy = vy * drag - gravity;
            vz *= drag;
            px += vx;
            py += vy;
            pz += vz;
            points.add(new Vec3(px, py, pz));
        }
        return points;
    }

    /** 检测轨迹穿过的所有实体，渲染红色 AABB 线框（不受 10 格攻击距离限制） */
    private static void renderEntitiesOnTrajectory(MultiBufferSource.BufferSource src, PoseStack ps,
                                                   Vec3 camPos, LocalPlayer player, List<Vec3> traj) {
        if (traj.isEmpty()) return;

        // 构建覆盖整条轨迹的包围盒
        Vec3 first = traj.get(0);
        double minX = first.x, minY = first.y, minZ = first.z;
        double maxX = first.x, maxY = first.y, maxZ = first.z;
        for (Vec3 p : traj) {
            if (p.x < minX) minX = p.x; if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y; if (p.y > maxY) maxY = p.y;
            if (p.z < minZ) minZ = p.z; if (p.z > maxZ) maxZ = p.z;
        }
        AABB trajBounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ).inflate(1.0);

        // 获取轨迹范围内的实体
        List<Entity> nearby = player.level().getEntitiesOfClass(Entity.class, trajBounds,
                e -> e != player && !e.isSpectator() && e.isPickable());

        // 检测每段轨迹是否与实体 AABB 相交
        for (Entity entity : nearby) {
            AABB box = entity.getBoundingBox();
            for (int i = 0; i < traj.size() - 1; i++) {
                if (box.clip(traj.get(i), traj.get(i + 1)).isPresent()) {
                    ps.pushPose();
                    renderEntityAABB(src, ps, entity, camPos, 0xFFFF0000);
                    ps.popPose();
                    break; // 命中一次就够了
                }
            }
        }
    }

    /** 模拟箭矢飞行轨迹并在世界中渲染线段 */
    private static void renderBowTrajectory(MultiBufferSource.BufferSource src, PoseStack ps,
                                            Vec3 camPos, LocalPlayer player, List<Vec3> traj) {
        if (traj.size() < 2) return;

        Vec3 eye = traj.get(0);
        Vec3 hit = HitStore.getPos();
        double hitDist = (hit != null)
                ? Math.sqrt((hit.x - eye.x) * (hit.x - eye.x) + (hit.z - eye.z) * (hit.z - eye.z))
                : Double.MAX_VALUE;

        ps.pushPose();
        Matrix4f mat = ps.last().pose();
        VertexConsumer buf = src.getBuffer(RenderType.LINES);

        for (int i = 0; i < traj.size() - 1; i++) {
            Vec3 a = traj.get(i);
            Vec3 b = traj.get(i + 1);

            // 绿 → 红渐变：距离越远准确率越低
            double curDist = Math.sqrt((b.x - eye.x) * (b.x - eye.x) + (b.z - eye.z) * (b.z - eye.z));
            double ratio = Math.min(curDist / Math.max(hitDist, 1.0), 1.0);
            int r = (int)(ratio * 255);
            int g = (int)((1.0 - ratio) * 255);
            int color = (0xFF << 24) | (r << 16) | (g << 8);

            buf.addVertex(mat, (float)(a.x - camPos.x), (float)(a.y - camPos.y), (float)(a.z - camPos.z))
               .setColor(color).setNormal(0, 1, 0);
            buf.addVertex(mat, (float)(b.x - camPos.x), (float)(b.y - camPos.y), (float)(b.z - camPos.z))
               .setColor(color).setNormal(0, 1, 0);

            if (curDist > hitDist + 10) break;
        }
        ps.popPose();
    }


    private static void addVertex(VertexConsumer buf, Matrix4f mat, float x, float y, float z, int color, float nx, float ny, float nz) {
        buf.addVertex(mat, x, y, z).setColor(color).setNormal(nx, ny, nz).setUv(0, 0).setUv2(240, 240);
    }


    // =========================================================================
    // 绘制方块被命中面的边框（LINE_STRIP 模式）
    // param src 顶点缓冲源
    // param mat 当前变换矩阵
    // param dir 命中的面方向
    // param bp 方块位置
    // param hit 命中位置
    // param color 颜色
    // =========================================================================
    private static void renderFaceOutline(MultiBufferSource.BufferSource src, Matrix4f mat,
                                          Direction dir, BlockPos bp, Vec3 hit, int color) {
        // 防御性检查：如果方向为 null 则不绘制
        if (dir == null) return;

        // 计算面四个角的坐标
        float[] c = faceCorners(dir, bp, hit);

        // 获取 LINE_STRIP 类型的顶点缓冲（连续线段）
        VertexConsumer buf = src.getBuffer(RenderType.LINE_STRIP);

        // 绘制四条边（四个角到四个角）
        // LINE_STRIP 模式：每个新顶点与前一个顶点形成线段
        buf.addVertex(mat, c[0], c[1], c[2]).setColor(color).setNormal(0, 1, 0).setUv(0, 0).setUv2(240, 240);
        buf.addVertex(mat, c[3], c[4], c[5]).setColor(color).setNormal(0, 1, 0).setUv(0, 0).setUv2(240, 240);
        buf.addVertex(mat, c[6], c[7], c[8]).setColor(color).setNormal(0, 1, 0).setUv(0, 0).setUv2(240, 240);
        buf.addVertex(mat, c[9], c[10], c[11]).setColor(color).setNormal(0, 1, 0).setUv(0, 0).setUv2(240, 240);
        buf.addVertex(mat, c[0], c[1], c[2]).setColor(color).setNormal(0, 1, 0).setUv(0, 0).setUv2(240, 240);
    }

    // =========================================================================
    // 绘制实体轴对齐包围盒（AABB）的线框
    // param src 顶点缓冲源
    // param ps 坐标变换栈
    // param entity 被绘制线框的实体
    // param camPos 相机位置（用于相对坐标计算）
    // param color 颜色
    // =========================================================================
    private static void renderEntityAABB(MultiBufferSource.BufferSource src,
                                         PoseStack ps, Entity entity, Vec3 camPos, int color) {
        // 获取实体的碰撞箱（边界框）
        AABB box = entity.getBoundingBox();

        // 获取实体的位置
        Vec3 pos = entity.position();

        // 将坐标原点移动到实体位置（相对于相机）
        ps.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);

        // 获取当前变换矩阵
        Matrix4f mat = ps.last().pose();

        // 计算包围盒相对于实体位置的坐标
        float x1 = (float)(box.minX - pos.x); // 最小X
        float y1 = (float)(box.minY - pos.y); // 最小Y
        float z1 = (float)(box.minZ - pos.z); // 最小Z
        float x2 = (float)(box.maxX - pos.x); // 最大X
        float y2 = (float)(box.maxY - pos.y); // 最大Y
        float z2 = (float)(box.maxZ - pos.z); // 最大Z

        // 获取线条类型的顶点缓冲
        VertexConsumer buf = src.getBuffer(RenderType.LINES);


        line(buf, mat, x2, y2, z2, x1, y2, z2, color); // 后边
        line(buf, mat, x1, y2, z2, x1, y2, z1, color); // 左边

        // -------------------------------------------------------------------------
        // 绘制四条竖直边（连接底面和顶面）
        // -------------------------------------------------------------------------
        line(buf, mat, x1, y1, z1, x1, y2, z1, color); // 左前边
        line(buf, mat, x2, y1, z1, x2, y2, z1, color); // 右前边
        line(buf, mat, x2, y1, z2, x2, y2, z2, color); // 右后边
        line(buf, mat, x1, y1, z2, x1, y2, z2, color); // 左后边
    }

    // =========================================================================
    // 绘制单条线段的辅助方法
    // param buf 顶点缓冲
    // param mat 变换矩阵
    // param x1/y1/z1 起点坐标
    // param x2/y2/z2 终点坐标
    // param color 颜色
    // =========================================================================
    private static void line(VertexConsumer buf, Matrix4f mat,
                             float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        buf.addVertex(mat, x1, y1, z1).setColor(color).setNormal(0, 1, 0).setUv(0, 0).setUv2(240, 240);
        buf.addVertex(mat, x2, y2, z2).setColor(color).setNormal(0, 1, 0).setUv(0, 0).setUv2(240, 240);
    }

    // =========================================================================
    // 计算方块被命中面的四个角坐标（相对于命中点，贴在表面上）
    // param dir 面的朝向方向
    // param bp 方块位置
    // param hit 命中位置
    // return 包含四个角的 float 数组 [x1,y1,z1, x2,y2,z2, x3,y3,z3, x4,y4,z4]
    // =========================================================================
    private static float[] faceCorners(Direction dir, BlockPos bp, Vec3 hit) {
        // 半格大小常量，用于计算面的边界
        float hs = 0.5F;

        // 获取面所在轴（X轴、Y轴或Z轴）
        Direction.Axis axis = dir.getAxis();

        // -------------------------------------------------------------------------
        // 计算面中心点坐标（相对于命中点）
        // bp.getX() + 0.5F：方块中心X坐标
        // dir.getStepX() * 0.5F：沿面方向偏移半格
        // - (float) hit.x：减去命中点的X坐标，得到相对坐标
        // -------------------------------------------------------------------------
        float cx = bp.getX() + 0.5F + dir.getStepX() * 0.5F - (float) hit.x;
        float cy = bp.getY() + 0.5F + dir.getStepY() * 0.5F - (float) hit.y;
        float cz = bp.getZ() + 0.5F + dir.getStepZ() * 0.5F - (float) hit.z;

        // 定义面内的"上"方向和"右"方向向量
        float ux, uy, uz; // "上"方向（面内的上）
        float vx, vy, vz; // "右"方向（面内的右）

        // -------------------------------------------------------------------------
        // 根据面的轴向确定 UV 方向
        // 不同朝向的面需要不同的 UV 方向来保证贴图正确
        // -------------------------------------------------------------------------
        if (axis == Direction.Axis.Y) {
            // 顶面/底面（朝上或朝下）
            // 上方向 = 南(Z+)，右方向 = 东(X+)
            ux = 0; uy = 0; uz = hs;
            vx = hs; vy = 0; vz = 0;
        } else if (axis == Direction.Axis.X) {
            // 东/西面（朝东或朝西）
            // 上方向 = 上(Y+)，右方向 = 南(Z+)
            ux = 0; uy = hs; uz = 0;
            vx = 0; vy = 0; vz = hs;
        } else { // Z轴（南/北面）
            // 上方向 = 上(Y+)，右方向 = 东(X+)
            ux = 0; uy = hs; uz = 0;
            vx = hs; vy = 0; vz = 0;
        }

        // -------------------------------------------------------------------------
        // 计算四个角坐标
        // 中心 ± 上向量 ± 右向量
        // 左下 = 中心 - 上 - 右
        // 右下 = 中心 - 上 + 右
        // 右上 = 中心 + 上 + 右
        // 左上 = 中心 + 上 - 右
        // -------------------------------------------------------------------------
        return new float[]{
                cx - ux - vx, cy - uy - vy, cz - uz - vz,
                cx - ux + vx, cy - uy + vy, cz - uz + vz,
                cx + ux + vx, cy + uy + vy, cz + uz + vz,
                cx + ux - vx, cy + uy - vy, cz + uz - vz,
        };
    }

}
