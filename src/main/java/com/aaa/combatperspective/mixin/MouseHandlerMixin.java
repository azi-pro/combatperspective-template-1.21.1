package com.aaa.combatperspective.mixin;

import com.aaa.combatperspective.data.CameraStore;
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

    /** grabMouse 后：第三人称覆盖为 HIDDEN + 边界钳制 */
    @Inject(method = "grabMouse()V", at = @At("TAIL"))
    private void overrideGrabInThirdPerson(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();


        long win = mc.getWindow().getWindow();
        clampCursor(win, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        if (isthirdPersonback(mc) && mc.screen == null) {
            GLFW.glfwSetInputMode(win, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
        else {
            GLFW.glfwSetInputMode(win, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        }

    }

    @Unique
    private static void clampCursor(long window, int w, int h) {
        double[] mx = new double[2];
        GLFW.glfwGetCursorPos(window, mx, mx);
        double cx = Mth.clamp(mx[0], 0, w - 1);
        double cy = Mth.clamp(mx[1], 0, h - 1);
        if (cx != mx[0] || cy != mx[1]) {
            GLFW.glfwSetCursorPos(window, cx, cy);
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
        // 进入第三人称 → 默认锁定鼠标
        if (!wasThirdPerson && isNowThirdPerson && mc.screen == null) {
            mc.mouseHandler.grabMouse();
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
    private void manageCursorInThirdPerson(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!isthirdPersonback(mc) || mc.screen != null) return;

        long win = mc.getWindow().getWindow();
        if (isAdjustKeyHeld()) {
            GLFW.glfwSetInputMode(win, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
            clampCursor(win, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        } else {
            // 松开调整键 → 重新锁定，重置差值标记
            if (GLFW.glfwGetInputMode(win, GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_DISABLED) {
                mc.mouseHandler.grabMouse();
                cpLastX = Double.NaN;
                cpLastY = Double.NaN;
            }
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
        if (!isAdjustKeyHeld()) return;

        // 首次 → 直接记录当前位置
        if (Double.isNaN(cpLastX)) {
            cpLastX = xpos;
            cpLastY = ypos;
            return;
        }

        double dx = xpos - cpLastX;
        double dy = ypos - cpLastY;
        cpLastX = xpos;
        cpLastY = ypos;

        double sensitivity = 0.2;
        if (dx != 0) CameraStore.setCameraSphYaw(CameraStore.getCameraSphYaw() - dx * sensitivity);
        if (dy != 0) CameraStore.setCameraSphPitch(
                Mth.clamp(CameraStore.getCameraSphPitch() - dy * sensitivity, -89, 89)
        );
        if (dx != 0 || dy != 0) CameraStore.syncDeltaToConfig();
        ci.cancel();
    }

    /**
     * 拦截滚轮，按住 LeftAlt 时调整摄像机距离。
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void adjustCameraOnScroll(long window, double scrollX, double scrollY, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!isthirdPersonback(mc) || mc.screen != null) return;
        if (!isAdjustKeyHeld()) return;

        double dist = CameraStore.getCameraSphDist() - scrollY * 0.2;
        CameraStore.setCameraSphDist(dist);
        CameraStore.syncDeltaToConfig();
        ci.cancel();
    }

    private static boolean isAdjustKeyHeld() {
        return com.aaa.combatperspective.CombatPerspectiveClient.ADJUST_CAMERA_KEY.isDown();
    }
}