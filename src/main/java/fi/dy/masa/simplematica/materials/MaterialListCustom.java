package fi.dy.masa.simplematica.materials;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.util.data.ItemType;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.simplematica.Simplematica;

/**
 * A material list that is created directly from item IDs and quantities,
 * bypassing the block-based schematic system. This allows tracking of any
 * item type including tools, food, potions, and other non-placeable items.
 * -
 * - From Stormatica by CubicMetre
 */
public class MaterialListCustom extends MaterialListBase
{
    public static final String JSON_FILE_EXTENSION = ".json";
    public static final String TEXT_FILE_EXTENSION = ".txt";
    private final String name;
    private final Path sourceFile;

    /**
     * Creates a custom material list from a map of items and quantities
     * @param name Display name for this material list
     * @param items Map of ItemType to quantity
     * @param sourceFile Optional source file path for reference
     */
    public MaterialListCustom(String name, Map<ItemType, Integer> items, @Nullable Path sourceFile)
    {
        super();
        this.name = name;
        this.sourceFile = sourceFile;

        this.materialListAll = ImmutableList.copyOf(
            MaterialListUtils.createMaterialListFromItems(items, Minecraft.getInstance().player)
        );
        this.refreshPreFilteredList();
        this.updateCounts();
    }

    /**
     * Loads a custom material list from a JSON file
     * Format:
     * {
     *   "name": "My Custom List",
     *   "items": [
     *     {"id": "minecraft:diamond_pickaxe", "count": 5},
     *     {"id": "minecraft:cooked_beef", "count": 64}
     *   ]
     * }
     *
     * @param file Path to the JSON file
     * @return MaterialListCustom instance or null if loading failed
     */
    @Nullable
    public static MaterialListCustom fromJsonFile(Path file)
    {
        try
        {
            String content = Files.readString(file);
            JsonElement element = JsonParser.parseString(content);

            if (!element.isJsonObject())
            {
                Simplematica.LOGGER.error("MaterialListCustom#fromJsonFile: Invalid JSON file '{}' - root must be an object", file);
                return null;
            }

            JsonObject root = element.getAsJsonObject();
            String name = root.has("name") ? root.get("name").getAsString() : file.getFileName().toString();

            if (!root.has("items") || !root.get("items").isJsonArray())
            {
                Simplematica.LOGGER.error("MaterialListCustom#fromJsonFile: JSON file '{}' missing 'items' array", file);
                return null;
            }

            JsonArray itemsArray = root.getAsJsonArray("items");
            Object2IntOpenHashMap<ItemType> items = new Object2IntOpenHashMap<>();

            for (JsonElement itemElement : itemsArray)
            {
                if (!itemElement.isJsonObject())
                {
                    Simplematica.LOGGER.warn("MaterialListCustom#fromJsonFile: Skipping invalid item entry in '{}'", file);
                    continue;
                }

                JsonObject itemObj = itemElement.getAsJsonObject();

                if (!itemObj.has("id") || !itemObj.has("count"))
                {
                    Simplematica.LOGGER.warn("MaterialListCustom#fromJsonFile: Skipping item entry missing 'id' or 'count' in '{}'", file);
                    continue;
                }

                String itemId = itemObj.get("id").getAsString();
                int count = itemObj.get("count").getAsInt();

                if (count <= 0)
                {
                    Simplematica.LOGGER.warn("MaterialListCustom#fromJsonFile: Skipping item '{}' with invalid count {} in '{}'", itemId, count, file);
                    continue;
                }

                Identifier identifier = Identifier.tryParse(itemId);
                if (identifier == null)
                {
                    Simplematica.LOGGER.warn("MaterialListCustom#fromJsonFile: Invalid item ID '{}' in '{}'", itemId, file);
                    continue;
                }

                Item item = BuiltInRegistries.ITEM.getValue(identifier);

                if (item == null)
                {
                    Simplematica.LOGGER.warn("MaterialListCustom#fromJsonFile: Unknown item '{}' in '{}'", itemId, file);
                    continue;
                }

                ItemStack stack = new ItemStack(item);
                ItemType type = new ItemType(stack, false, false);
                items.addTo(type, count);
            }

            if (items.isEmpty())
            {
                Simplematica.LOGGER.error("MaterialListCustom#fromJsonFile: No valid items found in '{}'", file);
                return null;
            }

            Simplematica.LOGGER.info("MaterialListCustom#fromJsonFile: Loaded {} item types from '{}'", items.size(), file);
            return new MaterialListCustom(name, items, file);
        }
        catch (IOException e)
        {
            Simplematica.LOGGER.error("MaterialListCustom#fromJsonFile: Failed to read JSON file '{}': {}", file, e.getMessage());
            return null;
        }
        catch (Exception e)
        {
            Simplematica.LOGGER.error("MaterialListCustom#fromJsonFile: Failed to parse JSON file '{}': {}", file, e.getMessage());
            return null;
        }
    }

    /**
     * Loads a custom material list from a text file
     * Format (lines starting with # are comments):
     * # My shopping list
     * minecraft:diamond_pickaxe 5
     * minecraft:cooked_beef 64
     * minecraft:ender_pearl 16
     *
     * @param file Path to the text file
     * @return MaterialListCustom instance or null if loading failed
     */
    @Nullable
    public static MaterialListCustom fromTextFile(Path file)
    {
        try (BufferedReader reader = Files.newBufferedReader(file))
        {
            Object2IntOpenHashMap<ItemType> items = new Object2IntOpenHashMap<>();
            String name = file.getFileName().toString();
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null)
            {
                lineNumber++;
                line = line.trim();

                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#"))
                {
                    continue;
                }

                // Parse line: "item_id count"
                String[] parts = line.split("\\s+");
                if (parts.length != 2)
                {
                    Simplematica.LOGGER.warn("MaterialListCustom#fromTextFile: Invalid line {} in '{}': expected 'item_id count'", lineNumber, file);
                    continue;
                }

                String itemId = parts[0];
                int count;

                try
                {
                    count = Integer.parseInt(parts[1]);
                }
                catch (NumberFormatException e)
                {
                    Simplematica.LOGGER.warn("MaterialListCustom#fromTextFile: Invalid count '{}' on line {} in '{}'", parts[1], lineNumber, file);
                    continue;
                }

                if (count <= 0)
                {
                    Simplematica.LOGGER.warn("MaterialListCustom#fromTextFile: Invalid count {} on line {} in '{}'", count, lineNumber, file);
                    continue;
                }

                Identifier identifier = Identifier.tryParse(itemId);
                if (identifier == null)
                {
                    Simplematica.LOGGER.warn("MaterialListCustom#fromTextFile: Invalid item ID '{}' on line {} in '{}'", itemId, lineNumber, file);
                    continue;
                }

                Item item = BuiltInRegistries.ITEM.getValue(identifier);

                if (item == null)
                {
                    Simplematica.LOGGER.warn("MaterialListCustom#fromTextFile: Unknown item '{}' on line {} in '{}'", itemId, lineNumber, file);
                    continue;
                }

                ItemStack stack = new ItemStack(item);
                ItemType type = new ItemType(stack, false, false);
                items.addTo(type, count);
            }

            if (items.isEmpty())
            {
                Simplematica.LOGGER.error("MaterialListCustom#fromTextFile: No valid items found in '{}'", file);
                return null;
            }

            Simplematica.debugLog("MaterialListCustom#fromTextFile: Loaded {} item types from '{}'", items.size(), file);
            return new MaterialListCustom(name, items, file);
        }
        catch (IOException e)
        {
            Simplematica.LOGGER.error("MaterialListCustom#fromTextFile: Failed to read text file '{}': {}", file, e.getMessage());
            return null;
        }
    }

    /**
     * Auto-detects file format and loads the material list
     * @param file Path to the file (.json or .txt)
     * @return MaterialListCustom instance or null if loading failed
     */
    @Nullable
    public static MaterialListCustom fromFile(Path file)
    {
        String fileName = file.getFileName().toString().toLowerCase();

        if (fileName.endsWith(JSON_FILE_EXTENSION))
        {
            return fromJsonFile(file);
        }
        else if (fileName.endsWith(TEXT_FILE_EXTENSION))
        {
            return fromTextFile(file);
        }
        else
        {
            Simplematica.LOGGER.error("MaterialListCustom#fromFile: Unsupported file format '{}' - expected .json or .txt", file);
            return null;
        }
    }

    /**
     * Exports this material list to a JSON file
     * @param file Path where the JSON file should be saved
     * @return true if export was successful
     */
    public boolean toJsonFile(Path file, boolean overwrite)
    {
        boolean exists = Files.exists(file);

        if (exists && overwrite)
        {
            try
            {
                Files.delete(file);
            }
            catch (IOException e)
            {
                Simplematica.LOGGER.error("MaterialListCustom#toJsonFile: Failed to delete file '{}'; {}", file.getFileName().toString(), e.getLocalizedMessage());
                return false;
            }
        }
        else if (exists)
        {
            Simplematica.LOGGER.error("MaterialListCustom#toJsonFile: Failed; file '{}' already exists", file.getFileName().toString());
            return false;
        }

        try
        {
            JsonObject root = new JsonObject();
            root.add("name", new JsonPrimitive(this.name));

            JsonArray itemsArray = new JsonArray();

            for (MaterialListEntry entry : this.materialListAll)
            {
                JsonObject itemObj = new JsonObject();
                ItemStack stack = entry.getStack();
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

                itemObj.add("id", new JsonPrimitive(itemId));
                itemObj.add("count", new JsonPrimitive(entry.getCountTotal()));

                itemsArray.add(itemObj);
            }

            root.add("items", itemsArray);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(root);

            Files.writeString(file, json);
            Simplematica.LOGGER.info("MaterialListCustom#toJsonFile: Exported material list to '{}'", file);
            return true;
        }
        catch (IOException e)
        {
            Simplematica.LOGGER.error("MaterialListCustom#toJsonFile: Failed to write JSON file '{}': {}", file, e.getMessage());
            return false;
        }
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getTitle()
    {
        if (this.sourceFile != null)
        {
            return StringUtils.translate("simplematica.gui.title.material_list.custom_file", this.name, this.sourceFile.getFileName().toString());
        }
        return StringUtils.translate("simplematica.gui.title.material_list.custom", this.name);
    }

    @Override
    public void reCreateMaterialList()
    {
        if (this.sourceFile != null && Files.exists(this.sourceFile))
        {
            MaterialListCustom reloaded = fromFile(this.sourceFile);
            if (reloaded != null)
            {
                this.materialListAll = reloaded.materialListAll;
                this.refreshPreFilteredList();
                this.updateCounts();
                Simplematica.debugLog("MaterialListCustom#reCreateMaterialList: Reloaded material list from '{}'", this.sourceFile);
            }
        }
        else
        {
            Simplematica.LOGGER.warn("MaterialListCustom#reCreateMaterialList: Cannot recreate material list - no source file");
        }
    }
}
