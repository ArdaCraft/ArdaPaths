package space.ajcool.ardapaths;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.WaterFluid;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import space.ajcool.ardapaths.block.PathMarkerBlock;
import space.ajcool.ardapaths.block.PathMarkerBlockEntity;
import space.ajcool.ardapaths.screen.PathMarkerEditScreen;
import space.ajcool.ardapaths.screen.PathSelectionScreen;
import space.ajcool.ardapaths.sound.TrailSoundInstance;

import java.util.*;

public class ArdaPathsClient implements ClientModInitializer
{
    public static List<PathMarkerBlockEntity> tickingPathMarkersLastFrame = new ArrayList<>();
    private static List<PathMarkerBlockEntity> tickingPathMarkers = new ArrayList<>();

    public static List<AnimatedTrail> animationTrails = new ArrayList<>();
    public static TrailSoundInstance trailSoundInstance = null;

    public static class AnimatedTrail {
        public int PathId;
        public Vec3i Start;
        public Vec3i Offset;
        public long TimeAlive;
        public Vec3d CurrentPosition;

        public AnimatedTrail(int pathId, Vec3i start, Vec3i offset)
        {
            PathId = pathId;
            Start = start;
            Offset = offset;
            TimeAlive = 0;
            CurrentPosition = Vec3d.of(start);
        }
    }

    private static long lastTickTime = 0;

    public static void genericMethod()
    {
        ArdaPaths.LOGGER.info("{}", tickingPathMarkers);
        ArdaPaths.LOGGER.info("{}", tickingPathMarkersLastFrame);
    }

    @Environment(EnvType.CLIENT)
    public static class PathParticleProvider implements ParticleFactory<DefaultParticleType> {
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

    private PathMarkerBlockEntity pathMarkerInRange = null;
    private static boolean titleShowing = false;
    private static boolean titlePlayed = false;
    private static int titleAlive = 0;

    @Override
    public void onInitializeClient()
    {
        var markerSet = new ArrayList<>(ClientWorld.BLOCK_MARKER_ITEMS);
        markerSet.add(ArdaPaths.PATH_MARKER_ITEM);
        ClientWorld.BLOCK_MARKER_ITEMS = Set.copyOf(markerSet);

        ParticleFactoryRegistry.getInstance().register(ArdaPaths.PATH_PARTICLE_TYPE, PathParticleProvider::new);

        ColorProviderRegistry.ITEM.register((itemStack, i) ->
        {
            for (ArdaPathsConfig.PathSettings path : ArdaPaths.CONFIG.paths)
            {
                if (path.Id != PathSelectionScreen.selectedPathId) continue;

                return path.PrimaryColor.encodedColor();
            }

            return new ArdaPathsConfig.ColorRGB(100, 100, 100).encodedColor();
        }, ArdaPaths.PATH_REVEALER_ITEM);

        // TICKS

        HudRenderCallback.EVENT.register((drawContext, tickDelta) ->
        {
            //region Render proximity message
            if (pathMarkerInRange == null || !titleShowing || !ArdaPaths.CONFIG.markerText) return;

            titleAlive += 1;

            var gui = MinecraftClient.getInstance().inGameHud;
            var font = gui.getTextRenderer();

            var width = MinecraftClient.getInstance().getWindow().getScaledWidth();
            var height = MinecraftClient.getInstance().getWindow().getScaledHeight();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            var textLength = pathMarkerInRange.proximityMessage.length() + 1;
            var numChars = Math.max(Math.min((int) titleAlive / 5, textLength), 1);

            var splitMessage = pathMarkerInRange.proximityMessage.split("\n");

            var numCharsLeft = numChars;

            var lines = new ArrayList<Text>();

            for (String line : splitMessage)
            {
                if (line.length() < numCharsLeft)
                {
                    lines.add(Text.literal(line));
                    numCharsLeft -= line.length();
                }
                else
                {
                    var partialLine = Text.empty()
                            .append(Text.literal(line.substring(0, numCharsLeft - 1)))
                            .append(Text.literal(line.substring(numCharsLeft - 1, numCharsLeft)).formatted(Formatting.GRAY));

                    lines.add(partialLine);

                    numCharsLeft -= numCharsLeft;
                }

                if (numCharsLeft <= 0) break;
            }

            var opacity = 255;

            var fadeDelay = 500 + (textLength * 5);
            if (titleAlive > fadeDelay) opacity = 255 - ((titleAlive - fadeDelay) * 2);

            if (opacity <= 8) titleShowing = false;

            for (int i = 0; i < lines.size(); i++)
            {
                drawContext.drawCenteredTextWithShadow(font, lines.get(i), (width / 2), (height / 5) + (10 * i), ColorHelper.Argb.getArgb(opacity, 255, 255, 255));
            }

            RenderSystem.disableBlend();
            //endregion
        });

        ClientTickEvents.START_WORLD_TICK.register(level ->
        {
            if (PathMarkerBlock.selectedBlockPosition != null && MinecraftClient.getInstance().player != null && !MinecraftClient.getInstance().player.getMainHandStack().isOf(ArdaPaths.PATH_MARKER_ITEM))
            {
                PathMarkerBlock.selectedBlockPosition = null;

                var message = Text.empty()
                        .append(Text.literal("ArdaPaths: ").formatted(Formatting.DARK_AQUA))
                        .append(Text.literal("Deselected origin block.").formatted(Formatting.RED));

                MinecraftClient.getInstance().player.sendMessage(message);

            }
            else if (PathMarkerBlock.selectedBlockPosition != null)
            {
                var random = level.random;
                level.addParticle(ParticleTypes.COMPOSTER, PathMarkerBlock.selectedBlockPosition.getX() + random.nextDouble(), PathMarkerBlock.selectedBlockPosition.getY()+ random.nextDouble(), PathMarkerBlock.selectedBlockPosition.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);

            }
        });

        ClientTickEvents.END_WORLD_TICK.register(level ->
        {
            var player = MinecraftClient.getInstance().player;
            if (!level.isClient || player == null) return;

            var mainHandItem = player.getMainHandStack();
            var pathMarkersThisTick = List.copyOf(tickingPathMarkers);

            if (mainHandItem.isOf(ArdaPaths.PATH_MARKER_ITEM))
            {
                pathMarkersThisTick.forEach(pathMarkerBlockEntity ->
                {
                    if (animationTrails.stream().noneMatch(trail -> (new BlockPos(trail.Start).equals(pathMarkerBlockEntity.getPos()))))
                    {
                        pathMarkerBlockEntity.createTrail(PathSelectionScreen.selectedPathId);
                    }
                });
            }
            else if (!mainHandItem.isOf(ArdaPaths.PATH_REVEALER_ITEM))
            {
                animationTrails.clear();
                pathMarkerInRange = null;
                trailSoundInstance = null;

                titleShowing = false;
                titlePlayed = false;

                return;
            }

            var currentTime = System.currentTimeMillis();
            var delta = currentTime - lastTickTime;
            lastTickTime = currentTime;

            var animationTrailsThisTick = List.copyOf(animationTrails);

            for (var animatedTrail : animationTrailsThisTick)
            {
                var start3d = new Vec3d(animatedTrail.Start.getX(), animatedTrail.Start.getY(), animatedTrail.Start.getZ());
                var offset3d = new Vec3d(animatedTrail.Offset.getX(), animatedTrail.Offset.getY(), animatedTrail.Offset.getZ());

                animatedTrail.TimeAlive += delta;

                var timeAliveSeconds = animatedTrail.TimeAlive / 1000f;
                var totalTimeSeconds =  offset3d.length() / 4.317f;
                var animationPoint = (float) (timeAliveSeconds / totalTimeSeconds);

                if (animationPoint >= 1)
                {
                    var stopBlockPos = new BlockPos(animatedTrail.Start.add(animatedTrail.Offset));
                    var optionalMarkerAtBlockPos = level.getBlockEntity(stopBlockPos, ArdaPaths.PATH_MARKER_BLOCK_ENTITY);
                    if (!mainHandItem.isOf(ArdaPaths.PATH_MARKER_ITEM) && optionalMarkerAtBlockPos.isPresent())
                    {
                        var markerAtPos = optionalMarkerAtBlockPos.get();
                        markerAtPos.createTrail(animatedTrail.PathId);
                    }

                    animationTrails.remove(animatedTrail);
                    continue;
                }

                var currentTrailPosition = start3d.add(offset3d.multiply(animationPoint, animationPoint, animationPoint)).add(0.5, 0.5, 0.5);
                var currentBlockPos = new BlockPos((int) Math.floor(currentTrailPosition.x), (int) Math.floor(currentTrailPosition.y), (int) Math.floor(currentTrailPosition.z));
                var currentBlockState = level.getBlockState(currentBlockPos);
                var inAir = currentBlockState.isAir() || currentBlockState.isOf(ArdaPaths.PATH_MARKER_BLOCK);

                for (int i = 0; i <= 10; i++)
                {
                    var checkPos = currentBlockPos.add(new Vec3i(0, inAir ? -i : i, 0));
                    var checkBlockPos = new BlockPos(checkPos);
                    var checkBlockState = level.getBlockState(checkBlockPos);

                    if (inAir && (checkBlockState.isAir() || checkBlockState.isOf(ArdaPaths.PATH_MARKER_BLOCK))) continue;

                    if (!inAir)
                    {
                        if (!checkBlockState.isAir() && !checkBlockState.isOf(ArdaPaths.PATH_MARKER_BLOCK)) continue;

                        checkBlockPos = new BlockPos( currentBlockPos.add(new Vec3i(0, i - 1, 0)));
                        checkBlockState = level.getBlockState(checkBlockPos);
                    }

                    var voxelShape = checkBlockState.getOutlineShape(level, checkBlockPos);
                    var max = voxelShape.getMax(Direction.Axis.Y);
                    currentTrailPosition = new Vec3d(currentTrailPosition.getX(), (float) (checkBlockPos.getY() + (max > 0 ? max : 1)), currentTrailPosition.getZ());
                    break;
                }

                var playerPosition = player.getPos();
                var distanceToTrail = playerPosition.distanceTo(currentTrailPosition);

                if (distanceToTrail > (mainHandItem.isOf(ArdaPaths.PATH_MARKER_ITEM) ? 100 : 15))
                {
                    animationTrails.remove(animatedTrail);
                    continue;
                }

                for (ArdaPathsConfig.PathSettings path : ArdaPaths.CONFIG.paths)
                {
                    if (path.Id != animatedTrail.PathId) continue;

                    animatedTrail.CurrentPosition = currentTrailPosition;

                    level.addParticle(ArdaPaths.PATH_PARTICLE_TYPE, currentTrailPosition.getX(), currentTrailPosition.getY() + 0.3, currentTrailPosition.getZ(),
                            path.PrimaryColor.encodedColor(), path.SecondaryColor.encodedColor(), path.TertiaryColor.encodedColor());
                }
            }

            var playerPosition = player.getPos();

            PathMarkerBlockEntity closestPathMarker = null;
            var closestDistance = Double.MAX_VALUE;

            PathMarkerBlockEntity closestPoximityMessage = null;
            var closestProximityDistance = Double.MAX_VALUE;

            for (var pathMarker : pathMarkersThisTick)
            {
                var distance = playerPosition.distanceTo(pathMarker.position());

                if (!pathMarker.proximityMessage.isEmpty() && distance < closestProximityDistance)
                {
                    closestPoximityMessage = pathMarker;
                    closestProximityDistance = distance;
                }

                if (pathMarker.targetOffsets.containsKey(PathSelectionScreen.selectedPathId) && distance < closestDistance)
                {
                    closestPathMarker = pathMarker;
                    closestDistance = distance;
                }
            }

            if (ArdaPaths.CONFIG.markerText && !titlePlayed && !titleShowing && closestPoximityMessage != null && closestProximityDistance <= closestPoximityMessage.activationRange)
            {
                pathMarkerInRange = closestPoximityMessage;
                titlePlayed = true;

                titleShowing = true;
                titleAlive = 0;
            }

            if (!ArdaPaths.CONFIG.markerText || (closestPoximityMessage == null || closestProximityDistance > closestPoximityMessage.activationRange) && !titleShowing) titlePlayed = false;

            if (mainHandItem.isOf(ArdaPaths.PATH_REVEALER_ITEM) && animationTrailsThisTick.isEmpty() && closestPathMarker != null && closestDistance <= 10)
            {
                closestPathMarker.createTrail(PathSelectionScreen.selectedPathId);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if (PathSelectionScreen.callingForTeleport && MinecraftClient.getInstance().player != null)
            {
                var playerPosition = MinecraftClient.getInstance().player.getPos();

                Vec3d closestPosition = null;
                var closestDistance = Double.MAX_VALUE;

                for (PathMarkerBlockEntity tickingPathMarker : tickingPathMarkers)
                {
                    if (!tickingPathMarker.targetOffsets.containsKey(PathSelectionScreen.selectedPathId)) continue;

                    var dist = tickingPathMarker.position().distanceTo(playerPosition);

                    if (dist < closestDistance)
                    {
                        closestPosition = tickingPathMarker.position();
                        closestDistance = dist;
                    }
                }

                if (closestPosition != null)
                {
                    var packetBuffer = PacketByteBufs.create();

                    packetBuffer.writeDouble(closestPosition.x);
                    packetBuffer.writeDouble(closestPosition.y);
                    packetBuffer.writeDouble(closestPosition.z);

                    ClientPlayNetworking.send(ArdaPaths.PATH_PLAYER_TELEPORT_PACKET, packetBuffer);
                }

                PathSelectionScreen.callingForTeleport = false;
            }

            tickingPathMarkers.clear();
        });


    }

    public static void addToPathMarkerToTickingList(PathMarkerBlockEntity pathMarkerBlockEntity)
    {
        tickingPathMarkers.add(pathMarkerBlockEntity);
    }

    public static void addAnimationTrail(BlockPos start, BlockPos offset, int pathId)
    {
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;

        var mainHandItem = client.player.getMainHandStack();
        if ((!mainHandItem.isOf(ArdaPaths.PATH_REVEALER_ITEM) && !mainHandItem.isOf(ArdaPaths.PATH_MARKER_ITEM)) || start == null || offset == null) return;

        var animatedTrail = new AnimatedTrail(pathId, start, offset);
        animationTrails.add(animatedTrail);

        if (!mainHandItem.isOf(ArdaPaths.PATH_REVEALER_ITEM)) return;

        if (trailSoundInstance == null)
        {
            trailSoundInstance = new TrailSoundInstance(animatedTrail);
            MinecraftClient.getInstance().getSoundManager().play(trailSoundInstance);
        }
        else trailSoundInstance.animatedTrail = animatedTrail;

    }

    public static boolean checkCtrlHeld()
    {
        var level = MinecraftClient.getInstance().world;

        return level != null && level.isClient() && Screen.hasControlDown();
    }

    public static void openEditorScreen(PathMarkerBlockEntity pathMarkerBlockEntity)
    {
        var level = MinecraftClient.getInstance().world;

        if (level != null && level.isClient()) MinecraftClient.getInstance().setScreen(new PathMarkerEditScreen(pathMarkerBlockEntity));
    }

    public static void openSelectionScreen()
    {
        var level = MinecraftClient.getInstance().world;

        if (level != null && level.isClient()) MinecraftClient.getInstance().setScreen(new PathSelectionScreen());
    }

    public static int selectedTrailId()
    {
        var level = MinecraftClient.getInstance().world;

        if (level != null && level.isClient()) return PathSelectionScreen.selectedPathId;

        return 0;
    }
}
