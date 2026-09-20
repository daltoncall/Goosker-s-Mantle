package com.goosker.mantle.mantle;

import com.goosker.mantle.GooskersMantle;
import com.goosker.mantle.config.MantleConfig;
import com.goosker.mantle.network.MantleNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MantleHandler {
    private static final double EPSILON = 1.0E-4D;
    private static final double FALLING_VELOCITY_THRESHOLD = -0.01D;
    private static final double OUTER_LIP_OVERLAP = 0.35D;
    private static final int MINIMUM_AIRBORNE_TICKS = 2;
    private static final int FLIGHT_EXIT_GRACE_TICKS = 5;
    private static final Map<UUID, PlayerState> PLAYER_STATES = new HashMap<>();

    private MantleHandler() {
    }

    public static void endServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            PlayerState state = PLAYER_STATES.computeIfAbsent(player.getUuid(), ignored -> new PlayerState(player.getY()));
            if (player.getAbilities().flying) {
                state.flightSuppressionUntilTick = player.getServerWorld().getTime() + FLIGHT_EXIT_GRACE_TICKS;
            }
            if (player.isOnGround()) {
                state.takeoffSurfaceY = player.getY();
                state.airborneTicks = 0;
            } else if (state.airborneTicks < Integer.MAX_VALUE) {
                state.airborneTicks++;
            }
        }

        PLAYER_STATES.keySet().removeIf(uuid -> server.getPlayerManager().getPlayer(uuid) == null);
    }

    public static void tryMantle(ServerPlayerEntity player, Direction direction) {
        if (direction.getAxis().isVertical() || !canAttempt(player)) {
            return;
        }

        PlayerState state = PLAYER_STATES.computeIfAbsent(player.getUuid(), ignored -> new PlayerState(player.getY()));
        long now = player.getServerWorld().getTime();
        Vec3d velocity = player.getVelocity();
        boolean falling = velocity.y < FALLING_VELOCITY_THRESHOLD;
        if ((!falling && state.airborneTicks < MINIMUM_AIRBORNE_TICKS)
                || now < state.cooldownUntilTick
                || now < state.flightSuppressionUntilTick
                || state.lastRequestTick == now) {
            return;
        }
        state.lastRequestTick = now;

        MantleConfig config = GooskersMantle.config();
        if (!hasReachableLedge(player, direction, state.takeoffSurfaceY, config)) {
            return;
        }

        player.setVelocity(velocity.x, Math.max(velocity.y, config.upward_velocity), velocity.z);
        player.velocityModified = true;
        player.fallDistance = 0.0F;
        state.cooldownUntilTick = now + config.cooldown_ticks;

        player.getServerWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                GooskersMantle.MANTLE_SOUND,
                SoundCategory.PLAYERS,
                1.3F,
                1.0F
        );
        MantleNetworking.broadcastAnimation(player, config.animation_ticks);
    }

    private static boolean canAttempt(ServerPlayerEntity player) {
        return !player.isOnGround()
                && !player.isSpectator()
                && !player.getAbilities().flying
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.isInLava()
                && !player.hasVehicle()
                && player.getPose() == EntityPose.STANDING;
    }

    private static boolean hasReachableLedge(
            ServerPlayerEntity player,
            Direction preferredDirection,
            double takeoffSurfaceY,
            MantleConfig config
    ) {
        // Keep the configured minimum anchored to the last standing surface
        // throughout an ordinary jump. Once the player genuinely falls below
        // that surface, use their current feet as the reference instead. This
        // preserves midair catches without letting the falling path turn a
        // slab or one-block step into a valid mantle.
        if (player.getVelocity().y < FALLING_VELOCITY_THRESHOLD
                && player.getY() + EPSILON < takeoffSurfaceY) {
            takeoffSurfaceY = player.getY();
        }

        List<Direction> directions = List.of(
                preferredDirection,
                preferredDirection.rotateYClockwise(),
                preferredDirection.rotateYCounterclockwise(),
                preferredDirection.getOpposite()
        );

        for (Direction direction : directions) {
            if (hasReachableLedgeInDirection(player, direction, takeoffSurfaceY, config)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasReachableLedgeInDirection(
            ServerPlayerEntity player,
            Direction direction,
            double takeoffSurfaceY,
            MantleConfig config
    ) {
        World world = player.getWorld();
        Box playerBox = player.getBoundingBox();
        double reachX = direction.getOffsetX() * config.detection_reach;
        double reachZ = direction.getOffsetZ() * config.detection_reach;
        Box probe = playerBox.stretch(reachX, 0.0D, reachZ).expand(0.025D, 0.0D, 0.025D);

        int minimumX = (int) Math.floor(probe.minX);
        int maximumX = (int) Math.floor(probe.maxX - EPSILON);
        int minimumZ = (int) Math.floor(probe.minZ);
        int maximumZ = (int) Math.floor(probe.maxZ - EPSILON);
        int minimumY = (int) Math.floor(takeoffSurfaceY);
        int maximumY = (int) Math.floor(takeoffSurfaceY + config.maximum_mantle_height);

        for (BlockPos pos : BlockPos.iterate(minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ)) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            VoxelShape shape = state.getCollisionShape(world, pos, ShapeContext.of(player));
            for (Box localBox : shape.getBoundingBoxes()) {
                Box collision = localBox.offset(pos);
                double ledgeHeight = collision.maxY - takeoffSurfaceY;
                if (ledgeHeight + EPSILON < config.minimum_mantle_height
                        || ledgeHeight - EPSILON > config.maximum_mantle_height
                        || !intersectsHorizontally(collision, probe)
                        || !isInFront(collision, playerBox, direction, config.detection_reach)) {
                    continue;
                }

                if (hasLandingClearance(player, collision, direction)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean intersectsHorizontally(Box first, Box second) {
        return first.maxX > second.minX + EPSILON
                && first.minX < second.maxX - EPSILON
                && first.maxZ > second.minZ + EPSILON
                && first.minZ < second.maxZ - EPSILON;
    }

    private static boolean isInFront(Box collision, Box playerBox, Direction direction, double reach) {
        return switch (direction) {
            case EAST -> collision.maxX >= playerBox.maxX - 0.05D && collision.minX <= playerBox.maxX + reach;
            case WEST -> collision.minX <= playerBox.minX + 0.05D && collision.maxX >= playerBox.minX - reach;
            case SOUTH -> collision.maxZ >= playerBox.maxZ - 0.05D && collision.minZ <= playerBox.maxZ + reach;
            case NORTH -> collision.minZ <= playerBox.minZ + 0.05D && collision.maxZ >= playerBox.minZ - reach;
            default -> false;
        };
    }

    private static boolean hasLandingClearance(ServerPlayerEntity player, Box ledge, Direction direction) {
        double halfWidth = player.getWidth() * 0.5D;
        double targetX = player.getX();
        double targetZ = player.getZ();

        if (direction == Direction.EAST) {
            targetX = ledge.minX + halfWidth + 0.02D;
        } else if (direction == Direction.WEST) {
            targetX = ledge.maxX - halfWidth - 0.02D;
        } else if (direction == Direction.SOUTH) {
            targetZ = ledge.minZ + halfWidth + 0.02D;
        } else if (direction == Direction.NORTH) {
            targetZ = ledge.maxZ - halfWidth - 0.02D;
        }

        targetX = clamp(targetX, ledge.minX + halfWidth, ledge.maxX - halfWidth);
        targetZ = clamp(targetZ, ledge.minZ + halfWidth, ledge.maxZ - halfWidth);

        Box standingBox = player.getDimensions(EntityPose.STANDING)
                .getBoxAt(new Vec3d(targetX, ledge.maxY, targetZ))
                .contract(0.01D);
        if (player.getWorld().isSpaceEmpty(player, standingBox)) {
            return true;
        }

        // Railings commonly occupy the middle of an otherwise standable block.
        // Check the approach-side lip as well, while keeping enough of the
        // player's footprint over the ledge that a solid ceiling still blocks it.
        double lipX = targetX;
        double lipZ = targetZ;
        if (direction == Direction.EAST) {
            lipX = ledge.minX - halfWidth + OUTER_LIP_OVERLAP;
        } else if (direction == Direction.WEST) {
            lipX = ledge.maxX + halfWidth - OUTER_LIP_OVERLAP;
        } else if (direction == Direction.SOUTH) {
            lipZ = ledge.minZ - halfWidth + OUTER_LIP_OVERLAP;
        } else if (direction == Direction.NORTH) {
            lipZ = ledge.maxZ + halfWidth - OUTER_LIP_OVERLAP;
        }

        Box lipStandingBox = player.getDimensions(EntityPose.STANDING)
                .getBoxAt(new Vec3d(lipX, ledge.maxY, lipZ))
                .contract(0.01D);
        return player.getWorld().isSpaceEmpty(player, lipStandingBox);
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (minimum > maximum) {
            return (minimum + maximum) * 0.5D;
        }
        return Math.max(minimum, Math.min(value, maximum));
    }

    private static final class PlayerState {
        private double takeoffSurfaceY;
        private int airborneTicks;
        private long cooldownUntilTick;
        private long flightSuppressionUntilTick;
        private long lastRequestTick = Long.MIN_VALUE;

        private PlayerState(double takeoffSurfaceY) {
            this.takeoffSurfaceY = takeoffSurfaceY;
        }
    }
}
