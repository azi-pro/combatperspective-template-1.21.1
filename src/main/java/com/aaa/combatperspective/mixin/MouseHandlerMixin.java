// =============================================================================
// MouseHandlerMixin.java - 鼠标输入控制 Mixin
// =============================================================================
// 包声明
package com.aaa.combatperspective.mixin;

// 导入视角类型枚举
import net.minecraft.client.CameraType;

// 导入 Minecraft 主类
import net.minecraft.client.Minecraft;

// 导入鼠标处理类
import net.minecraft.client.MouseHandler;

// 导入 GLFW 库，用于原生鼠标控制
import org.lwjgl.glfw.GLFW;

// 导入 Mixin 注解
import org.spongepowered.asm.mixin.Mixin;

// 导入 @Unique 注解
import org.spongepowered.asm.mixin.Unique;

// 导入注入注解
import org.spongepowered.asm.mixin.injection.At;

// 导入注入注解
import org.spongepowered.asm.mixin.injection.Inject;

// 导入回调信息类
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// =============================================================================
// 鼠标处理 Mixin：修改鼠标输入行为
// 在战斗视角模式下拦截和修改鼠标事件
// =============================================================================
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    // =========================================================================
    // 上一帧的视角类型（用于检测视角切换）
    // Unique：Mixin 类私有字段
    // =========================================================================
    @Unique
    private CameraType combatperspective$lastCameraType = CameraType.FIRST_PERSON;

    // =========================================================================
    // 阻止鼠标抓取方法
    // 当游戏尝试隐藏鼠标并进入"抓取"模式时拦截
    // Inject：在 MouseHandler.grabMouse() 开头注入
    // cancellable = true：允许取消原方法执行
    // param ci 回调信息
    // =========================================================================
    @Inject(method = "grabMouse()V", at = At.HEAD, cancellable = true)
    private void preventGrabInThirdPerson(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        // 仅拦截：第三人称后视角 + 无 GUI 打开
        // 这样游戏中不会尝试隐藏鼠标，保持鼠标可见用于视角控制
        if (isthirdPersonback(mc) && mc.screen == null) {
            // 取消原方法，阻止鼠标抓取
            ci.cancel();
        }
    }

    // =========================================================================
    // 辅助方法：判断是否为第三人称后视角
    // param mc Minecraft 实例
    // return true = 第三人称后视角
    // =========================================================================
    @Unique
    private boolean isthirdPersonback(Minecraft mc) {
        return isCameraThirdPerson(mc.options.getCameraType());
    }

    // =========================================================================
    // 辅助方法：判断视角类型是否为第三人称（非镜像）
    // param cameraType 视角类型
    // return true = 第三人称后视角
    // =========================================================================
    @Unique
    private boolean isCameraThirdPerson(CameraType cameraType) {
        // !isFirstPerson()：不是第一人称
        // !isMirrored()：不是第三人称前视角（镜像）
        return !cameraType.isFirstPerson() && !cameraType.isMirrored();
    }

    // =========================================================================
    // 视角切换监听方法
    // 检测视角变化并自动隐藏/显示鼠标
    // Inject：在 handleAccumulatedMovement() 开头注入
    // handleAccumulatedMovement 每帧都会被调用，用于处理累积的鼠标移动
    // param ci 回调信息
    // =========================================================================
    @Inject(method = "handleAccumulatedMovement()V", at = At.HEAD)
    private void onCameraSwitch(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        // 安全检查：确保 options 不为空
        if (mc.options == null) return;

        // 获取当前视角类型
        CameraType current = mc.options.getCameraType();

        // -------------------------------------------------------------------------
        // 快速路径：视角未变化，不处理
        // 这样可以避免不必要的逻辑执行
        // -------------------------------------------------------------------------
        if (current == combatperspective$lastCameraType) {
            return;
        }

        // 计算视角变化前后是否为第三人称
        boolean wasThirdPerson = isCameraThirdPerson(combatperspective$lastCameraType);
        boolean isNowThirdPerson = isCameraThirdPerson(current);

        // -------------------------------------------------------------------------
        // 从第一人称/镜像视角切换到第三人称后视角
        // 隐藏鼠标光标，让游戏接受鼠标移动作为视角控制
        // -------------------------------------------------------------------------
        if (!wasThirdPerson && isNowThirdPerson && mc.screen == null) {
            // GLFW.glfwSetInputMode：设置输入模式
            // GLFW.GLFW_CURSOR_HIDDEN：隐藏鼠标光标
            // mc.getWindow().getWindow()：获取 GLFW 窗口句柄
            GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }

        // -------------------------------------------------------------------------
        // 从第三人称后视角切换到其他视角
        // 恢复鼠标抓取，让鼠标正常显示并可以点击 GUI
        // -------------------------------------------------------------------------
        if (wasThirdPerson && !isNowThirdPerson) {
            mc.mouseHandler.grabMouse(); // 调用原版鼠标抓取方法
        }

        // 更新上一帧视角记录
        combatperspective$lastCameraType = current;
    }

    // =========================================================================
    // 锁定摄像机旋转方法
    // 在战斗视角模式下阻止鼠标旋转摄像机
    // Inject：在 turnPlayer() 开头注入
    // cancellable = true：允许取消原方法执行
    // param movementTime 鼠标移动时间增量
    // param ci 回调信息
    // =========================================================================
    @Inject(method = "turnPlayer(D)V", at = At.HEAD, cancellable = true)
    private void lockCameraInThirdPerson(double movementTime, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        // 如果是第三人称后视角且无 GUI 打开，取消旋转
        // 这样鼠标移动就不会影响摄像机旋转
        if (isthirdPersonback(mc) && mc.screen == null) {
            ci.cancel();
        }
    }

    // =========================================================================
    // 第三人称释放鼠标方法
    // 确保在第三人称后视角时鼠标是隐藏的
    // Inject：在 handleAccumulatedMovement() 开头注入（与方法5是同一个注入点）
    // param ci 回调信息
    // =========================================================================
    @Inject(method = "handleAccumulatedMovement()V", at = At.HEAD)
    private void releaseMouseInThirdPerson(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        // 判断是否为第三人称后视角
        boolean isthirdPersonback = !mc.options.getCameraType().isFirstPerson()
                && !mc.options.getCameraType().isMirrored();

        // 判断是否有 GUI 打开
        boolean noScreen = mc.screen == null;

        // 如果是第三人称后视角且无 GUI，隐藏鼠标
        if (isthirdPersonback && noScreen){
            // 设置鼠标光标为隐藏模式
            GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
    }
}