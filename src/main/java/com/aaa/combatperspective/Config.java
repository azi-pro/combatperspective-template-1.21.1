// =============================================================================
// Config.java - 配置文件定义
// =============================================================================
// 包声明
package com.aaa.combatperspective;

// 导入 Java 列表接口
import java.util.List;

// 导入 Minecraft 内置注册表，用于验证物品名称
import net.minecraft.core.registries.BuiltInRegistries;

// 导入资源位置类，用于解析物品的资源键
import net.minecraft.resources.ResourceLocation;

// 导入基础物品类
import net.minecraft.world.item.Item;

// 导入事件订阅注解
import net.neoforged.bus.api.SubscribeEvent;

// 导入事件总线订阅者注解
import net.neoforged.fml.common.EventBusSubscriber;

// 导入配置加载/重新加载事件
import net.neoforged.fml.event.config.ModConfigEvent;

// 导入 NeoForge 配置规格构建器
import net.neoforged.neoforge.common.ModConfigSpec;

// =============================================================================
// 配置类：定义模组的配置选项
// 使用 ModConfigSpec 构建器模式定义各种配置项
// 配置会自动保存到 config/xxx-common.toml 文件中
// =============================================================================
public class Config {
    // =========================================================================
    // 配置规格构建器
    // BUILDER 负责收集所有配置项的定义并构建最终的 SPEC
    // =========================================================================
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // =========================================================================
    // 原有配置项：是否在通用设置时打印泥土方块信息
    // ModConfigSpec.BooleanValue：布尔类型配置
    // .comment()：配置的注释说明（游戏中可以看到）
    // .define()：定义配置项，参数为默认值
    // =========================================================================
    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup") // 配置项的说明
            .define("logDirtBlock", true); // 默认值为 true

    // =========================================================================
    // 原有配置项：魔法数字
    // ModConfigSpec.IntValue：整数类型配置
    // .defineInRange()：定义带范围的整数，参数为 名称、默认值、最小值、最大值
    // =========================================================================
    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number") // 配置项的说明
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE); // 范围 0 到 最大整数

    // =========================================================================
    // 原有配置项：魔法数字的介绍文本
    // ModConfigSpec.ConfigValue<String>：字符串类型配置
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number") // 说明
            .define("magicNumberIntroduction", "The magic number is... "); // 默认文本

    // =========================================================================
    // 原有配置项：物品字符串列表
    // ModConfigSpec.ConfigValue<List<? extends String>>：字符串列表配置
    // .defineListAllowEmpty()：定义允许为空的列表
    //   参数1：配置键名
    //   参数2：默认值（空的字符串列表）
    //   参数3：空值工厂（返回空字符串）
    //   参数4：验证器（检查每个元素是否为有效物品名）
    // =========================================================================
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.") // 说明
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    // =========================================================================
    // ===================== 战斗视角配置 =====================
    // 以下是本模组的核心配置项
    // =========================================================================

    // =========================================================================
    // 配置项：是否启用触及相机
    // 触及相机功能：摄像机朝向最远触及点
    // =========================================================================
    public static final ModConfigSpec.BooleanValue ENABLE_REACH_CAMERA = BUILDER
            .comment("启用特殊视角：摄像机朝向最远触及点") // 中文说明
            .define("enableReachCamera", false); // 默认关闭

    // =========================================================================
    // 配置项：最远触及距离
    // 定义玩家能够交互的最大距离（方块/实体）
    // DoubleValue：双精度浮点数配置
    // =========================================================================
    public static final ModConfigSpec.DoubleValue REACH_DISTANCE = BUILDER
            .comment("最远触及距离") // 说明
            .defineInRange("reachDistance", 6.0D, 1.0D, 20.0D); // 默认 6 格，范围 1-20

    // =========================================================================
    // 配置项：相机距离玩家
    // 第三人称视角下相机与玩家之间的距离
    // =========================================================================
    public static final ModConfigSpec.DoubleValue CAMERA_DISTANCE = BUILDER
            .comment("相机距离玩家") // 说明
            .defineInRange("cameraDistance", 2.0D, 0.0D, 10.0D); // 默认 2 格，范围 0-10

    // =========================================================================
    // 配置项：相机高度
    // 第三人称视角下相机在玩家上方（或下方，负值）的高度
    // =========================================================================
    public static final ModConfigSpec.DoubleValue CAMERA_HEIGHT = BUILDER
            .comment("相机高度") // 说明
            .defineInRange("cameraHeight", 0.7D, -5.0D, 5.0D); // 默认 0.7 格，范围 -5 到 5

    // =========================================================================
    // 配置项：相机左右偏移
    // 第三人称视角下相机在玩家左侧（或右侧，负值）的偏移量
    // =========================================================================
    public static final ModConfigSpec.DoubleValue CAMERA_SIDE = BUILDER
            .comment("相机左右偏移") // 说明
            .defineInRange("cameraSide", 0.0D, -5.0D, 5.0D); // 默认居中，范围 -5 到 5

    // =========================================================================
    // 构建最终的配置规格
    // build() 方法会验证所有配置项并生成可使用的 SPEC
    // =========================================================================
    static final ModConfigSpec SPEC = BUILDER.build();

    // =========================================================================
    // 物品名称验证器
    // 用于验证 ITEM_STRINGS 列表中的每个条目是否为有效的物品资源键
    // param obj 要验证的对象
    // return 如果是有效的物品名称则返回 true
    // =========================================================================
    private static boolean validateItemName(final Object obj) {
        // instanceof 检查对象是否为 String 类型
        // 如果是，则解析为 ResourceLocation 并检查注册表中是否存在
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }

    // =========================================================================
    // 配置事件处理内部类
    // EventBusSubscriber：自动注册到事件总线
    // modid：指定只响应本模组的配置事件
    // =========================================================================
    @EventBusSubscriber(modid = CombatPerspective.MOD_ID)
    public static class ConfigEvents {
        // =====================================================================
        // 配置加载事件处理方法
        // 当配置文件被加载时（游戏启动时）调用
        // SubscribeEvent：订阅 ModConfigEvent.Loading 事件
        // param configEvent 配置加载事件
        // =====================================================================
        @SubscribeEvent
        static void onLoad(final ModConfigEvent.Loading configEvent) {
            // 目前为空，可以在这里添加配置加载时的处理逻辑
            // 例如：验证配置值的合理性、初始化依赖配置的功能等
        }
    }
}