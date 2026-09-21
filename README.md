# ArdaPaths
***Customize and reveal guiding paths for players to follow in the world.***

ArdaPaths is a Fabric mod designed to immerse players in a world by allowing configurable paths to show animated trails.

The mod is initially configured for use on ArdaCraft and with their recreation of Middle-earth. With ArdaPaths, players can embark on their own journey through Middle-earth like never before, tracing the iconic paths of beloved characters from the Lord of the Rings series.

Whether it's Frodo's perilous trek to Mount Doom or Aragorn's valiant march to reclaim his throne, each path can be meticulously plotted using the features in this mod.

## Requirements

- Minecraft 26.1.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.155.2+26.1.2 or a compatible newer 26.1.2 build
- Java 25

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

<img alt="Pathfinder UI" align="left" width="280" src="https://github.com/user-attachments/assets/90e8b6a0-28f9-46ca-9170-7625fca536c2">

- `Chapter Start` indicate if this marker is the start of the chapter.
- `Show title on trail` indicate if the chapter title should be displayed on the trail when the chapter becomes active.
- `Proximity messaage` the message displayed to players when they are within range of the marker
- `Activation range` the range (in blocks) at which the proximity message is displayed. Weather also uses this range, with a 3-block minimum so markers without proximity messages can still trigger weather.
- `Weather` the weather to apply when the marker is followed, or undefined to leave the current weather unchanged
- `Date and Time` the date and time to apply when the marker is followed, in `DD/MM/YYYY HH:MM` format, or blank/null to leave the current time unchanged. This can be authored whether or not the client-side DaylightChangerStruggle mod is installed.
- `Target Marker Date and Time` and `Target Marker ID` let the server spread date-time values from the current marker to another marker in the current editor path and chapter. Copy a marker ID with the marker ID button at the top of the editor; IDs use `<packed block position>`. The editor's selected path and chapter are used when saving. Spreads can cover multiple in-fiction days, and if the target date-time is earlier than the source, intermediate markers move backwards through time.
- `Clear range` clears the date-time and activation mode on every marker from the current marker to the target marker, and removes the target marker settings from the current marker.
- `Activation` chooses how marker time is applied: `Computed` interpolates from the previous timed marker along the trail, including across multiple dates, while `Marker Range` eases inside this marker's activation range. A marker's time stays in force after the marker is passed, until another time marker or computed segment takes over, the trail is left, the Pathfinder is sheathed, or dynamic environment is switched off.
- `Auto-Teleport Target` coordinates or a HuskHomes warp name triggered when a player reaches the marker.
- `Give Item` an item identifier granted when a player reaches the marker, or `clear` to store the held item back in the inventory.
- `Look At` coordinates (`x y z`) for a point of interest the player can focus on while holding the Pathfinder within 10 blocks of the marker.
- `R Speed` the speed at which the message is displayed to the player (lower is faster)
- `F Delay` Base delay (in ms) applied before any text fading begins. This value ensures a minimum fade delay regardless of the text length.
- `F Factor` Additional delay applied per character. Longer text results in a longer total fade delay.
- `Opacity` the minimum opacity of the text before the message disappear completely


#### Marker links screen

A marker can belong to multiple paths and chapters. The `Edit Links` button on the marker configuration screen allows breaking these links.

#### Chapter Configuration screen

Accessed through the marker configuration screen by clicking the `Edit Chapters` button.

<img alt="Pathfinder Chapter UI" align="left" width="280" src="https://github.com/user-attachments/assets/1ce9b230-9dfe-4772-8dc8-f74100370acf">

- `id` a unique identifier for the chapter
- `name` the name of the chapter. Will be displayed on chapter changed on a trail when using the Pathfinder if the user enabled the chapter titles display
- `date` the date associated with this message / event (display purposes only)
- `index` the index of the chapter : this will determine its position in the chapter selection dropdowns and is used in dertermining which chapter to switch to when changing chapters.
- `warp` the optional HuskHomes warp name for this chapter. Warp names stay separate from coordinates.
- `Coordinates` the optional `x`, `y`, and `z` chapter-start position. Coordinates are preferred over warp resolution when returning to a chapter start.
- `Dimension` the dimension identifier for the coordinates, such as `minecraft:overworld`. It is required when coordinates are set and is saved together with them.

<br/><br/><br/>

##### Notes

- A new Chapter can be added by clicking the `+` next to the chapter selection dropdown
- The current chapter can be deleted by clicking the `-` next to the chapter selection dropdown (note, the default chapter for a given path _cannot be deleted_)
- The current **Path colors** can be adjusted, all colors are expected to be hex color values (ie: #eab113). The path colors define the trail, the chapters dropdown and the title colors.
- The **Path colors** can be changed without editing a chapter (click the `apply` button to save the changes). Color changes are also saved when editing a chapter and clicking the `save` button.
- `Hide Default chapter` hides the path's `default` chapter from the Pathfinder chapter dropdown while keeping it editable in the chapter editor.

### Pathfinder

The pathfinder is an item that allows players to select and follow paths. When used, the pathfinder opens the following configuration screen.

<img alt="Pathfinder client UI" align="left" width="280" src="https://github.com/user-attachments/assets/46c50ff2-9468-4bad-ac23-b291f2142336">

- `Select a Path to Follow` dropdown allows selecting a character's path to follow
- `Select a Chapter` dropdown allows selecting a chapter from the book within the selected path
- `Return to Path` teleports the player to the last visited path marker for the selected chapter
- `Return to Chapter Start` teleports the player to the chapter's configured coordinates, or to its warp if no coordinates are set and a warp resolver is available.
- `Proximity text` toggle the display of proximity messages when approaching markers
- `Text speed multiplier` - Default **100% of set value** adjusts the speed at which proximity messages are displayed (lower is faster)
- `Chapter titles` - Default **Off** - toggle the display of chapter titles when reaching a new chapter marker
- `Chapter title fade delay` - Default **2 seconds** - the speed at which chapter titles fade out (lower is faster)
- `Auto walk speed` - Default **100% of walking speed** - adjusts the speed used by auto-walk movement
- `Show trail waypoints` - Default **Off** - toggles waypoint markers for the next trail node
- `Dynamic environmental effects` - Default **Off** - allows trail markers to change the time of day and the weather as you follow a path. Requires the client-side [DaylightChangerStruggle](https://github.com/JuggleStruggle/DaylightChangerStruggle) mod for time changes and the client-side [Weather Changer](https://github.com/Lucaslah/WeatherChanger) mod for weather changes - this option is hidden from the Pathfinder screen when neither is installed, and each effect is skipped when its mod is missing. Leaving the trail, disconnecting, changing dimensions, or turning this option off restores server-controlled weather.
- `baseline_date` (config file only) - Default **04/09/3006 12:00** - anchors the current world time to an Arda date before the first timed marker takes over, so dynamic environmental effects begin from the expected date frame.

While holding the Pathfinder near a marker with `Look At` coordinates, an eye prompt appears above the hotbar. Hold the Focus key, `Left Alt` by default, to ease the camera onto the authored point and keep it there; release the key to ease back to the view you had when you pressed it. When auto-walk is active and no nearby look-at target is available, pressing Focus immediately recentres the camera onto the trail instead of waiting for the normal idle delay.

## Path Configuration

By default, ArdaPaths is configured for use on the ArdaCraft server with four built-in paths of characters from the LOTR series.

New paths can be added to the `arda-paths/server.json` file with the following format. Set `hideDefault` to `true` on a path when its `default` chapter should stay editable but not appear in the Pathfinder chapter dropdown:

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
            "hideDefault": false,
            "chapters": {
                "default": {
                    "id": "default",
                    "name": "Default",
                    "index": 0,
                    "warp": "warpLocation",
                    "coordinates": {
                        "x": -56,
                        "y": 16,
                        "z": 12
                    },
                    "dimension": "minecraft:overworld"
                }
            }
        }
    ]
}
```

Chapter starts resolve in this order: configured `coordinates` with `dimension`, then a resolvable `warp`, then missing. If a warp exists but the optional warp integration is unavailable or cannot resolve it, editor marker lists report an unresolvable chapter start instead of treating the chapter as unconfigured. When a warp-resolved start marker is found, or a backup finds a flagged chapter-start marker for a warp-backed chapter with no coordinates, ArdaPaths writes the coordinates back to `server.json` and syncs clients so later operations no longer depend on the warp.

## Credits

***Credit to Monsterfish_ for the pathfinder texture.***

***Credit to Ajcool & Paul for developing the mod.***
 
 
 
