package space.ajcool.ardapaths.mc.sounds;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;

/**
 * Registry for custom sound events in ArdaPaths.
 * Handles registration of sound events with the Minecraft registry.
 */
public class ModSounds {
    /**
     * The sound event played when traversing a trail.
     */
    public static final SoundEvent TRAIL = register("trail_sound");

    /**
     * Register a sound.
     *
     * @param id The sound's ID.
     */
    @SuppressWarnings("SameParameterValue")
    private static SoundEvent register(final String id) {
        final Identifier identifier = new Identifier(ArdaPaths.MOD_ID, id);
        return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
    }

    /**
     * Initializes the sound event registry by forcing class loading.
     * This method is called during mod initialization.
     */
    public static void init() {
    }
}
