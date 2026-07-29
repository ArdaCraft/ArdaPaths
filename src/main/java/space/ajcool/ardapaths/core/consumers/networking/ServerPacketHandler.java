package space.ajcool.ardapaths.core.consumers.networking;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import space.ajcool.ardapaths.core.PermissionHelper;

import java.util.function.Function;

/**
 * Abstract base class for fire-and-forget server-side packet handlers.
 * Implementations receive packets sent from the client without needing to send a response.
 *
 * @param <T> the type of packet this handler processes
 */
@Slf4j(topic = "ardapaths")
public abstract class ServerPacketHandler<T extends IPacket> extends PacketHandler implements IServerPacketHandler<T> {
    /**
     * Function to deserialize the packet from a PacketByteBuf.
     */
    private final Function<PacketByteBuf, T> reader;

    /**
     * Constructs a ServerPacketHandler with a channel name and packet reader.
     *
     * @param channel the channel name for this packet handler
     * @param reader  function to deserialize packets from PacketByteBuf
     */
    public ServerPacketHandler(final String channel, final Function<PacketByteBuf, T> reader) {
        super(channel);
        this.reader = reader;
    }

    /**
     * Internal handler that deserializes the packet and delegates to the abstract handle method.
     *
     * @param server  the Minecraft server
     * @param player  the player who sent the packet
     * @param handler the network handler
     * @param buf     the packet data
     * @param sender  the packet sender
     */
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        T packet = reader.apply(buf);
        server.execute(() -> {
            if (requiresEditPermission() && !PermissionHelper.hasEditPermission(player)) {
                log.warn("Rejected unauthorized packet on {} from {}", getChannelId(), player.getUuidAsString());
                return;
            }

            handle(server, player, handler, packet, sender);
        });
    }

    /**
     * Indicates whether this handler requires the sender to have ArdaPaths edit permission.
     *
     * @return true when packets handled by this channel mutate editable path state
     */
    protected boolean requiresEditPermission() {
        return false;
    }

    /**
     * Handles the deserialized packet. Implementations should perform server-side logic
     * such as modifying world state, updating configurations, or logging events.
     *
     * @param server  the Minecraft server
     * @param player  the player who sent the packet
     * @param handler the network handler
     * @param packet  the deserialized packet
     * @param sender  the packet sender
     */
    protected abstract void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, T packet, PacketSender sender);
}
