package space.ajcool.ardapaths.mc.particles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;
import space.ajcool.ardapaths.core.ModConstants;

/**
 * Registry for custom particle types in ArdaPaths.
 * Handles both registration and client-side initialization of particle types and factories.
 */
public class ModParticles {

    /**
     * The particle type for trail path particles.
     * If adding a new particle, make sure to add it to the {@link ModParticles#initClient} method.
     */
    public static final ParticleType<PathParticleEffect> PATH = register(
            "path",
            new ParticleType<>(true) {
                /**
                 * Returns the structured codec for path particle effect data.
                 *
                 * @return the path particle effect codec
                 */
                @Override
                public com.mojang.serialization.@NotNull MapCodec<PathParticleEffect> codec() {
                    return PathParticleEffect.CODEC;
                }

                /**
                 * Returns the network codec for path particle effect data.
                 *
                 * @return the path particle effect stream codec
                 */
                @Override
                public net.minecraft.network.codec.@NotNull StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, PathParticleEffect> streamCodec() {
                    return PathParticleEffect.STREAM_CODEC;
                }
            });

    /**
     * Register a particle type.
     *
     * @param <T>  the type of particle type
     * @param id   the particle's ID
     * @param type the particle type to register
     * @return the registered particle type
     */
    @SuppressWarnings("SameParameterValue")
    private static <T extends ParticleType<?>> T register(final String id, final T type) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, ModConstants.modId(id), type);
    }

    /**
     * Initializes the particle type registry by forcing class loading.
     * This method is called during mod initialization and must be invoked before {@link #initClient()}.
     */
    @SuppressWarnings("EmptyMethod")
    public static void init() {
    }

    /**
     * Initialize the particles on the client.
     * We have to call this method separately because typings are dumb.
     * This method must be invoked <b>after</b> {@link ModParticles#init}.
     */
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(PATH, PathParticleProvider::new);
    }
}
