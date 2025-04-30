package irs.modid.mixin.client;

import irs.modid.RainMuffler;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Redirect;
import irs.modid.RainMuffler; // Your utility class
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

@Mixin(SoundSystem.class)
public abstract class ExampleClientMixin {
	@Redirect(
			method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/sound/SoundInstance;getVolume()F"
			)
	)
	private float adjustRainVolume(SoundInstance sound) {
		if (isRainSound(sound)) {
			boolean enclosed = RainMuffler.isInEnclosedSpace();
			if (enclosed) {
				return sound.getVolume() * 0.2f;
			}
		}
		return sound.getVolume();
	}

	private boolean isRainSound(SoundInstance sound) {
		return sound.getCategory() == SoundCategory.WEATHER &&
				sound.getId().equals(Identifier.of("minecraft", "weather.rain"));
	}



	/*
	private boolean isRainSound(SoundInstance sound) {
		Identifier id = sound.getId();
		return sound.getCategory() == SoundCategory.WEATHER &&
				(id.getPath().contains("rain") || id.getPath().contains("weather"));
	}
	 */

}