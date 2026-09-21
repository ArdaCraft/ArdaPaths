package space.ajcool.ardapaths.core.networking.packets.server;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import space.ajcool.ardapaths.core.ModConstants;
import space.ajcool.ardapaths.core.consumers.networking.IPacket;

/**
 * Packet sent from client to server to update a path's metadata and colors.
 * @param id the unique path identifier
 * @param name the path name
 * @param primaryColor the primary color in ARGB format
 * @param secondaryColor the secondary color in ARGB format
 * @param tertiaryColor the tertiary color in ARGB format
 * @param hideDefault whether the default chapter should be hidden from player-facing chapter lists
 */
public record PathDataUpdatePacket(
        String id,
        String name,
        int primaryColor,
        int secondaryColor,
        int tertiaryColor,
        boolean hideDefault
) implements IPacket
{
    /**
     * Network channel used for path metadata updates.
     */
    public static final ResourceLocation CHANNEL = ModConstants.modId("path_data_update_request");

    @Override
    public FriendlyByteBuf build()
    {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(id);
        buf.writeUtf(name);
        buf.writeInt(primaryColor);
        buf.writeInt(secondaryColor);
        buf.writeInt(tertiaryColor);
        buf.writeBoolean(hideDefault);
        return buf;
    }

    public static PathDataUpdatePacket read(FriendlyByteBuf buf)
    {
        final String pathId = buf.readUtf();
        final String pathName = buf.readUtf();
        final int pathPrimaryColor = buf.readInt();
        final int pathSecondaryColor = buf.readInt();
        final int pathTertiaryColor = buf.readInt();
        final boolean pathHideDefault = buf.readBoolean();
        return new PathDataUpdatePacket(pathId, pathName, pathPrimaryColor, pathSecondaryColor, pathTertiaryColor, pathHideDefault);
    }
}
