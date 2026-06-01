// =============================================================================
// CombatPerspective.java - 模组主类（服务端入口）
// =============================================================================
// 包声明，声明当前类所在的包路径
package com.aaa.combatperspective;

// 导入创造模式标签页相关类
import com.aaa.combatperspective.item.ModCreativeModeTabs;
// 导入物品注册相关类
import com.aaa.combatperspective.item.ModItems;

// 导入日志系统，使用 slf4j 作为日志门面
import org.slf4j.Logger;
// 使用 Mojang 提供的 LogUtils 获取日志器
import com.mojang.logging.LogUtils;

// 导入 Minecraft 注册表，用于查询方块/物品等
import net.minecraft.core.registries.BuiltInRegistries;
// 导入注册表类型常量
import net.minecraft.core.registries.Registries;
// 导入聊天组件，用于 GUI 显示文本
import net.minecraft.network.chat.Component;
// 导入食物属性（目前未使用）
import net.minecraft.world.food.FoodProperties;
// 导入方块物品包装类
import net.minecraft.world.item.BlockItem;
// 导入创造模式标签页相关类
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
// 导入基础物品类
import net.minecraft.world.item.Item;
// 导入方块相关类
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
// 导入方块状态属性
import net.minecraft.world.level.block.state.BlockBehaviour;
// 导入材质颜色
import net.minecraft.world.level.material.MapColor;

// 导入 NeoForge 事件总线，用于事件监听和发布
import net.neoforged.bus.api.IEventBus;
// 导入事件订阅注解
import net.neoforged.bus.api.SubscribeEvent;
// 导入 Mod 注解，声明这是一个 NeoForge 模组
import net.neoforged.fml.common.Mod;
// 导入配置文件类型
import net.neoforged.fml.config.ModConfig;
// 导入模组容器，用于获取模组元数据
import net.neoforged.fml.ModContainer;
// 导入通用初始化事件
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
// 导入 NeoForge 事件总线
import net.neoforged.neoforge.common.NeoForge;
// 导入创造模式标签页构建事件
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
// 导入服务器启动事件
import net.neoforged.neoforge.event.server.ServerStartingEvent;
// 导入延迟块注册器
import net.neoforged.neoforge.registries.DeferredBlock;
// 导入延迟持有者
import net.neoforged.neoforge.registries.DeferredHolder;
// 导入延迟物品注册器
import net.neoforged.neoforge.registries.DeferredItem;
// 导入延迟注册器
import net.neoforged.neoforge.registries.DeferredRegister;

// 导入静态引用，自定义创造模式标签页的标签页实例
import static com.aaa.combatperspective.item.ModCreativeModeTabs.CP_TAB;

// =============================================================================
// 模组主类声明
// Mod 注解标记此类为 NeoForge 模组
// MOD_ID 必须与 META-INF/neoforge.mods.toml 中的 modId 一致
// =============================================================================
@Mod(CombatPerspective.MOD_ID)
public class CombatPerspective {
    // =========================================================================
    // 静态常量：模组唯一标识符，所有资源都以此为命名空间
    // =========================================================================
    public static final String MOD_ID = "combatperspective";

    // =========================================================================
    // 静态常量：Logger 实例，用于输出日志到控制台和游戏日志文件
    // LogUtils.getLogger() 会自动使用类名作为 logger 名称
    // =========================================================================
    public static final Logger LOGGER = LogUtils.getLogger();

    // =========================================================================
    // 构造函数：模组加载时调用的入口点
    // NeoForge 会自动注入 IEventBus（事件总线）和 ModContainer（模组容器）
    // =========================================================================
    public CombatPerspective(IEventBus modEventBus, ModContainer modContainer) {
        // -------------------------------------------------------------------------
        // 添加通用设置监听器
        // modEventBus.addListener 注册一个回调，在模组加载的 commonSetup 阶段被调用
        // this::commonSetup 是一种方法引用，指向当前的 commonSetup 方法
        // -------------------------------------------------------------------------
        modEventBus.addListener(this::commonSetup);

        // -------------------------------------------------------------------------
        // 注册物品系统
        // ModItems.register 会将物品延迟注册器绑定到事件总线
        // 这样物品才能被正确注册到游戏注册表中
        // -------------------------------------------------------------------------
        ModItems.register(modEventBus);

        // -------------------------------------------------------------------------
        // 注册创造模式标签页系统
        // 允许在创造模式物品栏中显示本模组的物品
        // -------------------------------------------------------------------------
        ModCreativeModeTabs.register(modEventBus);

        // -------------------------------------------------------------------------
        // 注册本类到 NeoForge 全局事件总线
        // 只有当本类需要响应事件（如 @SubscribeEvent 标记的方法）时需要这行
        // 如果没有带 @SubscribeEvent 的方法，这行可以删除
        // -------------------------------------------------------------------------
        NeoForge.EVENT_BUS.register(this);

        // -------------------------------------------------------------------------
        // 添加创造模式标签页内容监听器
        // 当游戏构建创造模式物品栏时，会调用 addCreative 方法
        // 用于将模组物品添加到对应的标签页中
        // -------------------------------------------------------------------------
        modEventBus.addListener(this::addCreative);

        // -------------------------------------------------------------------------
        // 注册配置文件
        // modContainer.registerConfig 将 Config.SPEC 注册为模组的配置文件
        // ModConfig.Type.COMMON 表示这是通用配置，对所有端（客户端/服务端）生效
        // 配置文件会在游戏启动时自动加载，并可在游戏中修改
        // -------------------------------------------------------------------------
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // =========================================================================
    // 通用设置方法：在模组初始化的早期阶段调用
    // 用于执行一些需要注册表已就绪的初始化代码
    // param event 通用设置事件
    // =========================================================================
    private void commonSetup(FMLCommonSetupEvent event) {
        // 输出日志，表示模组已加载
        LOGGER.info("HELLO FROM COMMON SETUP");

        // -------------------------------------------------------------------------
        // 条件日志：检查配置中的 logDirtBlock 选项
        // getAsBoolean() 获取配置文件中定义的布尔值
        // 如果玩家启用了这个选项，则打印泥土方块的注册键（资源位置）
        // -------------------------------------------------------------------------
        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            // BuiltInRegistries.BLOCK.getKey(Blocks.DIRT) 获取泥土方块的资源键
            // 如 minecraft:dirt
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        // -------------------------------------------------------------------------
        // 打印魔法数字配置
        // 使用字符串模板，{} 会被实际值替换
        // MAGIC_NUMBER_INTRODUCTION 是前缀文本，MAGIC_NUMBER 是实际数值
        // -------------------------------------------------------------------------
        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        // -------------------------------------------------------------------------
        // 遍历物品字符串列表并打印
        // ITEM_STRINGS 是一个列表配置项，可以配置多个物品
        // forEach 遍历列表，每个 item 都是一个物品的资源键字符串
        // -------------------------------------------------------------------------
        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // =========================================================================
    // 添加创造模式物品方法：在构建创造模式物品栏时被调用
    // param event 创造模式标签页内容构建事件
    // event.getTabKey() 返回当前正在构建的标签页
    // event.accept() 将物品添加到该标签页的物品列表中
    // =========================================================================
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // -------------------------------------------------------------------------
        // 如果当前构建的是战斗用品标签页
        // CreativeModeTabs.COMBAT 是原版游戏的战斗物品栏
        // 则接受（添加）本模组的铁制长剑
        // -------------------------------------------------------------------------
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.Iron_LongSword);
        }

        // -------------------------------------------------------------------------
        // 如果当前构建的是本模组的自定义标签页
        // CP_TAB 是我们在 ModCreativeModeTabs 中定义的自定义标签页
        // 则同样接受铁制长剑，使其在两个地方都能找到
        // -------------------------------------------------------------------------
        if (event.getTabKey() == CP_TAB) {
            event.accept(ModItems.Iron_LongSword);
        }
    }

    // =========================================================================
    // 服务器启动事件处理方法
    // SubscribeEvent 注解表示此方法订阅了 ServerStartingEvent 事件
    // 当服务器启动时（单人游戏创建世界或多人服务器启动），此方法会被调用
    // param event 服务器启动事件
    // =========================================================================
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 输出日志，表示服务器正在启动
        LOGGER.info("HELLO from server starting");
    }
}