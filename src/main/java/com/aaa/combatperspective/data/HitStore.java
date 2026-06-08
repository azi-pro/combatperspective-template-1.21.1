package com.aaa.combatperspective.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** 射线命中信息 */
public class HitStore {
    private static Vec3 hitPos;
    private static Direction hitDir;
    private static BlockPos hitBlockPos;
    private static boolean hitBlock;

    public static void set(Vec3 pos, Direction dir, BlockPos blockPos, boolean isBlock) {
        hitPos = pos;
        hitDir = dir;
        hitBlockPos = blockPos;
        hitBlock = isBlock;
    }

    public static Vec3 getPos()         { return hitPos; }
    public static Direction getDir()    { return hitDir; }
    public static BlockPos getBlockPos(){ return hitBlockPos; }
    public static boolean isBlock()     { return hitBlock; }
}
