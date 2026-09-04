package space.ajcool.ardapaths.mc.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * A looping ambient sound that follows the active trail geometry and fades with distance.
 */
@Environment(value = EnvType.CLIENT)
public class TrailSoundInstance extends AbstractTickableSoundInstance {

    /**
     * The amount the sound envelope increases per tick while it is fading in.
     */
    private static final float FADE_IN_STEP = 0.02f;

    /**
     * The number of seconds used when fading the sound out while the player is on the trail.
     */
    private static final float FADE_OUT_SECONDS = 5.0f;

    /**
     * The amount the sound envelope decreases per tick while it is fading out.
     */
    private static final float FADE_OUT_STEP = 1.0f / (20.0f * FADE_OUT_SECONDS);

    /**
     * The ambient slider offset that keeps the trail sound below other environmental ambience.
     */
    private static final float AMBIENT_VOLUME_OFFSET = 0.5f;

    /**
     * Temporal volume envelope used to fade the sound in and out without pops.
     */
    private float envelope;

    /**
     * Desired volume envelope set by the trail renderer from proximity to the trail.
     */
    private float targetEnvelope;

    /**
     * Whether the sound should end once its terminal fade reaches silence.
     */
    private boolean released;

    /**
     * Constructs a trail sound instance at the supplied world position.
     *
     * @param x the world x coordinate
     * @param y the world y coordinate
     * @param z the world z coordinate
     */
    public TrailSoundInstance(double x, double y, double z) {
        super(ModSounds.TRAIL, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());

        this.looping = true;
        this.delay = 0;
        this.volume = 0.001F;
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        this.envelope = 0.0F;
        this.targetEnvelope = 1.0F;
        this.released = false;
    }

    /**
     * Updates the world position the sound is emitted from.
     *
     * @param x the world x coordinate
     * @param y the world y coordinate
     * @param z the world z coordinate
     */
    public void updatePosition(double x, double y, double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
    }

    /**
     * Sets the proximity-driven target envelope this sound should fade toward.
     *
     * @param target the target envelope in the range {@code [0, 1]}
     */
    public void setTargetEnvelope(float target) {
        this.released = false;
        this.targetEnvelope = Math.max(0.0F, Math.min(target, 1.0F));
    }

    /**
     * Releases the sound so it fades out and finishes once silent.
     */
    public void release() {
        this.released = true;
        this.targetEnvelope = 0.0F;
    }

    /**
     * Returns whether the trail sound should remain eligible for mixer updates.
     *
     * @return {@code true} while a client player exists
     */
    @Override
    public boolean canPlaySound() {
        return Minecraft.getInstance().player != null;
    }

    /**
     * Keeps the sound manager from discarding this instance between ticks.
     *
     * @return {@code true}
     */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    /**
     * Advances the fade envelope and updates the instance volume for the current ambient slider value.
     */
    @Override
    public void tick() {
        if (envelope > targetEnvelope) {
            envelope = Math.max(targetEnvelope, envelope - FADE_OUT_STEP);
        } else if (envelope < targetEnvelope) {
            envelope = Math.min(targetEnvelope, envelope + FADE_IN_STEP);
        }

        if (released && envelope == 0.0F) {
            volume = 0.0F;
            stop();
            return;
        }

        float maxVolume = maxVolume();
        if (maxVolume == 0.0F) {
            volume = 0.0F;
            return;
        }

        volume = Math.max(envelope * maxVolume, 0.001F);
    }

    /**
     * Computes the maximum effective instance volume from the current ambient slider setting.
     *
     * @return the ambient-corrected maximum volume for this sound
     */
    private float maxVolume() {
        float ambientVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.AMBIENT);
        if (ambientVolume <= AMBIENT_VOLUME_OFFSET) {
            return 0.0F;
        }

        return Math.min((ambientVolume - AMBIENT_VOLUME_OFFSET) / ambientVolume, 1.0F);
    }
}
