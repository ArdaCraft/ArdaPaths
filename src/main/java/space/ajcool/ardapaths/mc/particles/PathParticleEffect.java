package space.ajcool.ardapaths.mc.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

/**
 * Particle payload for ArdaPaths trail particles.
 *
 * @param primaryColor   the primary trail colour encoded as RGB integer bits
 * @param secondaryColor the secondary trail colour encoded as RGB integer bits, or zero when unused
 * @param tertiaryColor  the tertiary trail colour encoded as RGB integer bits, or zero when unused
 */
public record PathParticleEffect(int primaryColor, int secondaryColor, int tertiaryColor) implements ParticleOptions {

    /**
     * Codec used by particle registries and data-driven particle serialization.
     */
    public static final MapCodec<PathParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("primary_color").forGetter(PathParticleEffect::primaryColor),
            Codec.INT.fieldOf("secondary_color").forGetter(PathParticleEffect::secondaryColor),
            Codec.INT.fieldOf("tertiary_color").forGetter(PathParticleEffect::tertiaryColor)
    ).apply(instance, PathParticleEffect::new));

    /**
     * Network codec used to synchronize path particle payloads.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, PathParticleEffect> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            PathParticleEffect::primaryColor,
            ByteBufCodecs.INT,
            PathParticleEffect::secondaryColor,
            ByteBufCodecs.INT,
            PathParticleEffect::tertiaryColor,
            PathParticleEffect::new
    );

    /**
     * Selects one configured trail colour with equal odds across non-zero colour slots.
     *
     * @param random the random source used for colour selection
     * @return the selected RGB colour
     */
    public int selectColor(RandomSource random) {
        if (secondaryColor == 0) {
            return primaryColor;
        }

        double rand = random.nextDouble();
        if (tertiaryColor == 0) {
            return rand < 0.5 ? primaryColor : secondaryColor;
        }

        if (rand < (1.0 / 3.0)) {
            return primaryColor;
        }

        return rand < (2.0 / 3.0) ? secondaryColor : tertiaryColor;
    }

    /**
     * Returns the registered particle type for this payload.
     *
     * @return the path particle type
     */
    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticles.PATH;
    }

}
