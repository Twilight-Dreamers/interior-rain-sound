package irs.modid;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.block.*;
import net.minecraft.state.property.Properties;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.shape.VoxelShape;

public class RainMuffler {
    private static final int MAX_SEARCH_DEPTH = 32;
    private static final Direction[] SEARCH_DIRECTIONS = Direction.values();

    public static boolean isInEnclosedSpace() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) {
            return false;
        }

        ClientWorld world = client.world;
        BlockPos startPos = client.player.getBlockPos();

        // Quick sky access check
        if (world.isSkyVisible(startPos)) return false;

        // Flood fill search
        return !canReachSky(world, startPos.mutableCopy(), new HashSet<>(), 0);
    }

    private static boolean isAirOrPassable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        // 1. Always pass through true air and replaceable blocks
        if (state.isAir()) return true;
        if (state.isReplaceable()) return true;

        // 2. Force ALL STAIRS to be solid (regardless of orientation)
        if (state.getBlock() instanceof StairsBlock) {
            return false; // Hard-code stairs as solid
        }

        // 3. Check actual collision shape
        return state.getCollisionShape(world, pos).isEmpty();
    }

    private static boolean canReachSky(World world, BlockPos.Mutable pos, Set<BlockPos> visited, int depth) {
        if (depth > MAX_SEARCH_DEPTH) return false;
        if (visited.contains(pos)) return false;

        BlockState state = world.getBlockState(pos);
        System.out.println("Checking " + pos + ": " + Registries.BLOCK.getId(state.getBlock()));

        // Special case - if we hit any stair, stop propagation
        if (state.getBlock() instanceof StairsBlock) {
            System.out.println("Blocked by stair at " + pos);
            return false;
        }

        if (world.isSkyVisible(pos)) return true;

        visited.add(pos.toImmutable());
        if (!isAirOrPassable(world, pos)) return false;

        for (Direction dir : SEARCH_DIRECTIONS) {
            pos.move(dir);
            if (canReachSky(world, pos, visited, depth + 1)) return true;
            pos.move(dir.getOpposite());
        }
        return false;
    }
}
