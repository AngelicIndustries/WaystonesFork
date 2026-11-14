package net.blay09.mods.waystones;

import net.blay09.mods.waystones.block.BlockWaystone;
import net.blay09.mods.waystones.block.ItemBlockWaystone;
import net.blay09.mods.waystones.block.TileWaystone;
import net.blay09.mods.waystones.compat.AntiqueAtlasMarkers;
import net.blay09.mods.waystones.item.ItemReturnScroll;
import net.blay09.mods.waystones.item.ItemWarpStone;
import net.blay09.mods.waystones.network.NetworkHandler;
import net.blay09.mods.waystones.varinstances.VarInstanceClient;
import net.blay09.mods.waystones.varinstances.VarInstanceCommon;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.oredict.ShapedOreRecipe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(
    modid = Waystones.MODID,
    name = "Waystones-X",
    dependencies = "required-after:unimixins@[0.3.1,)",
    guiFactory = "net.blay09.mods.waystones.client.gui.GuiFactory",
    customProperties = { @Mod.CustomProperty(k = "license", v = "LGPLv3"),
        @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/AngelicIndustries/WaystonesFork/issues"),
        @Mod.CustomProperty(k = "iconFile", v = "assets/waystones/logo_round.png"),
        @Mod.CustomProperty(k = "backgroundFile", v = "assets/waystones/background.png") })
@SuppressWarnings("unused")
public class Waystones {

    public static final String MODID = "waystones";

    @Mod.Instance(MODID)
    public static Waystones instance;

    @SidedProxy(
        serverSide = "net.blay09.mods.waystones.CommonProxy",
        clientSide = "net.blay09.mods.waystones.client.ClientProxy")
    public static CommonProxy proxy;

    public static BlockWaystone blockWaystone;
    public static BlockWaystone blockWaystoneSandstone;
    public static BlockWaystone blockWaystoneMossy;
    public static BlockWaystone blockWaystoneStonebrick;
    public static BlockWaystone blockWaystoneMossyStonebrick;
    public static BlockWaystone blockWaystoneNether;
    public static BlockWaystone blockWaystoneEnd;
    public static ItemReturnScroll itemReturnScroll;
    public static ItemWarpStone itemWarpStone;

    public static Configuration configuration;

    private WaystoneConfig config;

    public static final Logger LOG = LogManager.getLogger(MODID);

    public static VarInstanceClient varInstanceClient = new VarInstanceClient();
    public static VarInstanceCommon varInstanceCommon = new VarInstanceCommon();

    public static boolean hasLwjgl3 = Loader.isModLoaded("lwjgl3ify");
    public static boolean hasAntiqueAtlas = Loader.isModLoaded("antiqueatlas");
    public static boolean hasBiomic = Loader.isModLoaded("biomic");
    public static boolean hasBackhand = Loader.isModLoaded("backhand");

    public static boolean DEBUG_MODE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        String debugVar = System.getenv("MCMODDING_DEBUG_MODE");
        DEBUG_MODE = debugVar != null;
        LOG.info("Debugmode: {}", DEBUG_MODE);

        configuration = new Configuration(event.getSuggestedConfigurationFile());
        config = new WaystoneConfig();
        config.reloadLocal(configuration);
        setConfig(config);
        varInstanceCommon.preInitHook();
        WaystoneConfig.setConfig(configuration);
        if (configuration.hasChanged()) {
            configuration.save();
        }

        blockWaystone = new BlockWaystone();
        GameRegistry.registerBlock(blockWaystone, ItemBlockWaystone.class, "waystone");
        blockWaystoneSandstone = new BlockWaystone(TileWaystone.VARIANT_SANDSTONE, "waystone_sandstone");
        GameRegistry.registerBlock(blockWaystoneSandstone, ItemBlockWaystone.class, "waystone_sandstone");
        blockWaystoneMossy = new BlockWaystone(TileWaystone.VARIANT_MOSSY, "waystone_mossy");
        GameRegistry.registerBlock(blockWaystoneMossy, ItemBlockWaystone.class, "waystone_mossy");
        blockWaystoneStonebrick = new BlockWaystone(TileWaystone.VARIANT_STONEBRICK, "waystone_stonebrick");
        GameRegistry.registerBlock(blockWaystoneStonebrick, ItemBlockWaystone.class, "waystone_stonebrick");
        blockWaystoneMossyStonebrick = new BlockWaystone(
            TileWaystone.VARIANT_MOSSY_STONEBRICK,
            "waystone_mossy_stonebrick");
        GameRegistry.registerBlock(blockWaystoneMossyStonebrick, ItemBlockWaystone.class, "waystone_mossy_stonebrick");
        blockWaystoneNether = new BlockWaystone(TileWaystone.VARIANT_NETHER, "waystone_nether");
        GameRegistry.registerBlock(blockWaystoneNether, ItemBlockWaystone.class, "waystone_nether");
        blockWaystoneEnd = new BlockWaystone(TileWaystone.VARIANT_END, "waystone_end");
        GameRegistry.registerBlock(blockWaystoneEnd, ItemBlockWaystone.class, "waystone_end");
        GameRegistry.registerTileEntity(TileWaystone.class, MODID + ":waystone");

        itemReturnScroll = new ItemReturnScroll();
        GameRegistry.registerItem(itemReturnScroll, "warpScroll");

        itemWarpStone = new ItemWarpStone();
        GameRegistry.registerItem(itemWarpStone, "warpStone");

        NetworkHandler.init();

        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLInterModComms.sendMessage("Waila", "register", "net.blay09.mods.waystones.compat.WailaProvider.register");

        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (instance.config.allowReturnScrolls) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(itemReturnScroll, 3),
                    "GEG",
                    "PPP",
                    'G',
                    "nuggetGold",
                    'E',
                    Items.ender_pearl,
                    'P',
                    Items.paper));
        }

        if (instance.config.allowWarpStone) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(itemWarpStone),
                    "DED",
                    "EGE",
                    "DED",
                    'D',
                    "dyePurple",
                    'E',
                    Items.ender_pearl,
                    'G',
                    "gemEmerald"));
        }

        if (!config.creativeModeOnly) {
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(blockWaystone),
                    " S ",
                    "SWS",
                    "OOO",
                    'S',
                    Blocks.stone,
                    'W',
                    itemWarpStone,
                    'O',
                    Blocks.obsidian));
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(blockWaystoneSandstone),
                    " S ",
                    "SWS",
                    "OOO",
                    'S',
                    Blocks.sandstone,
                    'W',
                    itemWarpStone,
                    'O',
                    Blocks.obsidian));
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(blockWaystoneMossy),
                    " S ",
                    "SWS",
                    "OOO",
                    'S',
                    Blocks.mossy_cobblestone,
                    'W',
                    itemWarpStone,
                    'O',
                    Blocks.obsidian));
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(blockWaystoneStonebrick),
                    " S ",
                    "SWS",
                    "OOO",
                    'S',
                    new ItemStack(Blocks.stonebrick, 1, 0),
                    'W',
                    itemWarpStone,
                    'O',
                    Blocks.obsidian));
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(blockWaystoneMossyStonebrick),
                    " S ",
                    "SWS",
                    "OOO",
                    'S',
                    new ItemStack(Blocks.stonebrick, 1, 1),
                    'W',
                    itemWarpStone,
                    'O',
                    Blocks.obsidian));
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(blockWaystoneNether),
                    " S ",
                    "SWS",
                    "OOO",
                    'S',
                    Blocks.nether_brick,
                    'W',
                    itemWarpStone,
                    'O',
                    Blocks.obsidian));
            GameRegistry.addRecipe(
                new ShapedOreRecipe(
                    new ItemStack(blockWaystoneEnd),
                    " S ",
                    "SWS",
                    "OOO",
                    'S',
                    Blocks.end_stone,
                    'W',
                    itemWarpStone,
                    'O',
                    Blocks.obsidian));
        }

        if (config.lootReturnScrolls) {
            // Item, min, max, weight
            ChestGenHooks.getInfo(ChestGenHooks.VILLAGE_BLACKSMITH)
                .addItem(new WeightedRandomChestContent(new ItemStack(itemReturnScroll), 1, 1, 5));
            ChestGenHooks.getInfo(ChestGenHooks.MINESHAFT_CORRIDOR)
                .addItem(new WeightedRandomChestContent(new ItemStack(itemReturnScroll), 1, 1, 3));
            ChestGenHooks.getInfo(ChestGenHooks.DUNGEON_CHEST)
                .addItem(new WeightedRandomChestContent(new ItemStack(itemReturnScroll), 1, 2, 3));
            ChestGenHooks.getInfo(ChestGenHooks.PYRAMID_DESERT_CHEST)
                .addItem(new WeightedRandomChestContent(new ItemStack(itemReturnScroll), 1, 1, 3));
            ChestGenHooks.getInfo(ChestGenHooks.PYRAMID_JUNGLE_CHEST)
                .addItem(new WeightedRandomChestContent(new ItemStack(itemReturnScroll), 1, 1, 2));
            ChestGenHooks.getInfo(ChestGenHooks.STRONGHOLD_LIBRARY)
                .addItem(new WeightedRandomChestContent(new ItemStack(itemReturnScroll), 1, 1, 2));
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        config.reloadLocal(configuration);
        varInstanceCommon.rebuildCaches();
        if (WaystoneConfig.debugMode) {
            proxy.serverStarting(event);
        }
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        if (hasAntiqueAtlas) {
            AntiqueAtlasMarkers.syncGlobalMarkers();
        }
    }

    public static WaystoneConfig getConfig() {
        return instance.config;
    }

    public void setConfig(WaystoneConfig config) {
        this.config = config;
    }

    public static BlockWaystone getWaystoneBlock(int variant) {
        switch (variant) {
            case TileWaystone.VARIANT_SANDSTONE:
                return blockWaystoneSandstone;
            case TileWaystone.VARIANT_MOSSY:
                return blockWaystoneMossy;
            case TileWaystone.VARIANT_STONEBRICK:
                return blockWaystoneStonebrick;
            case TileWaystone.VARIANT_MOSSY_STONEBRICK:
                return blockWaystoneMossyStonebrick;
            case TileWaystone.VARIANT_NETHER:
                return blockWaystoneNether;
            case TileWaystone.VARIANT_END:
                return blockWaystoneEnd;
            default:
                return blockWaystone;
        }
    }

    public static void debug(String message) {
        if (DEBUG_MODE || WaystoneConfig.debugMode) {
            LOG.info("DEBUG: " + message);
        }
    }
}
