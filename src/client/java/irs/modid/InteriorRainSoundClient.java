package irs.modid;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import static irs.modid.PerformanceMonitor.printPerformanceStats;

public class InteriorRainSoundClient implements ClientModInitializer, ModMenuApi {
	public static ModConfig CONFIG;

	// ModMenu API integration (correct for modern versions)
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> AutoConfig.getConfigScreen(ModConfig.class, parent).get();
	}

	@Override
	public void onInitializeClient() {
		// 1. Config registration
		AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

		// 2. Block update listener (no need for manual ModMenu registration)
		ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
			if (CacheManager.lastCheckedPos != null &&  // Changed here
					blockEntity.getPos().getSquaredDistance(CacheManager.lastCheckedPos) < 16 * 16) {
				CacheManager.lastCheckedTick = 0;
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null &&
					client.player.age % 600 == 0 && // Every 30 seconds (20 ticks/sec * 30)
					DebugLogger.isDebugMode()) {
				printPerformanceStats(client.player);
			}
		});

		// 3. Debug command registration
		PerformanceCommand.register();
	}
}