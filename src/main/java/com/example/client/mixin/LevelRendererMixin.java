package com.example.client.mixin;

import com.example.client.SpeedmineState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    private GameRenderer gameRenderer;

    @Shadow
    protected abstract void submitHitOutline(
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        RenderType renderType,
        BlockOutlineRenderState state,
        int color,
        float lineWidth,
        boolean isTranslucent
    );

    @Inject(method = "submitBlockOutline", at = @At("TAIL"))
    private void onSubmitBlockOutline(
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        LevelRenderState levelRenderState,
        CallbackInfo ci
    ) {
        if (SpeedmineState.enabled && SpeedmineState.blockOutlineEnabled) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null && mc.level != null) {
                BlockPos destroyPos = ((MultiPlayerGameModeAccessor) mc.gameMode).getDestroyBlockPos();
                if (destroyPos != null) {
                    BlockState blockState = mc.level.getBlockState(destroyPos);
                    if (!blockState.isAir()) {
                        VoxelShape shape = blockState.getShape(mc.level, destroyPos);
                        if (!shape.isEmpty()) {
                            Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
                            BlockOutlineRenderState destroyState = new BlockOutlineRenderState(
                                destroyPos,
                                false,
                                false,
                                shape
                            );
                            poseStack.pushPose();
                            poseStack.translate(
                                (double) destroyPos.getX() - cameraPos.x,
                                (double) destroyPos.getY() - cameraPos.y,
                                (double) destroyPos.getZ() - cameraPos.z
                            );
                            int greenColor = 0xFF00FF00;
                            float lineWidth = this.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
                            this.submitHitOutline(
                                poseStack,
                                submitNodeCollector,
                                RenderTypes.lines(),
                                destroyState,
                                greenColor,
                                lineWidth,
                                false
                            );
                            poseStack.popPose();
                        }
                    }
                }
            }
        }
    }
}
