// DebugLogger.java
package irs.modid;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.registry.Registries;

public class DebugLogger {
    public static boolean isDebugMode() {
        return InteriorRainSoundClient.CONFIG != null &&
                InteriorRainSoundClient.CONFIG.debug_mode;
    }

    public static void debugChat(String message) {
        if (InteriorRainSoundClient.CONFIG.debug_mode) {
            PlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.sendMessage(Text.literal("[RainMuffler] " + message), false);
            }
        }
    }

    public static void logBlockCheck(World world, BlockPos pos, String status) {
        if (InteriorRainSoundClient.CONFIG.debug_mode) {
            BlockState state = world.getBlockState(pos);
            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
            debugChat(String.format("%s @ [%d, %d, %d] (%s)",
                    status, pos.getX(), pos.getY(), pos.getZ(), blockId));
        }
    }
}
