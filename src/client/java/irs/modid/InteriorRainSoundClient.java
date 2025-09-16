package irs.modid;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

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

		// F3 Debug
		HudRenderCallback.EVENT.register(this::onHudRender);
	}

	// RenderTickCounter is changed to float
	private void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();

		// Check both debug HUD visibility and mod's debug mode
		if (client.getDebugHud().shouldShowDebugHud() &&
				InteriorRainSoundClient.CONFIG.debug_mode) {

			// Position below vanilla debug info
			int x = 4;
			int y = client.getWindow().getScaledHeight() - 40;

			// White text with dark background
			context.fill(x - 2, y - 2, x + 100, y + 30, 0x80000000);

			context.drawText(client.textRenderer,
					"[Rain Muffler]", x, y, 0xFFFFFF, false);

			context.drawText(client.textRenderer,
					String.format("Avg: %.2fms", PerformanceMonitor.getAverageTime()),
					x, y + 10, 0x00FF00, false); // Green

			context.drawText(client.textRenderer,
					String.format("Cache: %.1f%%", PerformanceMonitor.getCacheHitRate()),
					x, y + 20, 0xFFA500, false); // Orange
		}
	}
}
