package irs.modid;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PerformanceCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // 1. Debug toggle command
            dispatcher.register(CommandManager.literal("rainmufflerdebug")
                    .executes(context -> {
                        ModConfig config = InteriorRainSoundClient.CONFIG;
                        config.debug_mode = !config.debug_mode;
                        AutoConfig.getConfigHolder(ModConfig.class).save();
                        context.getSource().sendFeedback(
                                () -> Text.literal("Debug mode ")
                                        .append(Text.literal(config.debug_mode ? "ON" : "OFF")
                                                .formatted(config.debug_mode ? Formatting.GREEN : Formatting.RED)),
                                false
                        );
                        return 1;
                    })
            );

            // 2. Performance stats command
            dispatcher.register(CommandManager.literal("rainmufflerstats")
                    .executes(context -> {
                        PerformanceMonitor.printPerformanceStats(context.getSource().getPlayerOrThrow());
                        return 1;
                    })
            );
        });
    }
}