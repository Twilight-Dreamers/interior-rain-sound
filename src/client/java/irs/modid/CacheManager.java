// CacheManager.java
package irs.modid;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class CacheManager {
    public static BlockPos lastCheckedPos = BlockPos.ORIGIN;
    public static long lastCheckedTick = 0;
    public static boolean lastResult = false;
    public static RegistryKey<Biome> lastBiomeKey;

    private static final double MAX_MOVE_DIST_SQ = 2.0 * 2.0;
    private static final double BIOME_CHECK_RADIUS = 4.0;

    // ===== Updated Cache Check =====
    public static boolean shouldUseCache(World world, BlockPos currentPos, long currentTick) {
        if (currentTick == lastCheckedTick) {
            PerformanceMonitor.incrementCacheHits();
            return true;
        }

        double distSq = currentPos.getSquaredDistance(lastCheckedPos);

        int cacheTicks = InteriorRainSoundClient.CONFIG != null ?
                InteriorRainSoundClient.CONFIG.cache_ticks : 10;

        boolean cacheValid = (currentTick - lastCheckedTick < cacheTicks)
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

        if (DebugLogger.isDebugMode()) {
            System.out.println("[Cache] Biome: " + currentBiome + " | Valid: " + cacheValid);
        }
        return cacheValid;
    }

    // ===== Updated Cache Update =====
    public static void updateCache(boolean result, BlockPos pos, long tick, World world) {
        lastResult = result;
        lastCheckedPos = pos.toImmutable();
        lastCheckedTick = tick;
        lastBiomeKey = world.getBiome(pos).getKey().orElse(null); // Update biome
    }

    public static boolean getLastResult() {
        return lastResult;
    }
}