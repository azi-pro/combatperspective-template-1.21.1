// =============================================================================
// CameraAccessor.java - 相机访问器 Mixin 接口
// =============================================================================
// 包声明
package com.aaa.combatperspective.mixin;

// 导入 Minecraft 相机类
import net.minecraft.client.Camera;

// 导入 Mixin 注解
import org.spongepowered.asm.mixin.Mixin;

// 导入 @Accessor 注解，用于生成字段访问器
import org.spongepowered.asm.mixin.gen.Accessor;

// =============================================================================
// 相机访问器 Mixin：提供对 Camera 私有字段的访问
// Mixin(Camera.class)：与 CameraMixin 作用于同一个类
// 这是一个接口，提供读写 Camera 私有字段的入口
// =============================================================================
@Mixin(Camera.class)
public interface CameraAccessor {
    // =========================================================================
    // 访问器方法：getCameraYaw
    // 对应 Camera.camerayaw 字段（如果存在的话）
    // Accessor("camerayaw")：指定要访问的字段名
    // return 摄像机 yaw 角度
    // =========================================================================
    @Accessor("camerayaw")       // 对于 @Unique 字段，名字就是 field 名
    float getCameraYaw();

    // =========================================================================
    // 访问器方法：setCameraYaw
    // 设置 camerayaw 字段的值
    // param yaw 新的 yaw 角度
    // =========================================================================
    @Accessor("camerayaw")
    void setCameraYaw(float yaw);
}