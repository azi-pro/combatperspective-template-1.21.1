package com.aaa.combatperspective.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 玩家 Y 以上的方块渲染为 30% 不透明度。
 * 对每个区块段，正常绘制后再用混合模式重绘一次。
 */
@Mixin(value = LevelRenderer.class)
public class LevelRendererAlphaMixin {

    private static int currentOriginY = Integer.MIN_VALUE;
    private static RenderType currentRenderType;

    /** 记录当前渲染层 */
    @Redirect(
            method = "renderSectionLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;setupRenderState()V"
            )
    )
    private void captureRenderType(RenderType type) {
        currentRenderType = type;
        type.setupRenderState();
    }

    /** 截获 getOrigin()，记录当前区块段的 Y 坐标 */
    @Redirect(
            method = "renderSectionLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;getOrigin()Lnet/minecraft/core/BlockPos;"
            )
    )
    private BlockPos captureOrigin(SectionRenderDispatcher.RenderSection section) {
        BlockPos origin = section.getOrigin();
        currentOriginY = origin.getY();
        return origin;
    }

    /** 截获 draw()：上方区块段 → 先正常绘制，再以 30% 不透明度混合绘制 */
    @Redirect(
            method = "renderSectionLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;draw()V"
            )
    )
    private void drawWithAlpha(VertexBuffer buffer) {
        Minecraft mc = Minecraft.getInstance();

        // 正常绘制
        buffer.draw();

        // 玩家 Y 以上的 solid/cutout 区块段 → 半透明叠层
        if (mc.player != null
                && currentOriginY > mc.player.getY()
                && currentRenderType != RenderType.translucent()
                && currentRenderType != RenderType.tripwire()) {

            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();

            // 不依赖着色器 ColorModulator，用 OpenGL 常量混合
            org.lwjgl.opengl.GL14.glBlendColor(1.0F, 1.0F, 1.0F, 0.3F);
            RenderSystem.blendFunc(
                    org.lwjgl.opengl.GL14.GL_CONSTANT_ALPHA,
                    org.lwjgl.opengl.GL14.GL_ONE_MINUS_CONSTANT_ALPHA
            );

            buffer.draw(); // 第二次绘制 → 30% 不透明度

            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }
    }
}
