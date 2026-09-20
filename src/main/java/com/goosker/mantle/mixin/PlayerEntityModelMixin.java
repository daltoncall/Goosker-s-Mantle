package com.goosker.mantle.mixin;

import com.goosker.mantle.client.ClientAnimationState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the mantle after vanilla and animation mods have calculated their
 * normal pose. The intentionally low priority makes this callback run after
 * Not Enough Animations' default-priority PlayerEntityModel mixin.
 */
@Mixin(value = PlayerEntityModel.class, priority = 500)
public abstract class PlayerEntityModelMixin<T extends LivingEntity> {
    @SuppressWarnings("unchecked")
    @Inject(method = "setAngles", at = @At("RETURN"))
    private void gooskersmantle$applyFinalPose(
            T livingEntity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo callbackInfo
    ) {
        if (!(livingEntity instanceof AbstractClientPlayerEntity player)) {
            return;
        }

        float tickDelta = MathHelper.clamp(animationProgress - player.age, 0.0F, 1.0F);
        PlayerEntityModel<AbstractClientPlayerEntity> model =
                (PlayerEntityModel<AbstractClientPlayerEntity>) (Object) this;
        ClientAnimationState.applyThirdPerson(player, model, tickDelta);
    }
}
