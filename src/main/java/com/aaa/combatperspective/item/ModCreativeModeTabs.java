// =============================================================================
// ModCreativeModeTabs.java - 创造模式标签页注册类
// =============================================================================
// 包声明
package com.aaa.combatperspective.item;

// 导入模组主类
import com.aaa.combatperspective.CombatPerspective;

// 导入注册表类型
import net.minecraft.core.registries.Registries;

// 导入聊天组件，用于显示标签页名称
import net.minecraft.network.chat.Component;

// 导入创造模式标签页类
import net.minecraft.world.item.CreativeModeTab;

// 导入物品堆叠类
import net.minecraft.world.item.ItemStack;

// 导入事件总线接口
import net.neoforged.bus.api.IEventBus;

// 导入延迟持有者
import net.neoforged.neoforge.registries.DeferredItem;

// 导入延迟注册器
import net.neoforged.neoforge.registries.DeferredRegister;

// 导入函数式接口
import java.util.function.Supplier;

// =============================================================================
// 创造模式标签页注册类
// 创建自定义的创造模式物品栏标签页
// =============================================================================
public class ModCreativeModeTabs {
    // =========================================================================
    // 创建延迟注册器
    // DeferredRegister.create() 第一个参数是注册表类型
    // Registries.CREATIVE_MODE_TAB：创造模式标签页注册表
    // 第二个参数是模组 ID
    // =========================================================================
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CombatPerspective.MOD_ID);

    // =========================================================================
    // 创建自定义标签页的延迟持有者
    // Supplier<CreativeModeTab>：延迟加载的标签页实例
    // CREATIVE_MODE_TABS.register()：注册标签页
    // "combatperspective_tab"：标签页的资源路径
    // () -> CreativeModeTab.builder()：标签页工厂，使用构建器模式
    // =========================================================================
    public static final Supplier<CreativeModeTab> CP_TAB =
            CREATIVE_MODE_TABS.register("combatperspective_tab", () -> CreativeModeTab.builder()
                    // -----------------------------------------------------------------
                    // 设置标签页图标
                    // icon()：定义标签页在菜单中显示的图标
                    // () -> new ItemStack(...)：返回用作图标的物品堆叠
                    // ModItems.Iron_LongSword.get()：获取铁剑的持有者，然后 get() 获取实际物品
                    // -----------------------------------------------------------------
                    .icon(() -> new ItemStack(ModItems.Iron_LongSword.get()))

                    // -----------------------------------------------------------------
                    // 设置标签页标题
                    // title()：标签页显示的名称
                    // Component.translatable()：使用语言文件进行本地化
                    // "itemGroup.combatperspective_tab"：语言文件中的键名
                    // -----------------------------------------------------------------
                    .title(Component.translatable("itemGroup.combatperspective_tab"))

                    // -----------------------------------------------------------------
                    // 设置标签页内容
                    // displayItems()：定义哪些物品显示在这个标签页中
                    // parameters：包含搜索过滤器等信息
                    // output：输出物品的消费者
                    // accept()：将物品添加到标签页
                    // -----------------------------------------------------------------
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.Iron_LongSword);
                    }).build()); // build() 完成构建

    // =========================================================================
    // 注册方法：将注册器绑定到事件总线
    // param eventBus 模组事件总线
    // =========================================================================
    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}