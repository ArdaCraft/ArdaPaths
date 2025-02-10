package space.ajcool.ardapaths.config;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.config.client.ClientConfig;
import space.ajcool.ardapaths.config.shared.Color;
import space.ajcool.ardapaths.config.shared.PathData;
import space.ajcool.ardapaths.mc.items.ModItems;
import space.ajcool.ardapaths.mc.networking.PacketRegistry;
import space.ajcool.ardapaths.utils.Client;
import space.ajcool.ardapaths.utils.Compression;
import space.ajcool.ardapaths.utils.JsonUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ClientConfigManager extends ConfigManager<ClientConfig> {
    public ClientConfigManager(String configPath) {
        super(configPath);
    }

    @Override
    protected ClientConfig createDefault() {
        ClientConfig config = new ClientConfig();
        config.showProximityMessages(true);
        config.onlyRenderChapter(true);
        return config;
    }

    /**
     * Update the path data from the server.
     */
    public void updatePathData() {
        if (Client.isInSinglePlayer()) {
            ServerConfigManager serverConfigManager = ArdaPaths.CONFIG_MANAGER;
            this.onPathData(serverConfigManager.getConfig().getPaths());
        } else {
            PacketRegistry.PATH_DATA_REQUEST.sendToServer(buf -> {
                byte[] compressedJson = buf.readByteArray();
                String json = Compression.decompress(compressedJson);

                Type listType = new TypeToken<ArrayList<PathData>>(){}.getType();
                List<PathData> paths = JsonUtils.fromJson(json, listType);

                if (paths != null) {
                    this.onPathData(paths);
                }
            });
        }
    }

    /**
     * Called when path data is received from the server.
     *
     * @param paths The path data
     */
    public void onPathData(List<PathData> paths) {
        this.config.setPaths(paths);
        if (this.config.getSelectedPathId().isEmpty() && !paths.isEmpty()) {
            this.config.setSelectedPath(paths.get(0).getId());
        }
        this.save();

        ColorProviderRegistry.ITEM.register((itemStack, i) -> {
            for (PathData path : paths) {
                if (!path.getId().equalsIgnoreCase(this.config.getSelectedPathId())) continue;
                return path.getColor().asHex();
            }
            return Color.fromRgb(100, 100, 100).asHex();
        }, ModItems.PATH_REVEALER);
    }
}
