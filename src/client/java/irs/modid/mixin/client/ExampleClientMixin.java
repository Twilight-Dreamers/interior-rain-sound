package irs.modid.mixin.client;

import irs.modid.RainMuffler;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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

	@Unique
	private boolean isRainSound(SoundInstance sound) {
		return sound.getCategory() == SoundCategory.WEATHER &&
				sound.getId().equals(Identifier.of("minecraft", "weather.rain"));
	}

	

}