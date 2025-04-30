package irs.modid;

import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class RainMuffler {
    // Performance tracking
    private static final AtomicLong totalTimeNanos = new AtomicLong();
    private static final AtomicInteger callCount = new AtomicInteger();
    public static boolean debugMode = false;

    // Flood-fill config
    private static final int MAX_SEARCH_DEPTH = 24;
    private static final Direction[] SEARCH_DIRECTIONS = Direction.values();

    // Caching system
    private static final int CACHE_TICKS = 10; // Re-check every 10 ticks (~0.5s)
    private static final double MAX_MOVE_DIST_SQ = 2.0 * 2.0; // Re-check if moved >2 blocks
    private static boolean lastResult = false;
    static BlockPos lastCheckedPos = BlockPos.ORIGIN;
    static long lastCheckedTick = 0;

    public static boolean isInEnclosedSpace() {
        long startTime = System.nanoTime();
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.world == null || client.player == null) {
                return false;
            }

            ClientWorld world = client.world;
            BlockPos currentPos = client.player.getBlockPos();
            long currentTick = world.getTime();

            if (quickSkyCheck(world, currentPos)) {
                updateCache(false, currentPos, currentTick);
                return false;
            }

            // Check cache validity
            if (shouldUseCache(currentPos, currentTick)) {
                return lastResult;
            }

            // Full check required
            if (world.isSkyVisible(currentPos)) {
                updateCache(false, currentPos, currentTick);
                return false;
            }

            boolean result = !canReachSky(world, currentPos.mutableCopy(), new HashSet<>(), 0);
            updateCache(result, currentPos, currentTick);
            return result;
        } finally {
            long duration = System.nanoTime() - startTime;
            if (debugMode) {
                totalTimeNanos.addAndGet(duration);
                callCount.incrementAndGet();
            }
        }
    }

    private static boolean shouldUseCache(BlockPos currentPos, long currentTick) {
        // 1. Same tick? Always use cache
        if (currentTick == lastCheckedTick) {
            if (debugMode) System.out.println("[Cache] HIT (same tick)");
            return true;
        }

        // 2. Check distance and cache duration
        double distSq = currentPos.getSquaredDistance(lastCheckedPos);
        boolean cacheValid = (currentTick - lastCheckedTick < CACHE_TICKS)
                && (distSq <= MAX_MOVE_DIST_SQ);

        if (debugMode) {
            if (cacheValid) {
                System.out.printf("[Cache] HIT (pos: %.1f blocks, age: %d ticks)\n",
                        Math.sqrt(distSq), currentTick - lastCheckedTick);
            } else {
                System.out.printf("[Cache] MISS (pos: %.1f blocks, age: %d ticks)\n",
                        Math.sqrt(distSq), currentTick - lastCheckedTick);
            }
        }
        return cacheValid;
    }

    private static void updateCache(boolean result, BlockPos pos, long tick) {
        lastResult = result;
        lastCheckedPos = pos.toImmutable();
        lastCheckedTick = tick;
    }

    // ... [keep printPerformanceStats() unchanged] ...

    private static boolean quickSkyCheck(World world, BlockPos pos) {
        BlockPos.Mutable mutablePos = pos.mutableCopy();
        for (int y = pos.getY(); y < world.getHeight(); y++) {
            mutablePos.setY(y);
            BlockState state = world.getBlockState(mutablePos);

            // 1. Explicitly block stairs (critical!)
            if (state.getBlock() instanceof StairsBlock) {
                if (debugMode) System.out.println("[Raycast] Blocked by stair at " + mutablePos);
                return false;
            }

            // 2. Check passability (same logic as flood-fill)
            if (!isAirOrPassable(world, mutablePos)) {
                if (debugMode) System.out.println("[Raycast] Blocked by solid block at " + mutablePos);
                return false;
            }
        }
        return true; // Found open sky
    }

    // Optimized BFS implementation (instead of recursion)
    private static boolean canReachSky(World world, BlockPos.Mutable startPos, Set<BlockPos> visited, int depth) {
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos.toImmutable());
        visited.add(startPos.toImmutable());

        while (!queue.isEmpty() && depth <= MAX_SEARCH_DEPTH) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                BlockPos pos = queue.poll();

                // --- FIXED ORDER OF CHECKS ---
                // 1. First check if block is passable (stairs will fail here)
                if (!isAirOrPassable(world, pos)) {
                    continue; // Skip solid blocks like stairs
                }

                // 2. Then check sky visibility for passable blocks only
                if (world.isSkyVisible(pos)) {
                    return true;
                }

                // 3. Add neighbors to continue searching
                for (Direction dir : SEARCH_DIRECTIONS) {
                    BlockPos neighbor = pos.offset(dir);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
                if (debugMode && depth == 0) {
                    System.out.println("[FloodFill] Starting from: " + startPos);
                }
            }
            depth++;
        }
        return false;
    }

    private static boolean checkBlock(World world, BlockPos pos) {
        if (world.isSkyVisible(pos)) return true;

        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof StairsBlock) return false;

        return isAirOrPassable(world, pos);
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

    public static void printPerformanceStats() {
        if (callCount.get() == 0) return;

        double totalMs = totalTimeNanos.get() / 1_000_000.0;
        double avgMs = totalMs / callCount.get();

        System.out.println("\n=== Rain Muffler Performance ===");
        System.out.printf("Total checks: %d\n", callCount.get());
        System.out.printf("Total time: %.2fms\n", totalMs);
        System.out.printf("Average per check: %.3fms\n", avgMs);
        System.out.println("================================");

        // Reset counters
        totalTimeNanos.set(0);
        callCount.set(0);
    }
}
