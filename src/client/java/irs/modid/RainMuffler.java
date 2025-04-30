package irs.modid;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.HashSet;

public class RainMuffler {
    public static boolean isInEnclosedSpace() {
        long startTime = System.nanoTime();
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (invalidClientState(client)) return false;

            ClientWorld world = client.world;
            BlockPos currentPos = client.player.getBlockPos();
            long currentTick = world.getTime();

            // 1. Biome check
            if (BiomeChecker.shouldSkipCheck(world, currentPos)) {
                return false;
            }

            // 2. Quick vertical check
            if (FloodFillEngine.quickSkyCheck(world, currentPos)) {
                CacheManager.updateCache(false, currentPos, currentTick, world);
                return false;
            }

            // 3. Cache check
            if (CacheManager.shouldUseCache(world, currentPos, currentTick)) {
                return CacheManager.getLastResult() && !FloodFillEngine.quickSkyCheck(world, currentPos);
            }

            // 4. Full flood-fill check
            boolean result = performFullCheck(world, currentPos);
            CacheManager.updateCache(result, currentPos, currentTick, world);
            return result;
        } finally {
            PerformanceMonitor.recordExecution(System.nanoTime() - startTime);
        }
    }

    private static boolean invalidClientState(MinecraftClient client) {
        return client == null || client.world == null || client.player == null;
    }

    private static boolean performFullCheck(World world, BlockPos pos) {
        int maxDepth = InteriorRainSoundClient.CONFIG != null ?
                InteriorRainSoundClient.CONFIG.max_search_depth :
                24;

        // Remove the HashSet parameter from the call
        return !FloodFillEngine.canReachSky(
                world,
                pos.mutableCopy(), // This should be a BlockPos.Mutable
                maxDepth
        );
    }
}
