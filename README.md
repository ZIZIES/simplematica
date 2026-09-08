Simplematica
============

A stripped-down fork of [Litematica](https://github.com/maruohon/litematica) that keeps the
"load a schematic and build it" workflow and cuts the rest, plus a material list for the layer
you are actually standing on.

Client-side Fabric mod for Minecraft 26.2.

What it does
------------

* Loads schematics and renders them as a hologram
* Area selection, and saving a selection as a schematic
* Render Layers, for viewing one Y slice at a time
* Placement and alignment: move, rotate, mirror, nudge
* **A per-layer material list** -- see below

Per-layer material list
-----------------------

Litematica's material list is one flat total for the whole schematic, which does not answer the
question you have while building: *what do I need for this layer?*

Simplematica walks each sub-region's block state container once, buckets the non-air counts by
world Y and caches the result, so the material list can show:

* **Current Layer** -- exactly what the render layer is showing
* **Layer + Below** -- everything from the schematic floor up to the current layer, for building
  upwards
* **All** / **Render Layers** -- the original whole-schematic and world-scan views

The per-layer views follow the render layer as you move through it, and cost nothing per frame:
the tally is only rebuilt when the placement actually changes.

Only the Y axis is served from the cache. Render layers can also run along X or Z; those fall
back to the original block counting task, which does the full placement transform.

What was removed
----------------

* The creative in-world editing toolbox: Fill, Replace, Move, Delete, Paste, Grid Paste, Rebuild
* Schematic Projects, the versioned-build VCS feature
* The config options and hotkeys that existed only to serve the above

Building
--------

Needs JDK 25.

    gradlew build

The jar lands in `build/libs/`. To have every build dropped straight into a Minecraft instance,
point `mods_dir` at its mods folder in `gradle.properties`, or per-run:

    gradlew build -Pmods_dir="C:/path/to/instance/mods"

Older builds of this mod are cleared out of that folder first, since Fabric Loader refuses to
launch with two jars declaring the same mod id. Leave `mods_dir` empty to turn the copy off.

Credits and license
-------------------

Litematica was created by [masa (maruohon)](https://github.com/maruohon), and the Minecraft 26.2
branch this fork is based on is maintained by
[Sakura-Ryoko](https://github.com/sakura-ryoko/litematica). All of the hard parts -- the schematic
formats, the renderer, the placement maths -- are theirs.

Simplematica is licensed under the **GNU LGPL v3**, the same as Litematica. See `LICENSE.txt`.

It depends on [malilib](https://github.com/maruohon/malilib), also by masa, which is pulled
automatically from maven at build time.
