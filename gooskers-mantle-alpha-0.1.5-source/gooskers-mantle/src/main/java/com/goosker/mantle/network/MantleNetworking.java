package com.goosker.mantle.network;

import com.goosker.mantle.GooskersMantle;
import com.goosker.mantle.mantle.MantleHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public final class MantleNetworking {
    public static final Identifier REQUEST = GooskersMantle.id("request");
    public static final Identifier ANIMATION = GooskersMantle.id("animation");

    private MantleNetworking() {
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buffer, responseSender) -> {
            int horizontalId = buffer.readUnsignedByte();
            if (horizontalId > 3) {
                return;
            }

            Direction direction = Direction.fromHorizontal(horizontalId);
            server.execute(() -> MantleHandler.tryMantle(player, direction));
        });
    }

    public static void broadcastAnimation(ServerPlayerEntity player, int durationTicks) {
        for (ServerPlayerEntity viewer : PlayerLookup.tracking(player)) {
            sendAnimation(viewer, player.getId(), durationTicks);
        }
        sendAnimation(player, player.getId(), durationTicks);
    }

    private static void sendAnimation(ServerPlayerEntity recipient, int entityId, int durationTicks) {
        PacketByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(durationTicks);
        ServerPlayNetworking.send(recipient, ANIMATION, buffer);
    }
}
