// PerformanceMonitor.java
package irs.modid;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceMonitor {
    private static final AtomicLong totalTimeNanos = new AtomicLong();
    private static final AtomicInteger callCount = new AtomicInteger();
    private static final AtomicLong peakTimeNanos = new AtomicLong();
    private static final AtomicInteger cacheHits = new AtomicInteger();

    public static void incrementCacheHits() {
        cacheHits.incrementAndGet();
    }

    public static void recordExecution(long nanos) {
        totalTimeNanos.addAndGet(nanos);
        callCount.incrementAndGet();
        peakTimeNanos.accumulateAndGet(nanos, Math::max);
    }

    public static void printPerformanceStats(PlayerEntity player) {
        if (callCount.get() == 0) return;

        double totalMs = totalTimeNanos.get() / 1_000_000.0;
        double avgMs = totalMs / callCount.get();

        player.sendMessage(Text.literal("=== Rain Muffler Stats ===").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.literal(String.format(
                "Checks: %d | Avg: %.2fms | Peak: %.2fms",
                callCount.get(),
                avgMs,
                peakTimeNanos.get() / 1_000_000.0
        )), false);
        player.sendMessage(Text.literal(String.format(
                "Cache Hits: %d (%.1f%%)",
                cacheHits.get(),
                cacheHits.get() * 100.0 / callCount.get()
        )), false);

        // Reset counters
        totalTimeNanos.set(0);
        callCount.set(0);
        peakTimeNanos.set(0);
        cacheHits.set(0);
    }

    public static double getAverageTime() {
        return callCount.get() > 0 ?
                totalTimeNanos.get() / 1_000_000.0 / callCount.get() :
                0.0;
    }

    public static double getPeakTime() {
        return peakTimeNanos.get() / 1_000_000.0;
    }

    public static double getCacheHitRate() {
        return callCount.get() > 0 ?
                (cacheHits.get() * 100.0) / callCount.get() :
                0.0;
    }

    public static int getCacheHits() {
        return cacheHits.get();
    }
}
