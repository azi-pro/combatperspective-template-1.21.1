// =============================================================================
// CameraInvoker.java - 相机调用器 Mixin 接口
// =============================================================================
// 包声明
package com.aaa.combatperspective.mixin;

// 导入 Minecraft 相机类
import net.minecraft.client.Camera;

// 导入三维向量类
import net.minecraft.world.phys.Vec3;

// 导入 Mixin 注解
import org.spongepowered.asm.mixin.Mixin;

// 导入 @Invoker 注解
import org.spongepowered.asm.mixin.gen.Invoker;

// =============================================================================
// 相机调用器 Mixin：提供对 Camera 私有方法的访问
// Mixin(Camera.class)：与 CameraMixin 作用于同一个类
// 这是一个接口，提供调用 Camera 私有方法的入口
// =============================================================================
@Mixin(Camera.class)
public interface CameraInvoker {
    // =========================================================================
    // 调用器方法：invokeSetPosition
    // 对应 Camera 类的 private void setPosition(Vec3 pos) 方法
    // Invoker("setPosition")：告诉 Mixin 要调用哪个私有方法
    // 使用此方法可以直接设置摄像机的世界位置
    // param pos 新的位置向量
    // =========================================================================
    @Invoker("setPosition")
    void invokeSetPosition(Vec3 pos);

    // =========================================================================
    // 调用器方法：invokeSetRotation
    // 对应 Camera 类的 private void setRotation(float yaw, float pitch) 方法
    // Invoker("setRotation")：告诉 Mixin 要调用哪个私有方法
    // 使用此方法可以直接设置摄像机的旋转角度
    // param yaw 水平旋转角度（度数）
    // param pitch 垂直旋转角度（度数）
    // =========================================================================
    @Invoker("setRotation")
    void invokeSetRotation(float yaw, float pitch);
}