package irs.modid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;


public class InteriorRainSoundClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		PerformanceCommand.register();
		ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
			if (RainMuffler.lastCheckedPos != null &&
					blockEntity.getPos().getSquaredDistance(RainMuffler.lastCheckedPos) < 16 * 16) {
				RainMuffler.lastCheckedTick = 0;
			}
		});
	}
}