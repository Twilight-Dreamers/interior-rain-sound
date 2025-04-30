// ExampleClientMixin.java
package irs.modid.mixin.client;

import irs.modid.InteriorRainSoundClient;
import irs.modid.RainMuffler;
import irs.modid.VolumeAdjustingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public abstract class ExampleClientMixin {
	@Unique
	private static float currentRainVolume = 1.0f;
	@Unique
	private static float targetRainVolume = 1.0f;

	@Inject(method = "tick()V", at = @At("HEAD"))
	private void updateRainVolume(CallbackInfo ci) {
		float transitionSpeed = InteriorRainSoundClient.CONFIG != null ?
				MathHelper.clamp(InteriorRainSoundClient.CONFIG.transition_speed, 0.01f, 1.0f) :
				0.1f;

		currentRainVolume = MathHelper.clamp(
				MathHelper.lerp(transitionSpeed, currentRainVolume, targetRainVolume),
				0.0f,
				1.0f  // Ensure volume stays within valid range
		);
	}

	@ModifyVariable(
			method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
			at = @At("HEAD"),
			argsOnly = true
	)
	private SoundInstance modifyRainSound(SoundInstance sound) {
		if (isRainSound(sound)) {
			float interiorVolume = InteriorRainSoundClient.CONFIG != null ?
					MathHelper.clamp(InteriorRainSoundClient.CONFIG.interior_volume, 0.0f, 1.0f) :
					0.2f;

			targetRainVolume = RainMuffler.isInEnclosedSpace() ? interiorVolume : 1.0f;
			return new VolumeAdjustingSoundInstance(sound, MathHelper.clamp(currentRainVolume, 0.0f, 1.0f));
		}
		return sound;
	}

	@Unique
	private boolean isRainSound(SoundInstance sound) {
		return sound.getCategory() == SoundCategory.WEATHER &&
				sound.getId().equals(Identifier.of("minecraft", "weather.rain"));
	}
}