package com.aaa.combatperspective.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

enum CPTier implements Tier
{
    // =========================================================================
    // 长剑属性定义
    // 参数：耐久度、使用速度、攻击伤害、附魔值
    // LONG_SWORD(243, 6.0F, 3.0F, 15)
    // 243：耐久度（比铁剑略低）
    // 6.0F：攻击速度（铁剑是 1.6，钻石是 4.0）
    // 3.0F：额外攻击伤害（基础伤害 = 工具等级基础 + 额外伤害）
    // 15：附魔值（影响能附什么魔）
    // =========================================================================
    LONG_SWORD(243, 6.0F, 3.0F, 15);

    // =========================================================================
    // 工具等级接口要求的属性
    // =========================================================================
    private final int uses;             // 最大耐久度
    private final float speed;          // 攻击速度
    private final float attackDamage;   // 额外攻击伤害
    private final int enchantmentValue; // 附魔值

    // =========================================================================
    // 枚举构造函数
    // param uses 耐久度
    // param speed 攻击速度
    // param attackDamage 额外伤害
    // param enchantmentValue 附魔值
    // =========================================================================
    CPTier(int uses, float speed, float attackDamage, int enchantmentValue) {
        this.uses = uses;
        this.speed = speed;
        this.attackDamage = attackDamage;
        this.enchantmentValue = enchantmentValue;
    }

    // =========================================================================
    // 获取最大耐久度
    // Override：实现 Tier 接口的方法
    // return 最大使用次数
    // =========================================================================
    @Override public int getUses()
    {return uses;}

    // =========================================================================
    // 获取攻击速度
    // Override：实现 Tier 接口的方法
    // return 攻击速度值
    // =========================================================================
    @Override public float getSpeed()
    {return speed;}

    // =========================================================================
    // 获取额外攻击伤害
    // Override：实现 Tier 接口的方法
    // return 额外伤害值
    // =========================================================================
    @Override public float getAttackDamageBonus()
    {return attackDamage;}

    // =========================================================================
    // 获取不适合作为工具的方块标签
    // Override：实现 Tier 接口的方法
    // return 铁工具不能使用的方块标签
    // =========================================================================
    @Override public TagKey<Block> getIncorrectBlocksForDrops()
    {return BlockTags.INCORRECT_FOR_IRON_TOOL;}

    // =========================================================================
    // 获取附魔值
    // Override：实现 Tier 接口的方法
    // return 附魔值
    // =========================================================================
    @Override public int getEnchantmentValue()
    {return enchantmentValue;}

    // =========================================================================
    // 获取修复原料
    // Override：实现 Tier 接口的方法
    // return 铁锭作为修复材料
    // =========================================================================
    @Override public Ingredient getRepairIngredient()
    {return Ingredient.of(Items.IRON_INGOT);}
}
