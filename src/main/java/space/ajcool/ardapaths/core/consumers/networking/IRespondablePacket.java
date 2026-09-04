package space.ajcool.ardapaths.core.consumers.networking;

import java.util.UUID;

/**
 * Packet payload that carries its own request correlation identifier.
 *
 * @param <T> concrete packet type returned by the request-id wither
 */
public interface IRespondablePacket<T extends IRespondablePacket<T>> extends IPacket {

    /**
     * Placeholder request id used before a respondable packet is sent.
     */
    UUID UNASSIGNED_REQUEST_ID = new UUID(0L, 0L);

    /**
     * Gets the request id used to pair requests and responses.
     *
     * @return request correlation id
     */
    UUID requestId();

    /**
     * Creates a packet copy with the supplied request id.
     *
     * @param requestId request correlation id
     * @return packet copy carrying the supplied request id
     */
    T withRequestId(UUID requestId);
}
