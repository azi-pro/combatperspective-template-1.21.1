package com.aaa.combatperspective.data;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

/**
 * 投射物物理计算
 * 使用 Minecraft 1.21 真实的弓箭物理参数
 */
public class ProjectilePhysics {
    
    // =========================================================================
    // Minecraft 1.21 弓箭真实物理参数
    // =========================================================================
    
    /** 弓箭基础重力 (每 tick) - Minecraft Arrow 实体使用 */
    private static final double ARROW_GRAVITY = 0.05;
    
    /** 弓箭拖曳系数 */
    private static final double ARROW_DRAG = 0.99;
    
    /** 满蓄力弓箭初速度 (格/tick) - Minecraft 1.21 Arrow 实际值 */
    private static final double ARROW_MAX_POWER = 3.0;
    
    /** 每 tick 重力加速度（用于更精确模拟） */
    private static final double GRAVITY_PER_TICK = 0.0075;
    
    /**
     * 计算投射物轨迹
     * @param player 玩家
     * @param bowPower 弓的蓄力 (0.0 - 1.0)
     * @return 轨迹点列表
     */
    public static java.util.List<ProjectileStore.TrajectoryPoint> calculateTrajectory(
            LocalPlayer player, float bowPower) {
        
        java.util.List<ProjectileStore.TrajectoryPoint> points = new java.util.ArrayList<>();
        
        if (player == null) return points;
        
        // 获取投射物类型
        ProjectileStore.ProjectileType type = ProjectileStore.getProjectileType();
        
        // 获取物理参数
        double maxPower;
        double gravity;
        double drag;
        
        switch (type) {
            case ARROW -> {
                // 弓箭：使用 Minecraft 真实物理
                maxPower = ARROW_MAX_POWER * bowPower;
                gravity = GRAVITY_PER_TICK;
                drag = 1.0; // 弓箭无拖曳
            }
            case TRIDENT -> {
                // 三叉戟
                maxPower = 2.5 * bowPower;
                gravity = 0.006;
                drag = 1.0;
            }
            case SNOWBALL, EGG, ENDER_PEARL -> {
                // 雪球/鸡蛋/末影珍珠
                maxPower = 1.5 * bowPower;
                gravity = 0.03;
                drag = 0.98;
            }
            case POTION -> {
                // 药水
                maxPower = 0.5 * bowPower;
                gravity = 0.04;
                drag = 0.95;
            }
            default -> {
                maxPower = 1.0 * bowPower;
                gravity = 0.05;
                drag = 0.99;
            }
        }
        
        // 获取玩家眼睛位置和朝向
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        
        // 初始速度 = 方向 * 功率
        double vx = lookVec.x * maxPower;
        double vy = lookVec.y * maxPower;
        double vz = lookVec.z * maxPower;
        
        // 当前位置 = 眼睛位置
        double x = eyePos.x;
        double y = eyePos.y;
        double z = eyePos.z;
        
        // 上一帧位置
        Vec3 lastPos = eyePos;
        
        Level level = player.level();
        int maxPoints = ProjectileStore.getPredictionPoints();
        float maxDist = (float) ProjectileStore.getMaxDistance();
        
        // 逐 tick 模拟
        for (int i = 0; i < maxPoints; i++) {
            // 添加当前轨迹点
            Vec3 pos = new Vec3(x, y, z);
            points.add(new ProjectileStore.TrajectoryPoint(pos, i, false));
            
            // 上一帧位置
            lastPos = pos;
            
            // ===============================================================
            // Minecraft 物理更新
            // ===============================================================
            
            // 应用重力（每 tick）
            vy -= gravity;
            
            // 应用拖曳
            if (drag < 1.0) {
                vx *= drag;
                vz *= drag;
            }
            
            // 更新位置
            x += vx;
            y += vy;
            z += vz;
            
            // 新位置
            Vec3 newPos = new Vec3(x, y, z);
            
            // 碰撞检测
            HitResult hit = checkCollision(level, lastPos, newPos, player);
            
            if (hit != null) {
                // 找到落点
                Vec3 hitPos = hit.getLocation();
                points.add(new ProjectileStore.TrajectoryPoint(hitPos, i + 1, true));
                
                net.minecraft.core.BlockPos blockPos;
                if (hit instanceof BlockHitResult blockHit) {
                    blockPos = blockHit.getBlockPos();
                } else {
                    blockPos = new net.minecraft.core.BlockPos((int)hitPos.x, (int)hitPos.y, (int)hitPos.z);
                }
                
                ProjectileStore.update(hitPos, blockPos, points);
                return points;
            }
            
            // 检查距离限制
            if (eyePos.distanceTo(newPos) > maxDist) {
                ProjectileStore.update(null, null, points);
                return points;
            }
            
            // 低于世界底部
            if (y < -64) {
                ProjectileStore.update(new Vec3(x, -64, z), 
                    new net.minecraft.core.BlockPos((int)x, -64, (int)z), points);
                return points;
            }
        }
        
        // 超时
        ProjectileStore.update(null, null, points);
        return points;
    }
    
    /**
     * 检测两点之间的碰撞（线段检测）
     */
    private static HitResult checkCollision(Level level, Vec3 start, Vec3 end, Entity exclude) {
        
        // 方块碰撞检测
        ClipContext ctx = new ClipContext(
            start, end,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            exclude
        );
        
        BlockHitResult blockHit = level.clip(ctx);
        
        // 实体碰撞检测
        AABB sweepBox = new AABB(start, end).inflate(0.3);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            level, exclude,
            start, end,
            sweepBox,
            e -> !e.isSpectator() && e.isPickable() && e != exclude
        );
        
        // 比较哪个更近
        double blockDist = blockHit.getType() == HitResult.Type.BLOCK
            ? start.distanceToSqr(blockHit.getLocation()) : Double.MAX_VALUE;
        double entityDist = entityHit != null
            ? start.distanceToSqr(entityHit.getLocation()) : Double.MAX_VALUE;
        
        if (entityDist < blockDist && entityHit != null) {
            return entityHit;
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            return blockHit;
        }
        
        return null;
    }
    
    /**
     * 根据弓的蓄力计算速度
     * Minecraft 1.21 弓箭蓄力公式（非线性）
     */
    public static float calculateBowPower(LocalPlayer player) {
        if (player == null) return 0.0f;
        
        // 检测是否在使用弓/弩
        if (!player.isUsingItem()) {
            return 0.0f;
        }
        
        var useItem = player.getUseItem();
        var item = useItem.getItem();
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
        
        // 只对弓、弩显示轨迹
        if (!itemId.contains("bow") && !itemId.contains("crossbow")) {
            return 0.0f;
        }
        
        // 获取蓄力时间 (ticks)
        int useTime = player.getUseItemRemainingTicks();
        
        if (useTime <= 0) {
            return 0.0f;
        }
        
        // Minecraft 弓最大蓄力时间约 1.0 秒 (20 ticks)
        float maxChargeTime = 20.0f;
        
        // 蓄力比例 (0.0 - 1.0)
        float chargeRatio = Math.min(1.0f, useTime / maxChargeTime);
        
        // ===============================================================
        // Minecraft 非线性蓄力公式
        // 来源: net.minecraft.world.item.BowItem
        // f = (f^2 + f*2) / 3
        // 这使得初期蓄力更快，后期较慢
        // ===============================================================
        float power = (chargeRatio * chargeRatio + chargeRatio * 2.0f) / 3.0f;
        
        // 限制最大为 1.0
        if (power > 1.0f) {
            power = 1.0f;
        }
        
        return power;
    }
}
