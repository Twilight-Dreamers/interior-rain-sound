// CacheManager.java
package irs.modid;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class CacheManager {
    public static BlockPos lastCheckedPos = BlockPos.ORIGIN;
    public static long lastCheckedTick = 0;
    public static boolean lastResult = false;
    public static RegistryKey<Biome> lastBiomeKey;

    private static final double MAX_MOVE_DIST_SQ = 2.0 * 2.0;
    private static final double BIOME_CHECK_RADIUS = 4.0;

    public static class CacheEntry {
        public BlockPos pos;
        public long tick;
        public boolean result;
        public RegistryKey<Biome> biome;
    }

    private static CacheEntry entry = new CacheEntry();

    // ===== Updated Cache Check =====
    public static boolean shouldUseCache(World world, BlockPos currentPos, long currentTick) {
        if (currentTick == lastCheckedTick) {
            PerformanceMonitor.incrementCacheHits();
            return true;
        }

        double distSq = currentPos.getSquaredDistance(lastCheckedPos);
        boolean sameChunk = new ChunkPos(lastCheckedPos).equals(new ChunkPos(currentPos));
        int cacheTicks = InteriorRainSoundClient.CONFIG != null ?
                InteriorRainSoundClient.CONFIG.cache_ticks : 10;

        boolean cacheValid = (currentTick - lastCheckedTick < cacheTicks)
                && (sameChunk || distSq <= MAX_MOVE_DIST_SQ);

        RegistryKey<Biome> currentBiome = world.getBiome(currentPos).getKey().orElse(null);
        if (cacheValid && currentBiome != null && !currentBiome.equals(lastBiomeKey)) {
            int matches = 0;
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos checkPos = currentPos.offset(dir, (int) BIOME_CHECK_RADIUS);
                RegistryKey<Biome> nearbyBiome = world.getBiome(checkPos).getKey().orElse(null);
                if (currentBiome.equals(nearbyBiome)) matches++;
            }
            cacheValid = matches >= 3; // At least 3/4 directions match
        }

        if (!cacheValid) reset();

        if (DebugLogger.isDebugMode()) {
            System.out.println("[Cache] " + (cacheValid ? "HIT" : "MISS")
                    + " | DTick=" + (currentTick - lastCheckedTick)
                    + " Dist=" + String.format("%.1f", Math.sqrt(distSq))
                    + " Biome=" + (currentBiome != null ? currentBiome.getValue() : "null")
            );
        }

        return currentTick - entry.tick < cacheTicks
                && entry.pos.equals(currentPos)
                && entry.biome.equals(currentBiome);
    }

    // ===== Updated Cache Update =====
    public static void updateCache(boolean result, BlockPos pos, long tick, World world) {
        lastResult = result;
        lastCheckedPos = pos.toImmutable();
        lastCheckedTick = tick;
        lastBiomeKey = world.getBiome(pos).getKey().orElse(null); // Update biome
    }

    public static void reset() {
        lastCheckedPos = BlockPos.ORIGIN;
        lastCheckedTick = 0;
        lastResult = false;
        lastBiomeKey = null;
    }

    public static boolean getLastResult() {
        return lastResult;
    }
}