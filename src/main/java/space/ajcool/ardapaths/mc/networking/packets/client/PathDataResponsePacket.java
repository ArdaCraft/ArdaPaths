package space.ajcool.ardapaths.mc.networking.packets.client;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.config.server.ServerConfig;
import space.ajcool.ardapaths.config.shared.PathSettings;
import space.ajcool.ardapaths.mc.networking.ClientPacket;
import space.ajcool.ardapaths.paths.Paths;

/**
 * Packet sent from the server to the client containing path data.
 */
public class PathDataResponsePacket extends ClientPacket {
    public PathDataResponsePacket() {
        super("path_data_response");
    }

    /**
     * Create a new {@link PathDataResponsePacket} containing the given server config.
     *
     * @param config The path data to send
     */
    public PacketByteBuf create(ServerConfig config) {
        PacketByteBuf buf = PacketByteBufs.create();
        String json = new Gson().toJson(config);
        buf.writeString(json);
        return buf;
    }

    public void handle(PacketByteBuf buf) {
        String json = buf.readString(32767);
        try {
            ServerConfig config = new Gson().fromJson(json, ServerConfig.class);
            if (config == null) {
                return;
            }

            Paths.clearPaths();
            for (PathSettings path : config.paths) {
                Paths.addPath(path);
            }
            ArdaPathsClient.onPathDataInitialized();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
