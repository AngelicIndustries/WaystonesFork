package net.blay09.mods.waystones.block;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import net.blay09.mods.waystones.WaystoneConfig;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.client.gui.GuiWarpStone;
import net.blay09.mods.waystones.util.NameGenerator;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockWaystone extends BlockContainer {

    private final int defaultVariant;
    private final String iconName;

    @SideOnly(Side.CLIENT)
    private IIcon inventoryIcon;

    public BlockWaystone() {
        this(TileWaystone.VARIANT_STONE, "waystone");
    }

    public BlockWaystone(int defaultVariant, String registryName) {
        super(Material.rock);

        this.defaultVariant = defaultVariant;
        this.iconName = registryName + "_icon";
        setBlockName(Waystones.MODID + ":" + registryName);

        setHardness(5f);
        setResistance(Waystones.getConfig().invulnerableWaystones ? 6000000.0F : 2000f);

        setCreativeTab(CreativeTabs.tabDecorations);
    }

    public int getDefaultVariant() {
        return defaultVariant;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getInventoryIcon() {
        return inventoryIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
        inventoryIcon = register.registerIcon(Waystones.MODID + ":" + iconName);
    }

    private static boolean isWaystoneBlock(Block block) {
        return block instanceof BlockWaystone;
    }

    @Override
    public IIcon getIcon(int side, int metadata) {
        return Blocks.stone.getIcon(side, metadata);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return Waystones.proxy.getWaystoneRenderId();
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        if (metadata == ForgeDirection.UNKNOWN.ordinal()) {
            return null;
        }
        return new TileWaystone();
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        float maxLight = Waystones.getConfig().waystoneLightLevel;
        if (maxLight <= 0f) return 0;
        if (world instanceof World) {
            World w = (World) world;
            if (w.isRemote) {
                return Waystones.proxy.getWaystoneLightValue(maxLight);
            }
        }
        return (int) (maxLight * 15f);
    }

    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
        if (Waystones.getConfig().creativeModeOnly && !player.capabilities.isCreativeMode) {
            return -1f;
        }

        if (Waystones.getConfig().invulnerableWaystones) {
            TileWaystone tileWaystone = getTileWaystone(world, x, y, z);

            if (tileWaystone != null) {
                String waystoneOwner = tileWaystone.getWaystoneOwner();
                boolean hasOwner = !Objects.equals(waystoneOwner, "");

                if (hasOwner && !waystoneOwner.contentEquals(
                    player.getUniqueID()
                        .toString()))
                    return -1f;
            }
        }
        return super.getPlayerRelativeBlockHardness(player, world, x, y, z);
    }

    @Override
    public boolean canEntityDestroy(IBlockAccess world, int x, int y, int z, Entity entity) {
        if (Waystones.getConfig().invulnerableWaystones) {

            if (!(entity instanceof EntityPlayer player)) {
                return false;
            }

            if (player.capabilities.isCreativeMode) {
                return true;
            }

            TileWaystone waystone = getTileWaystone((World) world, x, y, z);
            if (waystone != null) {

                String ownerId = waystone.getWaystoneOwner();
                boolean hasOwner = !Objects.equals(ownerId, "");

                if (hasOwner) {
                    return Objects.equals(
                        player.getUniqueID()
                            .toString(),
                        waystone.getWaystoneOwner());
                }
            }
        }

        return true;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        Block blockBelow = world.getBlock(x, y - 1, z);
        if (isWaystoneBlock(blockBelow)) {
            return false;
        }
        Block blockAbove = world.getBlock(x, y + 2, z);
        return !isWaystoneBlock(blockAbove) && super.canPlaceBlockAt(world, x, y, z)
            && world.getBlock(x, y + 1, z)
                .isReplaceable(world, x, y + 1, z);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityLiving, ItemStack itemStack) {
        int orientation = BlockPistonBase.determineOrientation(world, x, y, z, entityLiving);
        world.setBlockMetadataWithNotify(x, y, z, orientation, 1 | 2);
        world.setBlock(x, y + 1, z, this, ForgeDirection.UNKNOWN.ordinal(), 1 | 2);
        TileWaystone tileWaystone = getTileWaystone(world, x, y, z);
        if (tileWaystone != null && !world.isRemote) {
            tileWaystone.setPlacedByPlayer(true);
        }
        if (tileWaystone != null && world.isRemote
            && entityLiving instanceof EntityPlayer
            && (!Waystones.getConfig().creativeModeOnly || ((EntityPlayer) entityLiving).capabilities.isCreativeMode)) {
            Waystones.proxy.openWaystoneNameEdit(tileWaystone);
        }
    }

    @Override
    public void dropBlockAsItemWithChance(World world, int x, int y, int z, int meta, float chance, int fortune) {
        if (Waystones.getConfig().disableWaystoneDrops) {
            return;
        }
        super.dropBlockAsItemWithChance(world, x, y, z, meta, chance, fortune);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int metadata) {
        TileWaystone tileWaystone = getTileWaystone(world, x, y, z);
        if (tileWaystone != null) {
            WaystoneManager.removeServerWaystone(new WaystoneEntry(tileWaystone));
        }
        super.breakBlock(world, x, y, z, block, metadata);
        if (isWaystoneBlock(world.getBlock(x, y + 1, z))) {
            world.setBlockToAir(x, y + 1, z);
        } else if (isWaystoneBlock(world.getBlock(x, y - 1, z))) {
            world.setBlockToAir(x, y - 1, z);
        }
    }

    // Unnamed world-generated waystones in auto mode get a name instead of asking for one
    private static boolean isNamedOnActivation(TileWaystone tileWaystone) {
        return Waystones.getConfig().unnamedWaystoneNaming == WaystoneConfig.UnnamedWaystoneNaming.AUTO
            && !tileWaystone.isPlacedByPlayer();
    }

    public static void clientActivationEffects(World world, int x, int y, int z) {
        Waystones.proxy.playSound("random.levelup", 1f);
        for (int i = 0; i < 32; i++) {
            world.spawnParticle(
                "enchantmenttable",
                x + 0.5 + (world.rand.nextDouble() - 0.5) * 2,
                y + 3,
                z + 0.5 + (world.rand.nextDouble() - 0.5) * 2,
                0,
                -5,
                0);
            world.spawnParticle(
                "enchantmenttable",
                x + 0.5 + (world.rand.nextDouble() - 0.5) * 2,
                y + 4,
                z + 0.5 + (world.rand.nextDouble() - 0.5) * 2,
                0,
                -5,
                0);
        }
    }

    public static void sendActivationChatMessage(EntityPlayer player, TileWaystone tileWaystone) {
        ChatComponentText nameComponent = new ChatComponentText(tileWaystone.getWaystoneName());
        nameComponent.getChatStyle()
            .setColor(EnumChatFormatting.WHITE);
        ChatComponentTranslation chatComponent = new ChatComponentTranslation(
            "waystones:activatedWaystone",
            nameComponent);
        chatComponent.getChatStyle()
            .setColor(EnumChatFormatting.YELLOW);
        player.addChatComponentMessage(chatComponent);
    }

    public static void setSpawnPoint(World world, EntityPlayer player, TileWaystone tileWaystone) {
        ForgeDirection facing = ForgeDirection
            .getOrientation(world.getBlockMetadata(tileWaystone.xCoord, tileWaystone.yCoord, tileWaystone.zCoord));
        player.setSpawnChunk(
            new ChunkCoordinates(
                tileWaystone.xCoord + facing.offsetX,
                tileWaystone.yCoord + facing.offsetY,
                tileWaystone.zCoord + facing.offsetZ),
            true);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        TileWaystone tileWaystone = getTileWaystone(world, x, y, z);
        if (tileWaystone == null) {
            return true;
        }
        boolean unnamed = tileWaystone.getWaystoneName()
            .isEmpty();
        if (world.isRemote && unnamed) {
            if (!isNamedOnActivation(tileWaystone)) {
                Waystones.proxy.openWaystoneNameEdit(tileWaystone);
            }
            return true;
        }
        if (world.isRemote) {
            if ((player.isSneaking() || WaystoneManager.playerActivatedWaystone(player, tileWaystone))
                && (player.capabilities.isCreativeMode || !Waystones.getConfig().creativeModeOnly)) {
                Waystones.proxy.openWaystoneSelection(tileWaystone, false, GuiWarpStone.TeleportSource.WAYSTONE);
            }
            return true;
        }
        if (!world.isRemote) {
            if (unnamed) {
                if (!isNamedOnActivation(tileWaystone)) {
                    return true;
                }
                tileWaystone.setWaystoneName(
                    NameGenerator.generate(
                        world,
                        tileWaystone.xCoord,
                        tileWaystone.yCoord,
                        tileWaystone.zCoord,
                        world.rand,
                        name -> WaystoneManager.isNameTaken(player, name)));
            }
            WaystoneManager.activateWaystone(player, tileWaystone);

            if (Waystones.getConfig().setSpawnPoint) {
                setSpawnPoint(world, player, tileWaystone);
            }
        } else {
            clientActivationEffects(world, x, y, z);
        }
        return true;
    }

    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (!WaystoneConfig.disableParticles && random.nextFloat() < 0.75f) {
            TileWaystone tileWaystone = getTileWaystone(world, x, y, z);
            if (tileWaystone == null) {
                return;
            }
            Waystones.proxy.spawnWaystoneParticles(world, x, y, z, tileWaystone, random);
        }
    }

    public static TileWaystone getTileWaystone(World world, int x, int y, int z) {
        TileWaystone tileWaystone = (TileWaystone) world.getTileEntity(x, y, z);
        if (tileWaystone == null) {
            TileEntity tileBelow = world.getTileEntity(x, y - 1, z);
            if (tileBelow instanceof TileWaystone) {
                return (TileWaystone) tileBelow;
            } else {
                return null;
            }
        }
        return tileWaystone;
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        if (isWaystoneBlock(world.getBlock(x, y - 1, z))) {
            return AxisAlignedBB.getBoundingBox(x, y - 1, z, x + 1, y + 1, z + 1);
        } else {
            return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 2, z + 1);
        }
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        if (isWaystoneBlock(world.getBlock(x, y - 1, z))) {
            setBlockBounds(0f, -1f, 0f, 1f, 1f, 1f);
        } else {
            setBlockBounds(0f, 0f, 0f, 1f, 2f, 1f);
        }
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list,
        Entity entity) {
        if (isWaystoneBlock(world.getBlock(x, y - 1, z))) {
            AxisAlignedBB boundingBox = AxisAlignedBB.getBoundingBox(x, y - 1, z, x + 1, y + 1, z + 1);
            if (boundingBox != null && mask.intersectsWith(boundingBox)) {
                list.add(boundingBox);
            }
        } else {
            AxisAlignedBB boundingBox = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 2, z + 1);
            if (boundingBox != null && mask.intersectsWith(boundingBox)) {
                list.add(boundingBox);
            }
        }
    }
}
