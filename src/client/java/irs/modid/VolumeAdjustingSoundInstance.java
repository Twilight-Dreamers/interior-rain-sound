package irs.modid;

import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class VolumeAdjustingSoundInstance implements SoundInstance {
    private final SoundInstance wrapped;
    private final float volumeMultiplier;

    public VolumeAdjustingSoundInstance(SoundInstance wrapped, float multiplier) {
        this.wrapped = wrapped;
        this.volumeMultiplier = multiplier;
    }

    @Override
    public float getVolume() {
        return MathHelper.clamp(
                wrapped.getVolume() * volumeMultiplier,
                0.0f,
                1.0f  // Clamp final output
        );
    }

    // Correct method signature for Minecraft 1.21.5
    @Override
    public WeightedSoundSet getSoundSet(SoundManager soundManager) {
        return wrapped.getSoundSet(soundManager);
    }

    // Other delegated methods
    @Override public Identifier getId() { return wrapped.getId(); }
    @Override public Sound getSound() { return wrapped.getSound(); }
    @Override public SoundCategory getCategory() { return wrapped.getCategory(); }
    @Override public boolean isRelative() { return wrapped.isRelative(); }
    @Override public boolean isRepeatable() { return wrapped.isRepeatable(); }
    @Override public int getRepeatDelay() { return wrapped.getRepeatDelay(); }
    @Override public float getPitch() { return wrapped.getPitch(); }
    @Override public double getX() { return wrapped.getX(); }
    @Override public double getY() { return wrapped.getY(); }
    @Override public double getZ() { return wrapped.getZ(); }
    @Override public AttenuationType getAttenuationType() { return wrapped.getAttenuationType(); }
}