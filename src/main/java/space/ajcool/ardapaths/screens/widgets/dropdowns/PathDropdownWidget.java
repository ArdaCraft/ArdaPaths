package space.ajcool.ardapaths.screens.widgets.dropdowns;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.config.shared.PathData;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class PathDropdownWidget extends DropdownWidget<PathData> {
    public PathDropdownWidget(int x, int y, int width, int height, String defaultValue) {
        super(x, y, width, height, 8);
        this.setOptions(ArdaPathsClient.CONFIG.getPaths());
        this.setSelected(defaultValue);
    }

    public PathDropdownWidget(int x, int y, int width, int height, String defaultValue, Consumer<PathData> onSelect) {
        this(x, y, width, height, defaultValue);
        this.setItemSelectedListener(onSelect);
    }

    @Override
    public Text getItemDisplay(PathData item) {
        if (item == null) {
            return Text.literal("Select a Path");
        } else {
            return Text.literal(item.getName()).fillStyle(Style.EMPTY.withColor(item.getColor().asHex()));
        }
    }

    public void setSelected(String pathId) {
        for (PathData path : this.getOptions()) {
            if (path == null) continue;
            if (path.getId().equals(pathId)) {
                this.setSelected(path);
                return;
            }
        }
    }
}

