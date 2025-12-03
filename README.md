# ArdaPaths
***Customize and reveal guiding paths for players to follow in the world.***

ArdaPaths is a Fabric mod designed to immerse players in a world by allowing configurable paths to show animated trails.

The mod is initially configured for use on ArdaCraft and with their recreation of Middle-earth. With ArdaPaths, players can embark on their own journey through Middle-earth like never before, tracing the iconic paths of beloved characters from the Lord of the Rings series.

Whether it's Frodo's perilous trek to Mount Doom or Aragorn's valiant march to reclaim his throne, each path can be meticulously plotted using the features in this mod.

## Blocks and Items

### Path Marker
This is the main item of the mod. When placed in the world, multiple path markers can be connected together. By using the path marker item on a path marker block, then selecting another path marker, you can set the second block as the target; a path will now appear between the two blocks.

Path markers can also have a message and a range configured that is displayed when players get close enough. Ctrl + Use with this item on an existing path marker allows you to configure the range and message.

Using this item on a path marker, then clicking on another, will set the second marker as the targeted path.

> [!IMPORTANT]
> A marker can be referenced by multiple paths and chapters.

#### Marker Configuration screen

Accessed by Ctrl + Use on a placed Path Marker block.

> [!IMPORTANT]
> When editing a marker, the configuration will be applied to current selected chapter on the Pathfinder. Make sure to select the correct chapter on the Pathfinder _before_ editing a marker.

The configuration screen allows editing of the marker properties. By default the current selected chapter on the Pathfinder is used to determine which chapter's marker properties are being edited. The path and chapter can be changed using the `Edit Data for Path` and `Chapter` dropdowns at the top of the screen.

A marker is configured as follows:
- `Chapter Start` indicate if this marker is the start of the chapter.
- `Show title on trail` indicate if the chapter title should be displayed on the trail when the chapter becomes active.
- `Proximity messaage` the message displayed to players when they are within range of the marker
- `Activation range` the range (in blocks) at which the proximity message is displayed
- `R Speed` the speed at which the message is displayed to the player (lower is faster)
- `F Delay` Base delay (in ms) applied before any text fading begins. This value ensures a minimum fade delay regardless of the text length.
- `F Factor` Additional delay applied per character. Longer text results in a longer total fade delay.
- `Opacity` the minimum opacity of the text before the message disappear completely

#### Marker links screen

A marker can belong to multiple paths and chapters. The `Edit Links` button on the marker configuration screen allows breaking these links.

#### Chapter Configuration screen

Accessed through the marker configuration screen by clicking the `Edit Chapters` button.

A chapter is configured as follows:
- `id` a unique identifier for the chapter
- `name` the name of the chapter. Will be displayed on chapter changed on a trail when using the Pathfinder if the user enabled the chapter titles display
- `date` the date associated with this message / event (display purposes only)
- `index` the index of the chapter : this will determine its position in the chapter selection dropdowns and is used in dertermining which chapter to switch to when changing chapters.
- `warp` the warp location for this chapter (this fuctionnality uses HuskHomes warp command). This is the teleport location when the user clicks `Return to Chapter Start` on his pathfinder. Acceptable values for this field are either a warp location or a set of coordinates (x y z) such as `-56 16 12`

##### Notes

- A new Chapter can be added by clicking the `+` next to the chapter selection dropdown
- The current chapter can be deleted by clicking the `-` next to the chapter selection dropdown (note, the default chapter for a given path _cannot be deleted_)
- The current **Path colors** can be adjusted, all colors are expected to be hex color values (ie: #eab113). The path colors define the trail, the chapters dropdown and the title colors.
- The **Path colors** can be changed without editing a chapter (click the `apply` button to save the changes). Color changes are also saved when editing a chapter and clicking the `save` button.

### Pathfinder

The pathfinder is an item that allows players to select and follow paths. When used, the pathfinder opens the following configuration screen.

- `Select a Path to Follow` dropdown allows selecting a character's path to follow
- `Select a Chapter` dropdown allows selecting a chapter from the book within the selected path
- `Return to Path` teleports the player to the last visited path marker for the selected chapter
- `Return to Chapter Start` teleports the player to the warp location defined for the selected chapter
- `Proximity text` toggle the display of proximity messages when approaching markers
- `Text speed multiplier` - Default **100% of set value** adjusts the speed at which proximity messages are displayed (lower is faster)
- `Chapter titles` - Default **Off** - toggle the display of chapter titles when reaching a new chapter marker
- `Chapter title fade delay` - Default **2 seconds** - the speed at which chapter titles fade out (lower is faster)

## Path Configuration

By default, ArdaPaths is configured for use on the ArdaCraft server with four built-in paths of characters from the LOTR series.

New paths can be added to the `arda-paths/server.json` file with the following format:

```json
{
    "paths": [
        {
            "id": "0",
            "name": "A Custom Path",
            "primary_color": {
                "red": 255,
                "green": 215,
                "blue": 0
            },
            "secondary_color": {
                "red": 230,
                "green": 194,
                "blue": 0
            },
            "tertiary_color": {
                "red": 255,
                "green": 227,
                "blue": 77
            },
            "chapters": {
                "default": {
                    "id": "default",
                    "name": "Default",
                    "date": "0",
                    "index": 0,
                    "warp": "warpLocation"
                }
            }
        }
    ]
}
```

## Credits

***Credit to Monsterfish_ for the pathfinder texture.***

***Credit to Ajcool & Paul for developing the mod.***
 
 
 