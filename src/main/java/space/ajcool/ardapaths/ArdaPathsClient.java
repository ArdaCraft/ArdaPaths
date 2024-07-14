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
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.GlowParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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
        public Vec3 Start;
        public Vec3 Offset;
        public long TimeAlive;
        public Vec3 CurrentPosition;

        public AnimatedTrail(int pathId, Vec3 start, Vec3 offset)
        {
            PathId = pathId;
            Start = start;
            Offset = offset;
            TimeAlive = 0;
            CurrentPosition = start;
        }
    }

    private static long lastTickTime = 0;

    public static void genericMethod()
    {
        ArdaPaths.LOGGER.info(tickingPathMarkers + "");
        ArdaPaths.LOGGER.info(tickingPathMarkersLastFrame + "");
    }

    @Environment(EnvType.CLIENT)
    public static class PathParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public PathParticleProvider(SpriteSet spriteSet) {
            this.sprite = spriteSet;
        }

        public Particle createParticle(SimpleParticleType simpleParticleType, ClientLevel level, double x, double y, double z, double encodedColorA, double encodedColorB, double encodedColorC) {
            var glowParticle = new GlowParticle(level, x, y, z, 0.0, 0.0, 0.0, this.sprite)
            {
                @Override
                public int getLightColor(float f) {
                    BlockPos blockPos = new BlockPos(this.x, this.y, this.z);
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

            glowParticle.setParticleSpeed(xSpeed, ySpeed, zSpeed);
            glowParticle.setLifetime(level.random.nextInt(10) + 10);

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
        var markerSet = new ArrayList<>(ClientLevel.MARKER_PARTICLE_ITEMS);
        markerSet.add(ArdaPaths.PATH_MARKER_ITEM);
        ClientLevel.MARKER_PARTICLE_ITEMS = Set.copyOf(markerSet);

        ClientSpriteRegistryCallback.event(InventoryMenu.BLOCK_ATLAS).register(((atlasTexture, registry) -> {
            registry.register(new ResourceLocation(ArdaPaths.ModID, "particle/path"));
        }));

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

        HudRenderCallback.EVENT.register((poseStack, tickDelta) ->
        {
            //region Render proximity message
            if (pathMarkerInRange == null || !titleShowing) return;

            titleAlive += 1;

            var gui = Minecraft.getInstance().gui;
            var font = gui.getFont();

            poseStack.pushPose();

            var width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            var height = Minecraft.getInstance().getWindow().getGuiScaledHeight();

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            var textLength = pathMarkerInRange.proximityMessage.length() + 1;
            var numChars = Math.max(Math.min((int) titleAlive / 5, textLength), 1);

            var splitMessage = pathMarkerInRange.proximityMessage.split("\n");

            var numCharsLeft = numChars;

            var lines = new ArrayList<Component>();

            for (String line : splitMessage)
            {
                if (line.length() < numCharsLeft)
                {
                    lines.add(Component.literal(line));
                    numCharsLeft -= line.length();
                }
                else
                {
                    lines.add(
                            Component.empty()
                                    .append(Component.literal(line.substring(0,numCharsLeft - 1)))
                                    .append(Component.literal(line.substring(numCharsLeft - 1, numCharsLeft)).withStyle(ChatFormatting.GRAY))
                    );

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
                var lineWidth = font.width(splitMessage[i]);
                font.drawShadow(poseStack, lines.get(i), (width / 2) - (lineWidth / 2), (height / 5) + (10 * i), FastColor.ARGB32.color(opacity, 255, 255, 255));
            }


            RenderSystem.disableBlend();
            //endregion
        });

        ClientTickEvents.START_WORLD_TICK.register(level ->
        {
            if (PathMarkerBlock.selectedBlockPosition != null && Minecraft.getInstance().player != null && !Minecraft.getInstance().player.getMainHandItem().is(ArdaPaths.PATH_MARKER_ITEM))
            {
                PathMarkerBlock.selectedBlockPosition = null;

                var message = Component.empty()
                        .append(Component.literal("ArdaPaths: ").withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal("Deselected origin block.").withStyle(ChatFormatting.RED));

                Minecraft.getInstance().player.sendSystemMessage(message);

            }
            else if (PathMarkerBlock.selectedBlockPosition != null)
            {
                var random = level.random;
                level.addParticle(ParticleTypes.COMPOSTER, PathMarkerBlock.selectedBlockPosition.getX() + random.nextDouble(), PathMarkerBlock.selectedBlockPosition.getY()+ random.nextDouble(), PathMarkerBlock.selectedBlockPosition.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);

            }
        });

        ClientTickEvents.END_WORLD_TICK.register(level ->
        {
            var player = Minecraft.getInstance().player;
            if (!level.isClientSide || player == null) return;

            var mainHandItem = player.getMainHandItem();
            var pathMarkersThisTick = List.copyOf(tickingPathMarkers);

            if (mainHandItem.is(ArdaPaths.PATH_MARKER_ITEM))
            {
                pathMarkersThisTick.forEach(pathMarkerBlockEntity ->
                {
                    if (animationTrails.stream().noneMatch(trail -> (new BlockPos(trail.Start.x, trail.Start.y, trail.Start.z)).equals(pathMarkerBlockEntity.getBlockPos())))
                    {
                        pathMarkerBlockEntity.createTrail(PathSelectionScreen.selectedPathId);
                    }
                });
            }
            else if (!mainHandItem.is(ArdaPaths.PATH_REVEALER_ITEM))
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
                var start = animatedTrail.Start;
                var offset = animatedTrail.Offset;

                animatedTrail.TimeAlive += delta;

                var timeAliveSeconds = animatedTrail.TimeAlive / 1000f;
                var totalTimeSeconds =  animatedTrail.Offset.length() / 4.317f;
                var animationPoint = (float) (timeAliveSeconds / totalTimeSeconds);

                if (animationPoint >= 1)
                {
                    var stop = new Vec3(start.x() + offset.x(), start.y() + offset.y(), start.z() + offset.z());
                    var blockPos = new BlockPos(stop.x(), stop.y() , stop.z());
                    var optionalMarkerAtPos = level.getBlockEntity(blockPos, ArdaPaths.PATH_MARKER_BLOCK_ENTITY);
                    if (!mainHandItem.is(ArdaPaths.PATH_MARKER_ITEM) && optionalMarkerAtPos.isPresent())
                    {
                        var markerAtPos = optionalMarkerAtPos.get();
                        markerAtPos.createTrail(animatedTrail.PathId);
                    }

                    animationTrails.remove(animatedTrail);
                    continue;
                }

                var trailPosition = start.add(offset.multiply(animationPoint, animationPoint, animationPoint));

                var currentBlockState = level.getBlockState(new BlockPos(trailPosition));
                var checkDown = currentBlockState.isAir() || currentBlockState.is(ArdaPaths.PATH_MARKER_BLOCK);

                for (int i = 0; i <= 10; i++)
                {
                    var blockPos = new BlockPos(trailPosition.add(new Vec3(0, i * (checkDown ? -1 : 1), 0)));
                    var blockState = level.getBlockState(blockPos);
                    if (blockState.isAir() || blockState.is(ArdaPaths.PATH_MARKER_BLOCK)) continue;

                    var voxelShape = blockState.getShape(level, blockPos);
                    var max = voxelShape.max(Direction.Axis.Y);

                    trailPosition = new Vec3(trailPosition.x(), (float) (blockPos.getY() + max), trailPosition.z());
                    break;
                }

                var playerPosition = player.position();
                var distanceToTrail = playerPosition.distanceTo(trailPosition);

                if (distanceToTrail > (mainHandItem.is(ArdaPaths.PATH_MARKER_ITEM) ? 100 : 15))
                {
                    animationTrails.remove(animatedTrail);
                    continue;
                }

                for (ArdaPathsConfig.PathSettings path : ArdaPaths.CONFIG.paths)
                {
                    if (path.Id != animatedTrail.PathId) continue;

                    animatedTrail.CurrentPosition = trailPosition;

                    level.addParticle(ArdaPaths.PATH_PARTICLE_TYPE, trailPosition.x(), trailPosition.y() + 0.3, trailPosition.z(),
                            path.PrimaryColor.encodedColor(), path.SecondaryColor.encodedColor(), path.TertiaryColor.encodedColor());
                }
            }

            var playerPosition = player.position();

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

            if (!titlePlayed && !titleShowing && closestPoximityMessage != null && closestProximityDistance <= closestPoximityMessage.activationRange)
            {
                pathMarkerInRange = closestPoximityMessage;
                titlePlayed = true;

                titleShowing = true;
                titleAlive = 0;
            }

            if ((closestPoximityMessage == null || closestProximityDistance > closestPoximityMessage.activationRange) && !titleShowing) titlePlayed = false;

            if (mainHandItem.is(ArdaPaths.PATH_REVEALER_ITEM) && animationTrailsThisTick.isEmpty() && closestPathMarker != null && closestDistance <= 10)
            {
                closestPathMarker.createTrail(PathSelectionScreen.selectedPathId);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if (PathSelectionScreen.callingForTeleport && Minecraft.getInstance().player != null)
            {
                var playerPosition = Minecraft.getInstance().player.position();

                Vec3 closestPosition = null;
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
        var client = Minecraft.getInstance();
        if (client.player == null) return;

        var mainHandItem = client.player.getMainHandItem();
        if ((!mainHandItem.is(ArdaPaths.PATH_REVEALER_ITEM) && !mainHandItem.is(ArdaPaths.PATH_MARKER_ITEM)) || start == null || offset == null) return;

        var xStart = start.getX() + 0.5f;
        var yStart = start.getY() + 0.5f;
        var zStart = start.getZ() + 0.5f;

        var animatedTrail = new AnimatedTrail(pathId, new Vec3(xStart, yStart, zStart), new Vec3(offset.getX(), offset.getY(), offset.getZ()));
        animationTrails.add(animatedTrail);

        if (!mainHandItem.is(ArdaPaths.PATH_REVEALER_ITEM)) return;

        if (trailSoundInstance == null)
        {
            trailSoundInstance = new TrailSoundInstance(animatedTrail);
            Minecraft.getInstance().getSoundManager().play(trailSoundInstance);
        }
        else trailSoundInstance.animatedTrail = animatedTrail;

    }

    public static boolean checkCtrlHeld()
    {
        var level = Minecraft.getInstance().level;

        return level != null && level.isClientSide() && Screen.hasControlDown();
    }

    public static void openEditorScreen(PathMarkerBlockEntity pathMarkerBlockEntity)
    {
        var level = Minecraft.getInstance().level;

        if (level != null && level.isClientSide()) Minecraft.getInstance().setScreen(new PathMarkerEditScreen(pathMarkerBlockEntity));
    }

    public static void openSelectionScreen()
    {
        var level = Minecraft.getInstance().level;

        if (level != null && level.isClientSide()) Minecraft.getInstance().setScreen(new PathSelectionScreen());
    }

    public static int selectedTrailId()
    {
        var level = Minecraft.getInstance().level;

        if (level != null && level.isClientSide()) return PathSelectionScreen.selectedPathId;

        return 0;
    }
}
