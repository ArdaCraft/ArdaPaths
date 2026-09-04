package space.ajcool.ardapaths.core.consumers.networking;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import space.ajcool.ardapaths.core.PermissionHelper;

import java.util.function.Function;

/**
 * Abstract base class for fire-and-forget server-side packet handlers.
 * Implementations receive packets sent from the client without needing to send a response.
 *
 * @param <T> the type of packet this handler processes
 */
@Slf4j(topic = "ardapaths")
public abstract class ServerPacketHandler<T extends IPacket> extends PacketHandler<T> implements IServerPacketHandler<T> {

    /**
     * Constructs a ServerPacketHandler with a channel identifier and packet reader.
     *
     * @param type   the payload type for this packet handler
     * @param reader function to deserialize packets from a friendly byte buffer
     */
    public ServerPacketHandler(final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<T> type, final Function<FriendlyByteBuf, T> reader) {
        super(type, IPacket.codec(reader));
    }

    /**
     * Handles a decoded packet and delegates to the concrete server operation.
     *
     * @param packet  the decoded packet
     * @param context the server networking context
     */
    public void receive(T packet, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (requiresEditPermission() && !PermissionHelper.hasEditPermission(player)) {
            log.warn("Rejected unauthorized packet on {} from {}", getChannelId(), player.getStringUUID());
            return;
        }

        handle(context.server(), player, player.connection, packet, context.responseSender());
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
     * @param sender  the response sender
     */
    @SuppressWarnings("unused")
    protected abstract void handle(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, T packet, PacketSender sender);
}
