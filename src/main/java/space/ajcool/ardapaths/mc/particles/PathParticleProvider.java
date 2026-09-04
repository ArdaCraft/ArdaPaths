package space.ajcool.ardapaths.mc.particles;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

/**
 * Factory for creating custom glow particles used in ArdaPaths trail rendering.
 * Creates particles with dynamic colours, lighting, and velocity for visual effect.
 */
@Environment(EnvType.CLIENT)
public class PathParticleProvider implements ParticleProvider<PathParticleEffect> {

    /**
     * The sprite provider for particle rendering.
     */
    private final SpriteSet sprite;

    /**
     * Constructs a PathParticleProvider with the given sprite provider.
     *
     * @param spriteSet the sprite provider for particle textures
     */
    public PathParticleProvider(SpriteSet spriteSet) {
        this.sprite = spriteSet;
    }

    /**
     * Creates a glow particle with trail path colours and animation.
     * Selects one of the effect colours and randomizes the particle's visual drift.
     *
     * @param effect        the path particle colour payload
     * @param level         the client world
     * @param x             the x coordinate
     * @param y             the y coordinate
     * @param z             the z coordinate
     * @param ignoredSpeedX the unused x velocity supplied by the particle spawner
     * @param ignoredSpeedY the unused y velocity supplied by the particle spawner
     * @param ignoredSpeedZ the unused z velocity supplied by the particle spawner
     * @return the created particle
     */
    public Particle createParticle(PathParticleEffect effect, ClientLevel level, double x, double y, double z, double ignoredSpeedX, double ignoredSpeedY, double ignoredSpeedZ) {
        var glowParticle = new GlowParticle(level, x, y, z, 0.0, 0.0, 0.0, this.sprite) {
            @Override
            public int getLightColor(float f) {
                BlockPos blockPos = new BlockPos((int) this.x, (int) this.y, (int) this.z);
                var lightColor = LevelRenderer.getLightColor(this.level, blockPos);

                int j = lightColor & 0xFF;
                int k = lightColor >> 16 & 0xFF;

                float brightness = Mth.clamp(((float) this.lifetime - ((float) this.age + f)) / (float) this.lifetime, 0.0f, 1.0f);

                if ((j += (int) (brightness * 240)) > 240) {
                    j = 240;
                }

                return j | k << 16;
            }
        };

        int selectedColor = effect.selectColor(level.random);

        float r = (selectedColor >> 16) & 0x0ff;
        float g = (selectedColor >> 8) & 0x0ff;
        float b = selectedColor & 0x0ff;

        glowParticle.setColor(r / 255, g / 255, b / 255);

        double SPEED_FACTOR = 0.02;
        double xSpeed = ((level.random.nextDouble() * 2) - 1) * SPEED_FACTOR;
        double ySpeed = ((level.random.nextDouble() * 2) - 1) * SPEED_FACTOR;
        double zSpeed = ((level.random.nextDouble() * 2) - 1) * SPEED_FACTOR;

        glowParticle.setParticleSpeed(xSpeed, ySpeed, zSpeed);
        glowParticle.setLifetime(level.random.nextInt(10) + 10);

        return glowParticle;
    }
}
