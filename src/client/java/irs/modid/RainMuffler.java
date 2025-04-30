package irs.modid;

import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.Identifier;

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

    // ===== New Fields =====
    private static RegistryKey<Biome> lastBiomeKey;
    private static final double BIOME_CHECK_RADIUS = 4.0; // Blocks

    private static final Set<RegistryKey<Biome>> DRY_BIOMES = Set.of(
            RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "desert")),
            RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "badlands"))
    );

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

            // 1. Dry biome check (skip processing if in desert/badlands)
            RegistryEntry<Biome> biome = world.getBiome(currentPos);
            if (DRY_BIOMES.contains(biome.getKey().orElse(null))) {
                if (debugMode) System.out.println("[Biome] Dry biome - skipping check");
                updateCache(false, currentPos, currentTick, world);
                return false;
            }

            // 2. A quick pass sky check
            if (quickSkyCheck(world, currentPos)) {
                updateCache(false, currentPos, currentTick, world);
                return false;
            }

            // 3. Check cache validity
            if (shouldUseCache(world, currentPos, currentTick)) {
                return lastResult;
            }

            // 3. Sky visibility check
            if (world.isSkyVisible(currentPos)) {
                updateCache(false, currentPos, currentTick, world);
                return false;
            }

            // 4. Full flood-fill check
            boolean result = !canReachSky(world, currentPos.mutableCopy(), new HashSet<>(), 0);
            updateCache(result, currentPos, currentTick, world);
            return result;
        } finally {
            long duration = System.nanoTime() - startTime;
            if (debugMode) {
                totalTimeNanos.addAndGet(duration);
                callCount.incrementAndGet();
            }
        }
    }

    // ===== Updated Cache Check =====
    private static boolean shouldUseCache(World world, BlockPos currentPos, long currentTick) {
        // 1. Check time and movement first (existing logic)
        if (currentTick == lastCheckedTick) return true;

        double distSq = currentPos.getSquaredDistance(lastCheckedPos);
        boolean cacheValid = (currentTick - lastCheckedTick < CACHE_TICKS)
                && (distSq <= MAX_MOVE_DIST_SQ);

        // 2. Biome check - invalidate if biome changed significantly
        RegistryKey<Biome> currentBiome = world.getBiome(currentPos).getKey().orElse(null);
        if (cacheValid && currentBiome != null && !currentBiome.equals(lastBiomeKey)) {
            BlockPos biomeCheckPos = currentPos.add(
                    (int)(BIOME_CHECK_RADIUS * (world.random.nextDouble() - 0.5)),
                    0,
                    (int)(BIOME_CHECK_RADIUS * (world.random.nextDouble() - 0.5))
            );
            RegistryKey<Biome> nearbyBiome = world.getBiome(biomeCheckPos).getKey().orElse(null);
            if (!currentBiome.equals(nearbyBiome)) {
                cacheValid = false; // Biome transition area
            }
        }

        if (debugMode) {
            System.out.println("[Cache] Biome: " + currentBiome + " | Valid: " + cacheValid);
        }
        return cacheValid;
    }

    // ===== Updated Cache Update =====
    private static void updateCache(boolean result, BlockPos pos, long tick, World world) {
        lastResult = result;
        lastCheckedPos = pos.toImmutable();
        lastCheckedTick = tick;
        lastBiomeKey = world.getBiome(pos).getKey().orElse(null); // Update biome
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
