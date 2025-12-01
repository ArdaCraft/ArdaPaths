package space.ajcool.ardapaths.core.data.config;

import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import space.ajcool.ardapaths.ArdaPaths;
import space.ajcool.ardapaths.core.Client;
import space.ajcool.ardapaths.core.data.Json;
import space.ajcool.ardapaths.core.data.config.client.ClientConfig;
import space.ajcool.ardapaths.core.data.config.shared.Color;
import space.ajcool.ardapaths.core.data.config.shared.PathData;
import space.ajcool.ardapaths.core.networking.PacketRegistry;
import space.ajcool.ardapaths.core.networking.packets.EmptyPacket;
import space.ajcool.ardapaths.mc.items.ModItems;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ClientConfigManager extends ConfigManager<ClientConfig>
{
    public ClientConfigManager(String configPath)
    {
        super(configPath);
    }

    @Override
    protected ClientConfig createDefault()
    {
        ClientConfig config = new ClientConfig();
        config.showProximityMessages(true);
        config.showChapterTitles(false);
        return config;
    }

    /**
     * Update the path data from the server.
     */
    public void updatePathData()
    {
        if (Client.isInSinglePlayer())
        {
            ServerConfigManager serverConfigManager = ArdaPaths.CONFIG_MANAGER;
            this.onPathData(serverConfigManager.getConfig().getPaths());
        }
        else
        {
            PacketRegistry.PATH_DATA_REQUEST.send(new EmptyPacket(), response ->
            {
                String json = response.json();

                Type listType = new TypeToken<ArrayList<PathData>>()
                {
                }.getType();

                List<PathData> paths = Json.fromJson(json, listType);

                if (paths != null)
                {
                    ArdaPaths.LOGGER.info("Updating path data");

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
    public void onPathData(List<PathData> paths)
    {
        this.config.setPaths(paths);

        if (this.config.getSelectedPathId().isEmpty() && !paths.isEmpty())
        {
            this.config.setSelectedPath(paths.get(0).getId());
        }

        this.save();

        ColorProviderRegistry.ITEM.register((itemStack, i) ->
        {
            for (PathData path : paths)
            {
                if (!path.getId().equalsIgnoreCase(this.config.getSelectedPathId())) continue;
                return path.getPrimaryColor().asHex();
            }
            return Color.fromRgb(100, 100, 100).asHex();
        }, ModItems.PATH_REVEALER);
    }
}
