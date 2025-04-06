package space.ajcool.ardapaths.mc.particles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;

public class ModParticles
{
    /**
     * If adding a new particle, make sure to add it to the {@link ModParticles#initClient} method.
     */

    public static final DefaultParticleType PATH = register(
            "path",
            FabricParticleTypes.simple(true)
    );

    /**
     * Register a particle type.
     *
     * @param id   The particle's ID
     * @param type The particle type to register
     */
    private static <T extends ParticleType<?>> T register(final String id, final T type)
    {
        return Registry.register(Registries.PARTICLE_TYPE, new Identifier(ArdaPaths.MOD_ID, id), type);
    }

    public static void init()
    {
    }

    /**
     * Initialize the particles on the client.
     * We have to call this method separately because typings are dumb.
     * This method must be invoked <b>after</b> {@link ModParticles#init}.
     */
    @Environment(EnvType.CLIENT)
    public static void initClient()
    {
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(PATH, PathParticleProvider::new);
    }
}
