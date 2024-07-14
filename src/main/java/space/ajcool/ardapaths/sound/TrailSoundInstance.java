package space.ajcool.ardapaths.sound;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.ArdaPathsClient;

@Environment(value=EnvType.CLIENT)
public class TrailSoundInstance extends AbstractTickableSoundInstance {
    public ArdaPathsClient.AnimatedTrail animatedTrail;

    public long lastTick;
    public long timeAlive;

    public TrailSoundInstance(ArdaPathsClient.AnimatedTrail animatedTrail) {
        super(ArdaPaths.TRAIL_SOUND_EVENT, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());

        this.timeAlive = 0;
        this.lastTick = System.currentTimeMillis();

        this.animatedTrail = animatedTrail;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;

        this.x = (float) animatedTrail.CurrentPosition.x;
        this.y = (float) animatedTrail.CurrentPosition.y;
        this.z = (float) animatedTrail.CurrentPosition.z;
    }

    @Override
    public boolean canPlaySound() {
        if (Minecraft.getInstance().player == null) return false;
        var mainHandItem = Minecraft.getInstance().player.getMainHandItem();

        return mainHandItem.is(ArdaPaths.PATH_REVEALER_ITEM);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        var delta = System.currentTimeMillis() - lastTick;

        timeAlive += delta;
        lastTick = System.currentTimeMillis();

        if (volume == 0)
        {
            this.stop();
            return;
        }

        this.x = (float) animatedTrail.CurrentPosition.x;
        this.y = (float) animatedTrail.CurrentPosition.y;
        this.z = (float) animatedTrail.CurrentPosition.z;

        if (timeAlive < 1500) volume = Math.max(((float) timeAlive / 1500) * 0.5F, 0.001F);
        else if (volume > 0) volume = Math.max(volume - 0.02F, 0);
    }
}

