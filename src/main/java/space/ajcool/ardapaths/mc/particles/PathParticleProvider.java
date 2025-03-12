package space.ajcool.ardapaths.mc.particles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class PathParticleProvider implements ParticleFactory<DefaultParticleType> {
    private final SpriteProvider sprite;

    public PathParticleProvider(SpriteProvider spriteSet) {
        this.sprite = spriteSet;
    }

    public Particle createParticle(DefaultParticleType simpleParticleType, ClientWorld level, double x, double y, double z, double encodedColorA, double encodedColorB, double encodedColorC) {
        var glowParticle = new GlowParticle(level, x, y, z, 0.0, 0.0, 0.0, this.sprite)
        {
            @Override
            public int getBrightness(float f) {
                BlockPos blockPos = new BlockPos((int) this.x, (int) this.y, (int) this.z);
                var lightColor = WorldRenderer.getLightmapCoordinates(this.world, blockPos);

                int j = lightColor & 0xFF;
                int k = lightColor >> 16 & 0xFF;

                float brightness = MathHelper.clamp(((float) this.maxAge - ((float) this.age + f)) / (float) this.maxAge, 0.0f, 1.0f);

                if ((j += (int) (brightness * 240)) > 240) {
                    j = 240;
                }

                return j | k << 16;
            }
        };

        var rand =  level.random.nextDouble();

        float r = ((int) encodedColorA >> 16) & 0x0ff;
        float g = ((int) encodedColorA >> 8) & 0x0ff;
        float b = (int) encodedColorA & 0x0ff;

        if (encodedColorB != 0 && rand >= (encodedColorC == 0 ? 0.5 : 0.3333)) {
            r = ((int) encodedColorB >> 16) & 0x0ff;
            g = ((int) encodedColorB >> 8) & 0x0ff;
            b = (int) encodedColorB & 0x0ff;
        } else if (encodedColorC != 0 && rand > 0.6666) {
            r = ((int) encodedColorC >> 16) & 0x0ff;
            g = ((int) encodedColorC >> 8) & 0x0ff;
            b = (int) encodedColorC & 0x0ff;
        }

        glowParticle.setColor(r / 255, g / 255, b / 255);

        double SPEED_FACTOR = 0.02;
        double xSpeed = ((level.random.nextDouble() * 2) - 1) * SPEED_FACTOR;
        double ySpeed = ((level.random.nextDouble() * 2) - 1) * SPEED_FACTOR;
        double zSpeed = ((level.random.nextDouble() * 2) - 1) * SPEED_FACTOR;

        glowParticle.setVelocity(xSpeed, ySpeed, zSpeed);
        glowParticle.setMaxAge(level.random.nextInt(10) + 10);

        return glowParticle;
    }
}

