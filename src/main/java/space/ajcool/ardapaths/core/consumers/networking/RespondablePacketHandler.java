package space.ajcool.ardapaths.core.consumers.networking;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import space.ajcool.ardapaths.ArdaPaths;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Abstract base class for bidirectional packet handlers that send a request and wait for a response.
 * Wraps each request with a UUID and dispatches responses to registered consumer callbacks.
 *
 * @param <T> the type of request packet
 * @param <U> the type of response packet
 */
@Slf4j(topic = "ardapaths")
public abstract class RespondablePacketHandler<T extends IPacket, U extends IPacket> extends PacketHandler implements IServerPacketHandler<T>, IClientPacketHandler {
    /**
     * Maximum time a response callback can remain pending before it is evicted.
     */
    private static final long RESPONSE_CONSUMER_TTL_MS = 30_000L;

    /**
     * All respondable handlers registered in this JVM, used to clear pending callbacks on disconnect.
     */
    private static final Set<RespondablePacketHandler<?, ?>> HANDLERS = ConcurrentHashMap.newKeySet();

    /**
     * Map of request UUIDs to response consumers waiting for their replies.
     */
    private final ConcurrentHashMap<UUID, ResponseConsumer<U>> responseConsumers = new ConcurrentHashMap<>();

    /**
     * Function to deserialize request packets from PacketByteBuf.
     */
    private final Function<PacketByteBuf, T> reader;

    /**
     * The channel identifier used for sending response packets back to the client.
     */
    @Getter
    private final Identifier responseChannelId;

    /**
     * Function to deserialize response packets from PacketByteBuf.
     */
    private final Function<PacketByteBuf, U> responseReader;

    /**
     * Constructs a RespondablePacketHandler with request and response channels.
     *
     * @param channel         the channel name for sending requests
     * @param reader          function to deserialize request packets
     * @param responseChannel the channel name for receiving responses
     * @param responseReader  function to deserialize response packets
     */
    public RespondablePacketHandler(
            final String channel,
            final Function<PacketByteBuf, T> reader,
            final String responseChannel,
            final Function<PacketByteBuf, U> responseReader
    ) {
        super(channel);
        this.reader = reader;
        responseChannelId = new Identifier(ArdaPaths.MOD_ID, responseChannel);
        this.responseReader = responseReader;
        HANDLERS.add(this);
    }

    /**
     * Sends a request packet and registers a consumer to handle the response.
     * The request is wrapped with a random UUID for tracking.
     *
     * @param packet   the request packet to send
     * @param consumer the callback to invoke when the response arrives, or null if no callback is needed
     */
    public void send(final T packet, final Consumer<U> consumer) {
        sweepExpiredResponseConsumers();
        UUID id = UUID.randomUUID();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(id);
        PacketByteBuf packetBuf = packet.build();
        buf.writeBytes(packetBuf);
        if (consumer != null) {
            responseConsumers.put(id, new ResponseConsumer<>(consumer, System.currentTimeMillis()));
        }
        ClientPlayNetworking.send(getChannelId(), buf);
    }

    /**
     * Internal handler that processes a request packet on the server and sends back a response.
     * Extracts the request UUID and packet data, delegates to the abstract handle method,
     * and sends the response packet back to the client.
     *
     * @param server  the Minecraft server
     * @param player  the player who sent the request
     * @param handler the network handler
     * @param buf     the request packet data with UUID prefix
     * @param sender  the packet sender
     */
    @Override
    public void handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        UUID requestId = buf.readUuid();
        T packet = reader.apply(buf);
        server.execute(() -> {
            CompletableFuture<U> responseFuture;
            try {
                responseFuture = handleAsync(server, player, handler, packet, sender);
            } catch (RuntimeException exception) {
                log.warn("Failed to handle ArdaPaths request on {}", getChannelId(), exception);
                responseFuture = CompletableFuture.completedFuture(errorResponse());
            }

            responseFuture.whenComplete((responsePacket, throwable) -> {
                U packetToSend = responsePacket;
                if (throwable != null) {
                    log.warn("Failed to complete ArdaPaths request on {}", getChannelId(), throwable);
                    packetToSend = errorResponse();
                }

                U finalPacketToSend = packetToSend;
                if (finalPacketToSend == null) {
                    return;
                }

                server.execute(() -> {
                    PacketByteBuf responseBuf = PacketByteBufs.create().writeUuid(requestId);
                    PacketByteBuf responsePacketBuf = finalPacketToSend.build();
                    responseBuf.writeBytes(responsePacketBuf);
                    sender.sendPacket(responseChannelId, responseBuf);
                });
            });
        });
    }

    /**
     * Creates a response packet for exceptional async failures.
     *
     * @return error response packet, or null when the handler cannot represent an error
     */
    protected U errorResponse() {
        return null;
    }

    /**
     * Processes the request packet and returns a response packet.
     * Implementations should perform server-side logic and generate an appropriate response.
     *
     * @param server  the Minecraft server
     * @param player  the player who sent the request
     * @param handler the network handler
     * @param packet  the deserialized request packet
     * @param sender  the packet sender
     * @return the response packet to send back to the client
     */
    public U handle(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, T packet, PacketSender sender) {
        throw new UnsupportedOperationException("Implement handle for synchronous replies, or override handleAsync.");
    }

    /**
     * Processes the request packet and returns a future response packet.
     * Override this for requests that need asynchronous work before replying.
     *
     * @param server  the Minecraft server
     * @param player  the player who sent the request
     * @param handler the network handler
     * @param packet  the deserialized request packet
     * @param sender  the packet sender
     * @return future response packet to send back to the client
     */
    public CompletableFuture<U> handleAsync(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, T packet, PacketSender sender) {
        return CompletableFuture.completedFuture(handle(server, player, handler, packet, sender));
    }

    /**
     * Internal handler that processes a response packet on the client side.
     * Extracts the request UUID and dispatches the response to the registered consumer.
     *
     * @param client  the Minecraft client
     * @param handler the network handler
     * @param buf     the response packet data with UUID prefix
     * @param sender  the packet sender
     */
    @Override
    public void handle(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender sender) {
        UUID requestId = buf.readUuid();
        U packet = responseReader.apply(buf);
        ResponseConsumer<U> responseConsumer = responseConsumers.remove(requestId);
        if (responseConsumer != null) {
            responseConsumer.consumer().accept(packet);
        }
    }

    /**
     * Clears pending response callbacks for every respondable handler.
     */
    public static void clearAllResponseConsumers() {
        HANDLERS.forEach(RespondablePacketHandler::clearResponseConsumers);
    }

    /**
     * Clears pending response callbacks for this handler.
     */
    private void clearResponseConsumers() {
        responseConsumers.clear();
    }

    /**
     * Removes stale response callbacks that will no longer receive a server reply.
     */
    private void sweepExpiredResponseConsumers() {
        long cutoff = System.currentTimeMillis() - RESPONSE_CONSUMER_TTL_MS;
        responseConsumers.entrySet().removeIf(entry -> entry.getValue().createdAtMs() < cutoff);
    }

    /**
     * Client-side callback awaiting a response packet.
     *
     * @param consumer    the callback to invoke when the response arrives
     * @param createdAtMs the wall-clock time when the request was sent
     */
    private record ResponseConsumer<U extends IPacket>(Consumer<U> consumer, long createdAtMs) {
    }
}
