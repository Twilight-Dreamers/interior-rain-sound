package irs.modid.mixin.client;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
		float originalVolume = sound.getVolume();
		if (isRainSound(sound) && isUnderCover()) {
			return originalVolume * 0.2f; // Reduce volume to 20%
		}
		return originalVolume;
	}

	private boolean isRainSound(SoundInstance sound) {
		return sound.getCategory() == SoundCategory.WEATHER &&
				sound.getId().equals(Identifier.of("minecraft", "weather.rain"));
	}

	private boolean isUnderCover() {
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;
		if (player == null) return false;

		World world = player.getWorld();
		BlockPos.Mutable pos = new BlockPos.Mutable(player.getX(), player.getEyeY(), player.getZ());

		for (int i = 0; i < 10; i++) {
			pos.move(Direction.UP);
			BlockState state = world.getBlockState(pos);
			if (!state.isAir()) {
				return true;
			}
		}
		return false;
	}
}