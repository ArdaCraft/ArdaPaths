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

@Environment(value=EnvType.CLIENT)
public class TrailSoundInstance extends MovingSoundInstance {
    public AnimatedTrail animatedTrail;

    public long lastTick;
    public long timeAlive;

    public TrailSoundInstance(AnimatedTrail animatedTrail) {
        super(ModSounds.TRAIL, SoundCategory.NEUTRAL, SoundInstance.createRandom());

        this.timeAlive = 0;
        this.lastTick = System.currentTimeMillis();

        this.animatedTrail = animatedTrail;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.01F;

        Vec3d currentPos = animatedTrail.getCurrentPos();
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

        if (volume == 0)
        {
            this.setDone();
            return;
        }

        Vec3d currentPos = animatedTrail.getCurrentPos();
        this.x = (float) currentPos.x;
        this.y = (float) currentPos.y;
        this.z = (float) currentPos.z;

        if (timeAlive < 1500) volume = Math.max(((float) timeAlive / 1500) * 0.5F, 0.001F);
        else if (volume > 0) volume = Math.max(volume - 0.02F, 0);
    }
}

