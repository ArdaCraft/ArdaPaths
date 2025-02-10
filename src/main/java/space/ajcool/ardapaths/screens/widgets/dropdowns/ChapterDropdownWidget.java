package space.ajcool.ardapaths.screens.widgets.dropdowns;

import net.minecraft.text.Text;
import space.ajcool.ardapaths.ArdaPathsClient;
import space.ajcool.ardapaths.config.shared.ChapterData;
import space.ajcool.ardapaths.config.shared.PathData;

import java.util.function.Consumer;

public class ChapterDropdownWidget extends DropdownWidget<ChapterData> {
    public ChapterDropdownWidget(int x, int y, int width, int height, String defaultValue) {
        super(x, y, width, height, 8);
        PathData selectedPath = ArdaPathsClient.CONFIG.getSelectedPath();
        if (selectedPath != null) {
            this.setOptions(selectedPath.getChapters());
            this.setSelected(defaultValue);
        }
    }

    public ChapterDropdownWidget(int x, int y, int width, int height, String defaultValue, Consumer<ChapterData> onSelected) {
        this(x, y, width, height, defaultValue);
        this.setItemSelectedListener(onSelected);
    }

    @Override
    public Text getItemDisplay(ChapterData option) {
        if (option == null) {
            return Text.literal("No Chapter Selected");
        } else {
            String text = option.getName();
            if (!option.getDate().isEmpty()) {
                text += " (" + option.getDate() + ")";
            }
            return Text.literal(text);
        }
    }

    public void setSelected(String chapterId) {
        for (ChapterData chapter : this.getOptions()) {
            if (chapter == null) continue;
            if (chapter.getId().equals(chapterId)) {
                this.setSelected(chapter);
                return;
            }
        }
    }
}
