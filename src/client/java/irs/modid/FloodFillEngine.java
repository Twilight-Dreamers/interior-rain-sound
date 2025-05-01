package irs.modid;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import java.util.ArrayDeque;
import java.util.Deque;

public class FloodFillEngine {
    private static final Direction[] SEARCH_DIRECTIONS = Direction.values();
    private static final ThreadLocal<BlockPos.Mutable> MUTABLE_POOL = ThreadLocal.withInitial(
            BlockPos.Mutable::new
    );

    // Main flood-fill implementation with spatial optimizations
    public static boolean canReachSky(World world, BlockPos.Mutable startPos, int maxDepth) {
        LongOpenHashSet visited = new LongOpenHashSet(512);
        Deque<BlockPos.Mutable> queue = new ArrayDeque<>(256);
        queue.add(startPos.mutableCopy());
        visited.add(startPos.asLong());

        int currentDepth = 0;

        while (!queue.isEmpty() && currentDepth <= maxDepth) {
            int levelSize = queue.size();
            while (levelSize-- > 0) {
                BlockPos.Mutable pos = queue.pollFirst();

                // Stair check remains first
                BlockState state = world.getBlockState(pos);
                if (state.getBlock() instanceof StairsBlock) {
                    DebugLogger.logBlockCheck(world, pos, "Blocked by stair");
                    continue;
                }

                // Sky visibility check
                if (world.isSkyVisible(pos)) {
                    DebugLogger.debugChat("Found sky access at " + pos);
                    return true;
                }

                // Use original passability check
                if (isBlocking(world, pos)) {
                    DebugLogger.logBlockCheck(world, pos, "Blocked by");
                    continue;
                }

                // Neighbor exploration with spatial optimization
                for (Direction dir : SEARCH_DIRECTIONS) {
                    BlockPos.Mutable neighbor = MUTABLE_POOL.get().set(pos);
                    neighbor.move(dir);

                    long packed = neighbor.asLong();
                    if (!visited.contains(packed) && withinSearchBounds(startPos, neighbor, maxDepth)) {
                        visited.add(packed);
                        queue.add(neighbor.mutableCopy());
                    }
                }
            }
            currentDepth++;
        }
        DebugLogger.debugChat("Max depth reached: " + maxDepth);
        return false;
    }

    // Optimized quick sky check with mutable reuse
    public static boolean quickSkyCheck(World world, BlockPos pos) {
        BlockPos.Mutable mutablePos = pos.mutableCopy();
        for (int y = pos.getY(); y < world.getHeight(); y++) {
            mutablePos.setY(y);
            if (isBlocking(world, mutablePos)) {
                return false;
            }
        }
        return true;
    }

    private static boolean withinSearchBounds(BlockPos start, BlockPos current, int maxDepth) {
        return Math.abs(current.getX() - start.getX()) <= maxDepth &&
                Math.abs(current.getY() - start.getY()) <= maxDepth &&
                Math.abs(current.getZ() - start.getZ()) <= maxDepth;
    }

    private static boolean isBlocking(World world, BlockPos pos) {
        return !isAirOrPassable(world, pos);
    }

    private static boolean isAirOrPassable(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() ||
                state.isReplaceable() ||
                state.getCollisionShape(world, pos).isEmpty();
    }
}
