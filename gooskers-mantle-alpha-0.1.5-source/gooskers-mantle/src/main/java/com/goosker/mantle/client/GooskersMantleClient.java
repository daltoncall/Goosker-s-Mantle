package com.goosker.mantle.client;

import com.goosker.mantle.network.MantleNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class GooskersMantleClient implements ClientModInitializer {
    private static final int FLIGHT_EXIT_GRACE_TICKS = 5;
    private static int flightExitGraceTicks;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MantleNetworking.ANIMATION,
                (client, handler, buffer, responseSender) -> {
                    int entityId = buffer.readVarInt();
                    int durationTicks = buffer.readVarInt();
                    client.execute(() -> ClientAnimationState.start(entityId, durationTicks));
                });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientAnimationState.tick(client);
            tickMantleInput(client);
        });
    }

    private static void tickMantleInput(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            flightExitGraceTicks = 0;
            return;
        }

        if (player.getAbilities().flying) {
            flightExitGraceTicks = FLIGHT_EXIT_GRACE_TICKS;
            return;
        }
        if (flightExitGraceTicks > 0) {
            flightExitGraceTicks--;
            return;
        }

        if (!client.options.jumpKey.isPressed()
                || player.isOnGround()
                || player.isClimbing()
                || player.isTouchingWater()
                || player.isInLava()
                || player.hasVehicle()) {
            return;
        }

        Vec3d velocity = player.getVelocity();
        Direction direction;
        if (velocity.x * velocity.x + velocity.z * velocity.z > 0.0025D) {
            direction = Direction.getFacing(velocity.x, 0.0D, velocity.z);
        } else {
            direction = Direction.fromRotation(player.getYaw());
        }

        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeByte(direction.getHorizontal());
        ClientPlayNetworking.send(MantleNetworking.REQUEST, buffer);
    }
}
