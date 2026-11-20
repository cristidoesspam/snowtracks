# Snow Tracks

A client-side Fabric mod for Minecraft 26.3. Walk through snow and leave footprints behind.
Nothing has to be installed on the server: it works in singleplayer and on any server.

## What you get

- **Footprints in snow.** Steps alternate left and right and point the way you are walking. Each step is two squares, one for the front of the foot and one for the heel. They show on snow layers and snow blocks.
- **They fade.** Prints fade out over about 60 seconds (adjustable). Snowfall or rain on a print makes it fade about four times faster, and new snow on top of it, or digging it away, removes it at once.
- **How you move matters.**
  - Sneaking: smaller, lighter prints, closer together.
  - Sprinting: longer strides.
  - Jumping, flying, swimming and riding: no prints.
- **Other players** leave prints you can see too.
- **Mob tracks.**
  - Paws: wolf, cat, ocelot, fox, polar bear, panda, armadillo, sniffer.
  - Hooves: horse, donkey, mule, skeleton horse, zombie horse, camel, llama, cow, mooshroom, sheep, pig, goat, hoglin, zoglin, ravager.
  - Bird feet: chicken, parrot.
  - Rabbits: a hop pattern (two long hind feet ahead of two small front paws) where each hop lands.
  - Prints scale with the mob's size, so a calf leaves smaller ones than a cow.
- **Other surfaces, each with its own look.**
  - Powder snow: big, soft, loose hollows that fall back in quickly.
  - Sand: shallow brown dents.
  - Mud: dark wet hollows with a bright rim that last longer.
- **Dragging tracks.** Something pulled by a lead (a boat, or an animal being held back) leaves a groove behind it.

## Settings

Open them from **Mod Menu** (Config button) or bind a key under *Controls > Snow Tracks*.

| Setting | Default | Meaning |
| --- | --- | --- |
| Snow Tracks | on | Master switch |
| Print Lifetime | 60 s | How long a print in snow lasts |
| Darkness | 80% | How strongly prints show |
| Other Players | on | Show other players' prints and their drag grooves |
| Mob Tracks | on | Show animal tracks |
| Max Prints | 600 | The most prints at once; the oldest go first, and animals give way before people (but keep a third) |

Settings are saved in `config/snowtracks.json`. There are also two bindable keys: open settings, and toggle the mod.

## How it works

Prints are ordinary particles lying flat on the ground, so the game does the culling, lighting and drawing, and a cap on the count keeps it cheap. Each tick the mod watches the entities within 48 blocks and, every stride they walk on the ground, puts a print on the surface under their feet. A print is skipped when it would hang over an edge, a step or a different surface.

## Building

```
gradlew build
```

The jar is `build/libs/snowtracks-1.0.0.jar`. Needs JDK 25, Fabric Loader 0.19.5+, Fabric API 0.163.0+26.3. Mod Menu is optional.

`tools/GenTextures.java` draws the sprites and the icon (`java tools/GenTextures.java src/main/resources/assets/snowtracks`).

## Limits

- Mobs from other mods are not recognised, and mobs outside the list above leave no tracks.
- A print only shows when the whole sprite fits on one level of one surface; on bumpy snow some steps are skipped.
- Shader packs that replace the particle pipeline may draw prints differently.
