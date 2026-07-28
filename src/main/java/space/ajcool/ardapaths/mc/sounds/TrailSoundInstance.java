package space.ajcool.ardapaths.mc.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.paths.rendering.objects.AnimatedTrail;

/**
 * A moving sound instance that plays trail ambience while the player holds the Path Revealer.
 * Follows the animated trail position and fades in/out with configurable volume.
 */
@Environment(value = EnvType.CLIENT)
public class TrailSoundInstance extends MovingSoundInstance {
    /**
     * The animated trail this sound follows.
     */
    public AnimatedTrail animatedTrail;

    /**
     * The timestamp of the last tick update.
     */
    public long lastTick;

    /**
     * Total time in milliseconds this sound has been alive.
     */
    public long timeAlive;

    /**
     * Constructs a trail sound instance for the given animated trail.
     * Initializes volume, position, and repeat settings.
     *
     * @param animatedTrail the trail to follow
     */
    public TrailSoundInstance(AnimatedTrail animatedTrail) {
        super(ModSounds.TRAIL, SoundCategory.NEUTRAL, SoundInstance.createRandom());

        this.timeAlive = 0;
        this.lastTick = System.currentTimeMillis();

        this.animatedTrail = animatedTrail;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.01F;

        Vec3d currentPos = animatedTrail.getCurrentRenderPos();

        this.x = (float) currentPos.x;
        this.y = (float) currentPos.y;
        this.z = (float) currentPos.z;
    }

    @Override
    public boolean canPlay() {
        if (MinecraftClient.getInstance().player == null) return false;
        var mainHandItem = MinecraftClient.getInstance().player.getMainHandStack();

        return mainHandItem.isOf(ModItems.PATH_REVEALER);
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public void tick() {
        var delta = System.currentTimeMillis() - lastTick;

        timeAlive += delta;
        lastTick = System.currentTimeMillis();

        if (volume == 0) {
            this.setDone();
            return;
        }

        Vec3d currentPos = animatedTrail.getCurrentRenderPos();

        this.x = (float) currentPos.x;
        this.y = (float) currentPos.y;
        this.z = (float) currentPos.z;

        if (timeAlive < 2000) volume = Math.max(((float) timeAlive / 2000) * 0.5F, 0.001F);
        else if (volume > 0) volume = Math.max(volume - 0.02F, 0);
    }
}

