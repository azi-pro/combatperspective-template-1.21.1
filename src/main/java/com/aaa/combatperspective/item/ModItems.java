// =============================================================================
// ModItems.java - 物品注册类
// =============================================================================
// 包声明
package com.aaa.combatperspective.item;

// 导入模组主类，用于获取 MOD_ID
import com.aaa.combatperspective.CombatPerspective;

// 导入方块标签
import net.minecraft.tags.BlockTags;

// 导入基础物品类
import net.minecraft.world.item.Item;

// 导入剑物品类
import net.minecraft.world.item.SwordItem;

// 导入事件总线接口
import net.neoforged.bus.api.IEventBus;

// 导入延迟物品持有者
import net.neoforged.neoforge.registries.DeferredItem;

// 导入延迟注册器
import net.neoforged.neoforge.registries.DeferredRegister;

// 导入方块标签键
import net.minecraft.tags.TagKey;

// 导入物品合成原料
import net.minecraft.world.item.crafting.Ingredient;

// 导入工具等级接口
import net.minecraft.world.item.Tier;

// 导入铁锭物品
import net.minecraft.world.item.Items;

// 导入方块类
import net.minecraft.world.level.block.Block;

// =============================================================================
// 武器工具等级枚举
// 实现 Tier 接口，定义自定义武器的材料属性
// =============================================================================
enum CPWeaponTier implements Tier
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
    CPWeaponTier(int uses, float speed, float attackDamage, int enchantmentValue) {
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

// =============================================================================
// 物品注册类
// 使用延迟注册模式，在游戏初始化时注册所有物品
// =============================================================================
public class ModItems {
    // =========================================================================
    // 创建延迟物品注册器
    // DeferredRegister.createItems() 创建物品类型的延迟注册器
    // 参数为模组 ID，用于所有注册物品的命名空间
    // =========================================================================
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CombatPerspective.MOD_ID);

    // =========================================================================
    // 注册铁制长剑
    // ITEMS.register() 注册一个物品到游戏注册表
    // "weapon/iron_longsword"：物品的资源路径（完整为 combatperspective:weapon/iron_longsword）
    // () -> new SwordItem(...)：物品工厂 lambda 表达式
    // SwordItem：剑物品类，自带攻击动画和耐久消耗
    // CPWeaponTier.LONG_SWORD：使用的工具等级
    // .attributes()：设置物品的属性（伤害、速度等）
    // .stacksTo(1)：最大堆叠数量为 1（剑不能堆叠）
    // =========================================================================
    public static final DeferredItem<Item> Iron_LongSword = ITEMS.register("weapon/iron_longsword",
        () -> new SwordItem(CPWeaponTier.LONG_SWORD, new Item.Properties()
                .attributes(SwordItem.createAttributes(CPWeaponTier.LONG_SWORD, 1, -2.1F))
                .stacksTo(1)
        )
    );

    // =========================================================================
    // 注册方法：将注册器绑定到事件总线
    // param eventBus 模组事件总线
    // =========================================================================
    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}