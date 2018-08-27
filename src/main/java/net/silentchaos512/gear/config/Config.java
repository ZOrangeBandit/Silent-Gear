package net.silentchaos512.gear.config;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.item.ICoreItem;
import net.silentchaos512.gear.init.ModItems;
import net.silentchaos512.lib.collection.EntityMatchList;
import net.silentchaos512.lib.collection.ItemMatchList;
import net.silentchaos512.lib.config.ConfigBase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Config extends ConfigBase {

    public static final Config INSTANCE = new Config();

    private static final String CAT_ITEMS = "items";
    private static final String CAT_NERFED_GEAR = CAT_ITEMS + SEP + "nerfed_gear";
    static final String CAT_TOOLS = CAT_ITEMS + SEP + "tools";

    /*
     * Items
     */
    public static EntityMatchList sinewAnimals = new EntityMatchList(true, false,
            "minecraft:cow", "minecraft:sheep", "minecraft:pig");
    private static final String SINEW_ANIMALS_COMMENT = "These entities can drop sinew. It is not restricted to animals.";

    public static ItemMatchList blockPlacerTools = new ItemMatchList(true, false,
            SilentGear.RESOURCE_PREFIX + "pickaxe", SilentGear.RESOURCE_PREFIX + "shovel", SilentGear.RESOURCE_PREFIX + "axe");
    private static final String BLOCK_PLACER_TOOLS_COMMENT = "These items will be able to place blocks by using them (right-click-to-place)";

    public static ItemMatchList itemsThatToolsCanUse = new ItemMatchList(true, false,
            "danknull:dank_null", "xreliquary:sojourner_staff", "torchbandolier:torch_bandolier");
    private static final String ITEMS_THAT_TOOLS_CAN_USE_COMMENT = "Items that block-placing tools can \"use\" by simulating a right-click.";

    public static float sinewDropRate;
    private static final float SINEW_DROP_RATE_DEFAULT = 0.2f;
    private static final String SINEW_DROP_RATE_COMMENT = "The probability an animal will drop sinew.";

    /*
     * Nerfed gear
     */

    public static Set<String> nerfedGear;
    private static final String NERFED_GEAR_COMMENT = "These items will have reduced durability to discourage use, but they can still be crafted and used as normal. Items from other mods can be added to the list, but I cannot guarantee their durability will actually change.";

    public static float nerfedGearMulti;
    private static final float NERFED_GEAR_MULTI_DEFAULT = 0.25f;
    private static final String NERFED_GEAR_MULTI_COMMENT = "The durability of items in the nerfed gear list will be multiplied by this value.";

    /*
     * Tools
     */

    private static List<ConfigOptionEquipment> equipmentConfigs = new ArrayList<>();
    public static ConfigOptionEquipment sword = forEquipment(ModItems.sword);
    public static ConfigOptionEquipment dagger = forEquipment(ModItems.dagger);
    public static ConfigOptionEquipment katana = forEquipment(ModItems.katana);
    public static ConfigOptionEquipment machete = forEquipment(ModItems.machete);
    public static ConfigOptionEquipment pickaxe = forEquipment(ModItems.pickaxe);
    public static ConfigOptionEquipment shovel = forEquipment(ModItems.shovel);
    public static ConfigOptionEquipment axe = forEquipment(ModItems.axe);
    public static ConfigOptionEquipment hammer = forEquipment(ModItems.hammer);
    public static ConfigOptionEquipment excavator = forEquipment(ModItems.excavator);
    public static ConfigOptionEquipment mattock = forEquipment(ModItems.mattock);
    public static ConfigOptionEquipment sickle = forEquipment(ModItems.sickle);
    public static ConfigOptionEquipment bow = forEquipment(ModItems.bow);
    public static ConfigOptionEquipment helmet = forEquipment(ModItems.helmet);
    public static ConfigOptionEquipment chestplate = forEquipment(ModItems.chestplate);
    public static ConfigOptionEquipment leggings = forEquipment(ModItems.leggings);
    public static ConfigOptionEquipment boots = forEquipment(ModItems.boots);

    public static boolean toolsBreakPermanently;
    private static final boolean TOOLS_BREAK_DEFAULT = false;
    private static final String TOOLS_BREAK_COMMENT = "If enabled, tools/weapons/armor are destroyed when broken, just like vanilla.";

    File directory;

    public Config() {
        super(SilentGear.MOD_ID);
    }

    public void onPreInit(FMLPreInitializationEvent event) {
        this.directory = new File(event.getModConfigurationDirectory().getPath(), "silentchaos512/" + SilentGear.MOD_ID + "/");
        config = new Configuration(new File(directory.getPath(), SilentGear.MOD_ID + ".cfg"));
        new File(directory.getPath(), "materials/").mkdirs();
        new File(directory.getPath(), "equipment/").mkdirs();
    }

    @Override
    public void init(File file) {
        load();
    }

    @Override
    public void load() {
        try {
            /*
             * Items
             */

            // Sinew
            String catSinew = CAT_ITEMS + SEP + "sinew";
            sinewAnimals.loadConfig(config, "Animals That Drop Sinew", catSinew, SINEW_ANIMALS_COMMENT);
            sinewDropRate = loadFloat("Drop Rate", catSinew, SINEW_DROP_RATE_DEFAULT, SINEW_DROP_RATE_COMMENT);

            // Block placer tools
            blockPlacerTools.loadConfig(config, "Items That Place Blocks", CAT_ITEMS, BLOCK_PLACER_TOOLS_COMMENT);
            itemsThatToolsCanUse.loadConfig(config, "Items That Block Placer Tools Can Use", CAT_ITEMS, ITEMS_THAT_TOOLS_CAN_USE_COMMENT);

            /*
             * Nerfed gear
             */

            config.setCategoryComment(CAT_NERFED_GEAR, "Settings for nerfing gear from vanilla or other mods.");
            config.setCategoryRequiresMcRestart(CAT_NERFED_GEAR, true);

            String[] nerfedItems = config.getStringList("Nerfed Gear List", CAT_NERFED_GEAR, getDefaultNerfedGear(), NERFED_GEAR_COMMENT);
            nerfedGear = ImmutableSet.copyOf(nerfedItems);
            nerfedGearMulti = loadFloat("Durability Multiplier", CAT_NERFED_GEAR, NERFED_GEAR_MULTI_DEFAULT, 0, 1, NERFED_GEAR_MULTI_COMMENT);

            /*
             * Tools
             */

            for (ConfigOptionEquipment option : equipmentConfigs)
                option.loadValue(config);

            toolsBreakPermanently = loadBoolean("Equipment Breaks Permanently", CAT_ITEMS, TOOLS_BREAK_DEFAULT, TOOLS_BREAK_COMMENT);
        } catch (Exception ex) {
            SilentGear.log.fatal("Could not load configuration file! This could end badly...");
            SilentGear.log.catching(ex);
        }
    }

    private static ConfigOptionEquipment forEquipment(ICoreItem item) {
        ConfigOptionEquipment option = new ConfigOptionEquipment(item);
        equipmentConfigs.add(option);
        return option;
    }

    private static String[] getDefaultNerfedGear() {
        Set<String> toolTypes = ImmutableSet.of("pickaxe", "shovel", "axe", "sword");
        Set<String> toolMaterials = ImmutableSet.of("wooden", "stone", "iron", "golden", "diamond");
        List<String> items = toolTypes.stream()
                .flatMap(type -> toolMaterials.stream()
                        .map(material -> "minecraft:" + material + "_" + type))
                .collect(Collectors.toList());

        Set<String> armorTypes = ImmutableSet.of("helmet", "chestplate", "leggings", "boots");
        Set<String> armorMaterials = ImmutableSet.of("leather", "chainmail", "iron", "diamond", "golden");
        items.addAll(armorTypes.stream()
                .flatMap(type -> armorMaterials.stream()
                        .map(material -> "minecraft:" + material + "_" + type))
                .collect(Collectors.toList()));

        return items.toArray(new String[0]);
    }

    public File getDirectory() {
        return directory;
    }
}
