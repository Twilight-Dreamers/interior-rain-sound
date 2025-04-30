package irs.modid;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;

public class PerformanceCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            dispatcher.register(CommandManager.literal("rainperf")
                    .executes(context -> {
                        RainMuffler.debugMode = !RainMuffler.debugMode;
                        context.getSource().sendMessage(Text.literal(
                                "Rain performance debug " + (RainMuffler.debugMode ? "§aENABLED" : "§cDISABLED")
                        ));
                        return 1;
                    })
                    .then(CommandManager.literal("stats")
                            .executes(context -> {
                                RainMuffler.printPerformanceStats();
                                context.getSource().sendMessage(Text.literal("Printed stats to console!"));
                                return 1;
                            })
                    )
            );
        });
    }
}
