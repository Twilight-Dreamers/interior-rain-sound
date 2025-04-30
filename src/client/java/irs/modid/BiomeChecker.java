// BiomeChecker.java
package irs.modid;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import java.util.Set;

public class BiomeChecker {
    private static final Set<RegistryKey<Biome>> DRY_BIOMES = Set.of(
            RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "desert")),
            RegistryKey.of(RegistryKeys.BIOME, Identifier.of("minecraft", "badlands"))
    );

    public static boolean shouldSkipCheck(World world, BlockPos pos) {
        // 1. Check if biome checks are enabled in config
        if (!InteriorRainSoundClient.CONFIG.biome_settings.enabled) return false;

        // 2. Get biome at position
        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
        RegistryKey<Biome> biomeKey = biomeEntry.getKey().orElse(null);

        // 3. Check if it's a dry biome we should ignore
        return DRY_BIOMES.contains(biomeKey) &&
                InteriorRainSoundClient.CONFIG.biome_settings.ignore_deserts;
    }

    public static boolean isDryBiome(RegistryKey<Biome> biome) {
        return InteriorRainSoundClient.CONFIG.biome_settings.enabled &&
                InteriorRainSoundClient.CONFIG.biome_settings.ignore_deserts &&
                DRY_BIOMES.contains(biome);
    }
}
