package com.aaa.combatperspective.mixin;

import com.aaa.combatperspective.data.CursorStore;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Unique
    private CameraType combatperspective$lastCameraType = CameraType.FIRST_PERSON;
    @Inject(method = "grabMouse()V", at = @At("HEAD"), cancellable = true)
    private void preventGrabInThirdPerson(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        // 仅拦截：第三人称后视角 + 无 GUI 打开
        if (isthirdPersonback(mc) && mc.screen == null) {
            ci.cancel();
        }
    }
    @Unique
    private boolean isthirdPersonback(Minecraft mc) {
        return isCameraThirdPerson(mc.options.getCameraType());
    }
    @Unique
    private boolean isCameraThirdPerson(CameraType cameraType) {
        return !cameraType.isFirstPerson() && !cameraType.isMirrored();
    }

    @Inject(method = "handleAccumulatedMovement()V", at = @At("HEAD"))
    private void onCameraSwitch(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        CameraType current = mc.options.getCameraType();
        // 视角未变化，直接返回（零开销快速路径）
        if (current == combatperspective$lastCameraType) {
            return;
        }
        boolean wasThirdPerson = isCameraThirdPerson(combatperspective$lastCameraType);
        boolean isNowThirdPerson = isCameraThirdPerson(current);
        // 进入第三人称后视角 → 隐藏鼠标
        if (!wasThirdPerson && isNowThirdPerson && mc.screen == null ) {
        //    GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
        // 退出第三人称后视角 → 恢复抓取
        if (wasThirdPerson && !isNowThirdPerson) {
            mc.mouseHandler.grabMouse();
        }
        combatperspective$lastCameraType = current;

    }

    @Inject(method = "turnPlayer(D)V", at = @At("HEAD"), cancellable = true)
    private void lockCameraInThirdPerson(double movementTime, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (isthirdPersonback(mc) && mc.screen == null) {
            ci.cancel();
        }
    }
    @Inject(method = "handleAccumulatedMovement()V", at = @At("HEAD"))
    private void releaseMouseInThirdPerson(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        boolean isthirdPersonback = !mc.options.getCameraType().isFirstPerson()
                && !mc.options.getCameraType().isMirrored();

        boolean noScreen = mc.screen == null;
        if (isthirdPersonback && noScreen){
        //    GLFW.glfwSetInputMode(mc.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
    }

    // ========== 摄像机调整：按住 LeftAlt + 鼠标/滚轮 ==========

    @Unique
    private double cpLastX = Double.NaN, cpLastY = Double.NaN;

    /**
     * GLFW CURSOR_DISABLED 下 xpos/ypos 是虚拟累计坐标，手动取差值。
     */
    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void adjustCameraOnMove(long window, double xpos, double ypos, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!isthirdPersonback(mc) || mc.screen != null) return;
        if (!isAdjustKeyHeld(window)) return;

        // 首次 → 记初始位置
        if (Double.isNaN(cpLastX)) { cpLastX = xpos; cpLastY = ypos; return; }

        double dx = xpos - cpLastX;
        double dy = ypos - cpLastY;
        cpLastX = xpos;
        cpLastY = ypos;

        double sensitivity = 0.2;
        CursorStore.setCameraSphYaw(CursorStore.getCameraSphYaw() - dx * sensitivity);
        CursorStore.setCameraSphPitch(
                Mth.clamp(CursorStore.getCameraSphPitch() - dy * sensitivity, -89, 89)
        );
        ci.cancel();
    }

    /**
     * 拦截滚轮，按住 LeftAlt 时调整摄像机距离。
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void adjustCameraOnScroll(long window, double scrollX, double scrollY, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!isthirdPersonback(mc) || mc.screen != null) return;
        if (!isAdjustKeyHeld(window)) return;

        double dist = CursorStore.getCameraSphDist() - scrollY * 0.2;
        CursorStore.setCameraSphDist(dist);
        ci.cancel();
    }

    /** Left Alt 是否按下 */
    private static boolean isAdjustKeyHeld(long window) {
        return com.aaa.combatperspective.CombatPerspectiveClient.ADJUST_CAMERA_KEY.isDown();
    }
}