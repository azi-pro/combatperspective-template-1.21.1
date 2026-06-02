// =============================================================================
// Config.java - 配置文件定义
// =============================================================================
// 包声明
package com.aaa.combatperspective;

import com.aaa.combatperspective.data.CursorStore;
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
    // deltaCameraZ：摄像机在玩家后方（Z轴）的距离
    public static final ModConfigSpec.DoubleValue CAMERA_DELTA_Z = BUILDER
            .comment("摄像机在玩家后方的距离 (deltaZ)")
            .defineInRange("cameraDeltaZ", 6.0D, 0.0D, 128.0D);

    // deltaCameraY：摄像机在玩家上方的高度
    public static final ModConfigSpec.DoubleValue CAMERA_DELTA_Y = BUILDER
            .comment("摄像机在玩家上方的高度 (deltaY)")
            .defineInRange("cameraDeltaY", 6.0D, 0.0D, 128.0D);

    // deltaCameraX：摄像机在玩家右方的偏移（负值=左方）
    public static final ModConfigSpec.DoubleValue CAMERA_DELTA_X = BUILDER
            .comment("摄像机左右偏移 (deltaX)")
            .defineInRange("cameraDeltaX", 0.0D, -10.0D, 128.0D);

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
            syncToCursorStore();
        }

        @SubscribeEvent
        static void onReload(final ModConfigEvent.Reloading configEvent) {
            syncToCursorStore();
        }

        private static void syncToCursorStore() {
            CursorStore.setDeltaCameraX(CAMERA_DELTA_X.get());
            CursorStore.setDeltaCameraY(CAMERA_DELTA_Y.get());
            CursorStore.setDeltaCameraZ(CAMERA_DELTA_Z.get());
        }
    }
}