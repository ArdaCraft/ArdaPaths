package space.ajcool.ardapaths.mc.particles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;

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
            new ParticleType<>(true, PathParticleEffect.PARAMETERS_FACTORY) {
                /**
                 * Returns the structured codec for path particle effect data.
                 *
                 * @return the path particle effect codec
                 */
                @Override
                public com.mojang.serialization.Codec<PathParticleEffect> getCodec() {
                    return PathParticleEffect.CODEC;
                }
            });

    /**
     * Register a particle type.
     *
     * @param id   The particle's ID
     * @param type The particle type to register
     */
    @SuppressWarnings("SameParameterValue")
    private static <T extends ParticleType<?>> T register(final String id, final T type) {
        return Registry.register(Registries.PARTICLE_TYPE, new Identifier(ArdaPaths.MOD_ID, id), type);
    }

    /**
     * Initializes the particle type registry by forcing class loading.
     * This method is called during mod initialization and must be invoked before {@link #initClient()}.
     */
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
