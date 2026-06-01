// =============================================================================
// CombatPerspectiveClient.java - 客户端模组主类（客户端入口 + 渲染）
// =============================================================================
// 包声明
package com.aaa.combatperspective;

// 导入 CursorStore 数据存储类，用于在 Mixin 之间共享数据
import com.aaa.combatperspective.data.CursorStore;

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
        // 输出日志，表示客户端模组加载成功
        CombatPerspective.LOGGER.info("客户端加载成功");
    }

    // =========================================================================
    // 当前 FOV 缓存变量
    // volatile 关键字：保证多线程间的可见性，防止缓存问题
    // 用于存储当前的视野角度值
    // =========================================================================
    private static volatile float currentFov = 70.0F;

    // =========================================================================
    // 初始化方法：手动注册类到事件总线
    // 用于确保静态方法可以响应事件
    // =========================================================================
    public static void init() {
        // 将本类注册到 NeoForge 全局事件总线
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
        Vec3 camHit = CursorStore.getHitPos();
        if (camHit == null) return;

        // 获取 Minecraft 实例和玩家引用
        Minecraft mc = Minecraft.getInstance();
        assert mc.player != null;

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
        // ---- 第一部分：绘制白色十字标记 ----
        // 这个十字表示摄像机射线与命中点的位置
        // 十字永远面向摄像头方向
        // -------------------------------------------------------------------------

        // 禁用深度测试，使十字始终可见（不被方块遮挡）
        RenderSystem.disableDepthTest();

        // 保存当前坐标变换状态
        poseStack.pushPose();

        // 将坐标原点移动到命中点位置（相对于相机）
        // 这样十字会绘制在命中点位置
        poseStack.translate(camHit.x - camPos.x, camHit.y - camPos.y, camHit.z - camPos.z);

        // 获取当前变换矩阵
        Matrix4f mat = poseStack.last().pose();

        // 绘制十字：0xFFFFFFFF = 白色（完全不透明）
        renderCross(bufferSource, mat, camPos, camHit, 0xFFFFFFFF);

        // 恢复坐标变换状态
        poseStack.popPose();

        // -------------------------------------------------------------------------
        // ---- 第二部分：绘制玩家视线射线命中框 ----
        // 根据玩家朝向（鼠标控制）绘制方块或实体选中框
        // -------------------------------------------------------------------------

        // 获取当前玩家
        LocalPlayer player = mc.player;

        // 计算玩家眼睛位置
        Vec3 eye = player.getEyePosition();

        // 计算玩家视线方向向量（1.0F 表示使用完整视角范围）
        Vec3 look = player.getViewVector(1.0F);

        // 计算射线终点：眼睛位置 + 方向 * 10 格距离
        Vec3 end = eye.add(look.scale(10.0));

        // -------------------------------------------------------------------------
        // 方块碰撞检测
        // ClipContext：射线检测上下文
        // OUTLINE：只检测方块轮廓（不填充）
        // NONE：不检测液体
        // player：检测实体，用于忽略玩家自身的碰撞
        // -------------------------------------------------------------------------
        ClipContext blockCtx = new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);

        // 执行方块射线检测，返回最近的命中结果
        BlockHitResult blockHit = player.level().clip(blockCtx);

        // -------------------------------------------------------------------------
        // 实体碰撞检测
        // 首先定义一个扫描包围盒，涵盖玩家前方的区域
        // expandTowards：根据视线方向扩展包围盒
        // inflate：扩大 1 格，确保能检测到实体
        // -------------------------------------------------------------------------
        AABB sweepBox = player.getBoundingBox().expandTowards(look.scale(10.0)).inflate(1.0);

        // 使用 ProjectileUtil 的实体扫描方法
        // lambda 表达式过滤条件：不是旁观者且可被拾取
        EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player.level(), player, eye, end, sweepBox,
                e -> !e.isSpectator() && e.isPickable());

        // -------------------------------------------------------------------------
        // 比较方块和实体哪个更近
        // distanceToSqr：计算距离的平方（避免开方运算，提高性能）
        // -------------------------------------------------------------------------
        double blockDist = blockHit.getType() == HitResult.Type.BLOCK
                ? eye.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
        double entityDist = entityHit != null
                ? eye.distanceToSqr(entityHit.getLocation()) : Double.MAX_VALUE;

        // -------------------------------------------------------------------------
        // 根据最近的命中结果绘制选中框
        // -------------------------------------------------------------------------

        // 如果方块更近且确实命中了方块
        if (blockDist < entityDist && blockHit.getType() == HitResult.Type.BLOCK) {
            // 获取命中位置
            Vec3 hitPos = blockHit.getLocation();

            // 计算实际距离（开方）
            double dist = Math.sqrt(blockDist);

            // 判断是否在交互范围内
            boolean inRange = dist <= player.blockInteractionRange();

            // 在范围内显示黄色，超出范围显示红色
            int color = inRange ? 0xFFFFFF00 : 0xFFFF0000;

            // 保存坐标变换状态
            poseStack.pushPose();

            // 将原点移动到命中方块位置
            poseStack.translate(hitPos.x - camPos.x, hitPos.y - camPos.y, hitPos.z - camPos.z);

            // 获取变换矩阵
            Matrix4f mat2 = poseStack.last().pose();

            // 绘制方块面边框
            renderFaceOutline(bufferSource, mat2, blockHit.getDirection(), blockHit.getBlockPos(), hitPos, color);

            // 恢复坐标变换
            poseStack.popPose();
        }
        // 否则如果实体更近（或存在）
        else if (entityHit != null) {
            // 获取被命中的实体
            Entity target = entityHit.getEntity();

            // 计算实际距离
            double dist = Math.sqrt(entityDist);

            // 判断是否在交互范围内
            boolean inRange = dist <= player.blockInteractionRange();

            // 在范围内显示黄色，超出范围显示红色
            int color = inRange ? 0xFFFFFF00 : 0xFFFF0000;

            // 保存坐标变换状态
            poseStack.pushPose();

            // 绘制实体包围盒线框
            renderEntityAABB(bufferSource, poseStack, target, camPos, color);

            // 恢复坐标变换
            poseStack.popPose();
        }

        // 重新启用深度测试，恢复正常渲染
        RenderSystem.enableDepthTest();
    }

    // =========================================================================
    // 绘制面朝摄像头的十字标记
    // param src 顶点缓冲源
    // param mat 当前变换矩阵
    // param camPos 相机位置
    // param hitPos 命中位置
    // param color 颜色（ARGB格式）
    // =========================================================================
    private static void renderCross(MultiBufferSource.BufferSource src, Matrix4f mat,
                                    Vec3 camPos, Vec3 hitPos, int color) {
        // 获取线条类型的顶点缓冲
        VertexConsumer buf = src.getBuffer(RenderType.LINES);

        // 十字大小（0.1 格）
        float s = 0.1F;

        // 计算从命中点指向相机的方向（十字朝向）
        Vec3 fwd = camPos.subtract(hitPos).normalize();

        // 计算右向量：forward × 世界Y轴（叉积得到垂直方向）
        Vec3 right = fwd.cross(new Vec3(0, 1, 0)).normalize();

        // 计算上向量：right × forward（得到垂直于两者的方向）
        Vec3 up = right.cross(fwd).normalize();

        // -------------------------------------------------------------------------
        // 绘制水平线
        // 从左到右的线段
        // buf.addVertex：添加一个顶点，包含位置、颜色和法线
        // setColor：设置顶点颜色
        // setNormal：设置法线（用于光照计算）
        // -------------------------------------------------------------------------
        buf.addVertex(mat, (float)(-right.x * s), (float)(-right.y * s), (float)(-right.z * s))
                .setColor(color).setNormal(0, 1, 0);
        buf.addVertex(mat, (float)( right.x * s), (float)( right.y * s), (float)( right.z * s))
                .setColor(color).setNormal(0, 1, 0);

        // -------------------------------------------------------------------------
        // 绘制竖直线
        // 从上到下的线段
        // -------------------------------------------------------------------------
        buf.addVertex(mat, (float)(-up.x * s), (float)(-up.y * s), (float)(-up.z * s))
                .setColor(color).setNormal(0, 1, 0);
        buf.addVertex(mat, (float)( up.x * s), (float)( up.y * s), (float)( up.z * s))
                .setColor(color).setNormal(0, 1, 0);
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
        buf.addVertex(mat, c[0], c[1], c[2]).setColor(color).setNormal(0, 1, 0); // 左下
        buf.addVertex(mat, c[3], c[4], c[5]).setColor(color).setNormal(0, 1, 0); // 右下
        buf.addVertex(mat, c[6], c[7], c[8]).setColor(color).setNormal(0, 1, 0); // 右上
        buf.addVertex(mat, c[9], c[10], c[11]).setColor(color).setNormal(0, 1, 0); // 左上
        buf.addVertex(mat, c[0], c[1], c[2]).setColor(color).setNormal(0, 1, 0); // 回到起点闭合
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

        // -------------------------------------------------------------------------
        // 绘制底面（Y = y1 的四个边）
        // -------------------------------------------------------------------------
        line(buf, mat, x1, y1, z1, x2, y1, z1, color); // 前边
        line(buf, mat, x2, y1, z1, x2, y1, z2, color); // 右边
        line(buf, mat, x2, y1, z2, x1, y1, z2, color); // 后边
        line(buf, mat, x1, y1, z2, x1, y1, z1, color); // 左边

        // -------------------------------------------------------------------------
        // 绘制顶面（Y = y2 的四个边）
        // -------------------------------------------------------------------------
        line(buf, mat, x1, y2, z1, x2, y2, z1, color); // 前边
        line(buf, mat, x2, y2, z1, x2, y2, z2, color); // 右边
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
        // 添加线段起点
        buf.addVertex(mat, x1, y1, z1).setColor(color).setNormal(0, 1, 0);
        // 添加线段终点
        buf.addVertex(mat, x2, y2, z2).setColor(color).setNormal(0, 1, 0);
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
                cx - ux - vx, cy - uy - vy, cz - uz - vz,  // 左下
                cx - ux + vx, cy - uy + vy, cz - uz + vz,  // 右下
                cx + ux + vx, cy + uy + vy, cz + uz + vz,  // 右上
                cx + ux - vx, cy + uy - vy, cz + uz - vz,  // 左上
        };
    }
}