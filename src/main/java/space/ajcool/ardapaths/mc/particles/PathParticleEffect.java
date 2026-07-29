package space.ajcool.ardapaths.mc.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.random.Random;

import java.util.Locale;

/**
 * Particle payload for ArdaPaths trail particles.
 *
 * @param primaryColor   the primary trail colour encoded as RGB integer bits
 * @param secondaryColor the secondary trail colour encoded as RGB integer bits, or zero when unused
 * @param tertiaryColor  the tertiary trail colour encoded as RGB integer bits, or zero when unused
 */
public record PathParticleEffect(int primaryColor, int secondaryColor, int tertiaryColor) implements ParticleEffect {
    /**
     * Codec used by particle registries and data-driven particle serialization.
     */
    public static final Codec<PathParticleEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("primary_color").forGetter(PathParticleEffect::primaryColor),
            Codec.INT.fieldOf("secondary_color").forGetter(PathParticleEffect::secondaryColor),
            Codec.INT.fieldOf("tertiary_color").forGetter(PathParticleEffect::tertiaryColor)
    ).apply(instance, PathParticleEffect::new));

    /**
     * Command and network parameter factory for path particles on Minecraft 1.20.1.
     */
    @SuppressWarnings("deprecation")
    public static final ParticleEffect.Factory<PathParticleEffect> PARAMETERS_FACTORY = new ParticleEffect.Factory<>() {
        /**
         * Reads a path particle effect from a command argument stream.
         *
         * @param type   the particle type being parsed
         * @param reader the command string reader
         * @return the parsed particle effect
         * @throws CommandSyntaxException when one of the expected colour values is missing or invalid
         */
        @Override
        public PathParticleEffect read(ParticleType<PathParticleEffect> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int primaryColor = reader.readInt();
            reader.expect(' ');
            int secondaryColor = reader.readInt();
            reader.expect(' ');
            int tertiaryColor = reader.readInt();
            return new PathParticleEffect(primaryColor, secondaryColor, tertiaryColor);
        }

        /**
         * Reads a path particle effect from the network buffer.
         *
         * @param type the particle type being decoded
         * @param buf  the packet buffer containing encoded colour values
         * @return the decoded particle effect
         */
        @Override
        public PathParticleEffect read(ParticleType<PathParticleEffect> type, PacketByteBuf buf) {
            return new PathParticleEffect(buf.readInt(), buf.readInt(), buf.readInt());
        }
    };

    /**
     * Selects one configured trail colour with equal odds across non-zero colour slots.
     *
     * @param random the random source used for colour selection
     * @return the selected RGB colour
     */
    public int selectColor(Random random) {
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
    public ParticleType<?> getType() {
        return ModParticles.PATH;
    }

    /**
     * Writes this particle payload to the network buffer.
     *
     * @param buf the packet buffer to write to
     */
    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(primaryColor);
        buf.writeInt(secondaryColor);
        buf.writeInt(tertiaryColor);
    }

    /**
     * Formats this particle payload for command and debug output.
     *
     * @return the particle type id followed by the encoded colour values
     */
    @Override
    public String asString() {
        return String.format(Locale.ROOT, "%s %d %d %d",
                Registries.PARTICLE_TYPE.getId(getType()),
                primaryColor,
                secondaryColor,
                tertiaryColor
        );
    }
}
