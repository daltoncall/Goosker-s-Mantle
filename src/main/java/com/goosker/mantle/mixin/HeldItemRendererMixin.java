package com.goosker.mantle.mixin;

import com.goosker.mantle.client.ClientAnimationState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HeldItemRenderer.class, priority = 500)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void gooskersmantle$animateHands(
            AbstractClientPlayerEntity player,
            float tickDelta,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            CallbackInfo callbackInfo
    ) {
        float amount = ClientAnimationState.poseAmount(player.getId(), tickDelta);
        if (amount <= 0.0F) {
            return;
        }

        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1.0F : -1.0F;
        matrices.translate(-side * 0.08D * amount, 0.24D * amount, -0.18D * amount);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-46.0F * amount));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * 9.0F * amount));
    }
}
