package com.goosker.mantle.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

public final class ClientAnimationState {
    private static final Map<Integer, MantleAnimation> ACTIVE = new HashMap<>();
    private static int clientTick;

    private ClientAnimationState() {
    }

    public static void tick(MinecraftClient client) {
        clientTick++;
        ACTIVE.values().removeIf(animation -> clientTick > animation.startTick + animation.durationTicks + 1);
        if (client.world == null) {
            ACTIVE.clear();
        }
    }

    public static void start(int entityId, int durationTicks) {
        ACTIVE.put(entityId, new MantleAnimation(clientTick, Math.max(1, durationTicks)));
    }

    public static float poseAmount(int entityId, float tickDelta) {
        MantleAnimation animation = ACTIVE.get(entityId);
        if (animation == null) {
            return 0.0F;
        }

        float progress = (clientTick + tickDelta - animation.startTick) / animation.durationTicks;
        if (progress < 0.0F || progress >= 1.0F) {
            return 0.0F;
        }

        // Lift quickly, settle on the ledge for a beat, then ease the arms all
        // the way back to their normal pose. Both joins have zero slope, so the
        // return cannot visibly snap even at a low frame rate.
        if (progress < 0.38F) {
            float lift = progress / 0.38F;
            float inverse = 1.0F - lift;
            return 1.0F - inverse * inverse * inverse;
        }
        if (progress < 0.48F) {
            return 1.0F;
        }

        float lower = (progress - 0.48F) / 0.52F;
        float smoothLower = lower * lower * (3.0F - 2.0F * lower);
        return 1.0F - smoothLower;
    }

    public static void applyThirdPerson(
            AbstractClientPlayerEntity player,
            PlayerEntityModel<AbstractClientPlayerEntity> model,
            float tickDelta
    ) {
        float amount = poseAmount(player.getId(), tickDelta);
        if (amount <= 0.0F) {
            return;
        }

        model.rightArm.pitch = MathHelper.lerp(amount, model.rightArm.pitch, -1.95F);
        model.leftArm.pitch = MathHelper.lerp(amount, model.leftArm.pitch, -1.95F);
        model.rightArm.yaw = MathHelper.lerp(amount, model.rightArm.yaw, -0.14F);
        model.leftArm.yaw = MathHelper.lerp(amount, model.leftArm.yaw, 0.14F);
        model.rightArm.roll = MathHelper.lerp(amount, model.rightArm.roll, -0.08F);
        model.leftArm.roll = MathHelper.lerp(amount, model.leftArm.roll, 0.08F);
        model.body.pitch = MathHelper.lerp(amount, model.body.pitch, 0.16F);
    }

    private record MantleAnimation(int startTick, int durationTicks) {
    }
}
