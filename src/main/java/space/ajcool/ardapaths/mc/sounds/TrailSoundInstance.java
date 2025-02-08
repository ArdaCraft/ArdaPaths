package space.ajcool.ardapaths.mc.sounds;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.mc.items.ModItems;

@Environment(value=EnvType.CLIENT)
public class TrailSoundInstance extends MovingSoundInstance {
    public ArdaPathsClient.AnimatedTrail animatedTrail;

    public long lastTick;
    public long timeAlive;

    public TrailSoundInstance(ArdaPathsClient.AnimatedTrail animatedTrail) {
        super(ModSounds.TRAIL, SoundCategory.NEUTRAL, SoundInstance.createRandom());

        this.timeAlive = 0;
        this.lastTick = System.currentTimeMillis();

        this.animatedTrail = animatedTrail;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.01F;

        this.x = (float) animatedTrail.CurrentPosition.x;
        this.y = (float) animatedTrail.CurrentPosition.y;
        this.z = (float) animatedTrail.CurrentPosition.z;
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

        if (volume == 0)
        {
            this.setDone();
            return;
        }

        this.x = (float) animatedTrail.CurrentPosition.x;
        this.y = (float) animatedTrail.CurrentPosition.y;
        this.z = (float) animatedTrail.CurrentPosition.z;

        if (timeAlive < 1500) volume = Math.max(((float) timeAlive / 1500) * 0.5F, 0.001F);
        else if (volume > 0) volume = Math.max(volume - 0.02F, 0);
    }
}

