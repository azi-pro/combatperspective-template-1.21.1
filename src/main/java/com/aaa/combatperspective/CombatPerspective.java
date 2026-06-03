// =============================================================================
// CombatPerspective.java - 模组主类（服务端入口）
// =============================================================================
// 包声明，声明当前类所在的包路径
package com.aaa.combatperspective;

import com.aaa.combatperspective.item.ModCreativeModeTabs;
import com.aaa.combatperspective.item.ModItems;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.minecraft.world.item.CreativeModeTabs;

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
        LOGGER.info("CombatPerspective 加载完成");
    }

    // =========================================================================
    // 添加创造模式物品方法：在构建创造模式物品栏时被调用
    // param event 创造模式标签页内容构建事件
    // event.getTabKey() 返回当前正在构建的标签页
    // event.accept() 将物品添加到该标签页的物品列表中
    // =========================================================================
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.Iron_LongSword.get());
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