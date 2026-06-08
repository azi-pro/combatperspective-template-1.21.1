package com.aaa.combatperspective;

import com.aaa.combatperspective.data.CameraStore;
import com.aaa.combatperspective.data.EdgeScrollStore;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue CAMERA_DELTA_Z = BUILDER
            .comment("摄像机在玩家后方的距离 (deltaZ)")
            .defineInRange("cameraDeltaZ", 6.0D, 0.0D, 128.0D);

    public static final ModConfigSpec.DoubleValue CAMERA_DELTA_Y = BUILDER
            .comment("摄像机在玩家上方的高度 (deltaY)")
            .defineInRange("cameraDeltaY", 6.0D, 0.0D, 128.0D);

    public static final ModConfigSpec.DoubleValue CAMERA_DELTA_X = BUILDER
            .comment("摄像机左右偏移 (deltaX)")
            .defineInRange("cameraDeltaX", 0.0D, -10.0D, 128.0D);

    public static final ModConfigSpec.IntValue FOV_MODE = BUILDER
            .comment("疾跑FOV处理: 0=方案一(锁FOV), 1=方案二(仅去疾跑扩视场角)")
            .defineInRange("fovMode", 1, 0, 1);

    public static final ModConfigSpec.BooleanValue EDGE_ROTATE_ENABLED = BUILDER
            .comment("鼠标移到屏幕边缘时自动旋转摄像机")
            .define("edgeRotateEnabled", true);

    public static final ModConfigSpec.DoubleValue EDGE_MARGIN_X = BUILDER
            .comment("水平边缘触发比例")
            .defineInRange("edgeMarginX", 0.10D, 0.01D, 0.50D);

    public static final ModConfigSpec.DoubleValue EDGE_MARGIN_Y = BUILDER
            .comment("竖直边缘触发比例")
            .defineInRange("edgeMarginY", 0.10D, 0.01D, 0.50D);

    public static final ModConfigSpec.DoubleValue CAMERA_YAW_SPEED = BUILDER
            .comment("摄像机水平旋转速度 (度/秒)")
            .defineInRange("cameraYawSpeed", 2.0D, 0.0D, 20.0D);

    public static final ModConfigSpec.DoubleValue CAMERA_PITCH_SPEED = BUILDER
            .comment("摄像机竖直旋转速度 (度/秒)")
            .defineInRange("cameraPitchSpeed", 2.0D, 0.0D, 20.0D);

    static final ModConfigSpec SPEC = BUILDER.build();

    @EventBusSubscriber(modid = CombatPerspective.MOD_ID)
    public static class ConfigEvents {
        @SubscribeEvent
        static void onLoad(final ModConfigEvent.Loading event) { sync(); }

        @SubscribeEvent
        static void onReload(final ModConfigEvent.Reloading event) { sync(); }

        private static void sync() {
            CameraStore.setDeltaCameraX(CAMERA_DELTA_X.get());
            CameraStore.setDeltaCameraY(CAMERA_DELTA_Y.get());
            CameraStore.setDeltaCameraZ(CAMERA_DELTA_Z.get());
            CameraStore.setFovMode(FOV_MODE.get());
            EdgeScrollStore.setEnabled(EDGE_ROTATE_ENABLED.get());
            EdgeScrollStore.setMarginX(EDGE_MARGIN_X.get());
            EdgeScrollStore.setMarginY(EDGE_MARGIN_Y.get());
            EdgeScrollStore.setYawSpeed(CAMERA_YAW_SPEED.get());
            EdgeScrollStore.setPitchSpeed(CAMERA_PITCH_SPEED.get());
        }
    }
}
