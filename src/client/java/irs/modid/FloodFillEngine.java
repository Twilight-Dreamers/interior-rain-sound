package irs.modid;

import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class FloodFillEngine {
    private static final Direction[] SEARCH_DIRECTIONS = Direction.values();

    public static boolean canReachSky(World world, BlockPos.Mutable startPos,
                                      Set<BlockPos> visited, int maxDepth) {
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos.toImmutable());
        visited.add(startPos.toImmutable());

        int depth = 0; // Initialize depth counter

        while (!queue.isEmpty() && depth <= maxDepth) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                BlockPos pos = queue.poll();

                // Stair check
                BlockState state = world.getBlockState(pos);
                if (state.getBlock() instanceof StairsBlock) {
                    DebugLogger.logBlockCheck(world, pos, "Blocked by stair");
                    continue;
                }

                if (world.isSkyVisible(pos)) {
                    DebugLogger.debugChat("Found sky access at " + pos);
                    return true;
                }

                if (!isAirOrPassable(world, pos)) {
                    DebugLogger.logBlockCheck(world, pos, "Blocked by");
                    continue;
                }

                // Explore neighbors
                for (Direction dir : SEARCH_DIRECTIONS) {
                    BlockPos neighbor = pos.offset(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
            depth++; // Increment depth after processing each level
        }
        DebugLogger.debugChat("Max search depth reached: " + maxDepth);
        return false;
    }

    public static boolean quickSkyCheck(World world, BlockPos pos) {
        BlockPos.Mutable mutablePos = pos.mutableCopy();
        for (int y = pos.getY(); y < world.getHeight(); y++) {
            mutablePos.setY(y);
            BlockState state = world.getBlockState(mutablePos);

            // 1. Explicitly block stairs (critical!)
            if (state.getBlock() instanceof StairsBlock) {
                if (DebugLogger.isDebugMode()) System.out.println("[Raycast] Blocked by stair at " + mutablePos);
                return false;
            }

            // 2. Check passability (same logic as flood-fill)
            if (!isAirOrPassable(world, mutablePos)) {
                if (DebugLogger.isDebugMode()) System.out.println("[Raycast] Blocked by solid block at " + mutablePos);
                return false;
            }
        }
        return true; // Found open sky
    }

    private static boolean isAirOrPassable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);

        // Explicitly block all stairs (even if their collision shape is empty)
        if (state.getBlock() instanceof StairsBlock) {
            return false;
        }

        return state.isAir() || state.isReplaceable() ||
                state.getCollisionShape(world, pos).isEmpty();
    }
}
