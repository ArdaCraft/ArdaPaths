package space.ajcool.ardapaths.mc.networking;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

public interface ServerPacketHandler extends Packet {
    /**
     * Handle an incoming packet on the server.
     * Called from the global receiver in the PacketRegistry.
     */
    void handle(MinecraftServer server,
                ServerPlayerEntity player,
                ServerPlayNetworkHandler handler,
                PacketByteBuf buf,
                PacketSender sender);
}
