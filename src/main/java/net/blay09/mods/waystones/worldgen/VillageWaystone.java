package net.blay09.mods.waystones.worldgen;

import java.util.List;
import java.util.Random;

import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.block.TileWaystone;
import net.blay09.mods.waystones.compat.VillageNamesCompat;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.VillagerRegistry;

// Test seed: 6344652377971967156
// Test coords (village names): 159 65 809
// Test coords (vanilla): 81 79 1539
// vanilla desert village: 1660196624
// vanilla jungle temple: -5136872554856456624, 16 100 -480
// stronghold: seed: lolol, coords: /tp @p -2108 ~ -284
// nether fortress: seed: lolol, coords: /tp 331 ~ -693
public class VillageWaystone {

    private static final String STRUCTURE_ID = "village";
    public static final String PIECE_ID = Waystones.MODID + ":VillageWaystone";

    // Untagged waystones from villages generated before tagging existed: look for our piece in the saved village data
    public static boolean isGeneratedAt(World world, int x, int z) {
        MapGenStructureData data = (MapGenStructureData) world.perWorldStorage
            .loadData(MapGenStructureData.class, "Village");
        if (data == null) {
            return false;
        }
        NBTTagCompound starts = data.func_143041_a();
        for (Object key : starts.func_150296_c()) {
            NBTTagCompound start = starts.getCompoundTag((String) key);
            if (!containsColumn(start.getIntArray("BB"), x, z)) {
                continue;
            }
            NBTTagList children = start.getTagList("Children", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < children.tagCount(); i++) {
                NBTTagCompound child = children.getCompoundTagAt(i);
                if (PIECE_ID.equals(child.getString("id")) && containsColumn(child.getIntArray("BB"), x, z)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Ignores Y, saved bounds can predate the piece being moved onto the ground
    private static boolean containsColumn(int[] bb, int x, int z) {
        return bb.length == 6 && x >= bb[0] && x <= bb[3] && z >= bb[2] && z <= bb[5];
    }

    public static class VillageWaystonePiece extends StructureVillagePieces.Village {

        public VillageWaystonePiece() {}

        public VillageWaystonePiece(StructureVillagePieces.Start start, int type, Random rand, StructureBoundingBox box,
            int coordBaseMode) {
            super(start, type);
            this.coordBaseMode = coordBaseMode;
            this.boundingBox = box;
        }

        public static VillageWaystonePiece buildComponent(StructureVillagePieces.Start start,
            List<StructureComponent> pieces, Random rand, int x, int y, int z, int coordBaseMode, int type) {
            StructureBoundingBox box = StructureBoundingBox
                .getComponentToAddBoundingBox(x, y, z, 0, 0, 0, 5, 6, 5, coordBaseMode);

            if (!canVillageGoDeeper(box)) return null;
            if (StructureComponent.findIntersecting(pieces, box) != null) return null;

            return new VillageWaystonePiece(start, type, rand, box, coordBaseMode);
        }

        @Override
        public boolean addComponentParts(World world, Random rand, StructureBoundingBox box) {
            if (this.field_143015_k < 0) {
                this.field_143015_k = this.getAverageGroundLevel(world, box);
                if (this.field_143015_k < 0) return true;

                // Lower the entire structure by 1 so platform is on the floor
                this.boundingBox.offset(0, this.field_143015_k - this.boundingBox.minY - 1, 0);
            }

            int xMin = this.boundingBox.minX;
            int yMin = this.boundingBox.minY;
            int zMin = this.boundingBox.minZ;

            // Clear area above platform
            this.fillWithAir(world, box, 0, 1, 0, 4, 5, 4);

            // Match vanilla village path replacement logic (for example sandstone in desert villages)
            Block platformBlock = this.func_151558_b(Blocks.gravel, 0);
            int platformMeta = this.func_151557_c(Blocks.gravel, 0);

            // Platform (1 block high)
            this.fillWithMetadataBlocks(
                world,
                box,
                0,
                0,
                0,
                4,
                0,
                4,
                platformBlock,
                platformMeta,
                platformBlock,
                platformMeta,
                false);

            // Waystone position (sit on top of platform)
            int waystoneX = xMin + 2;
            int waystoneY = yMin + 1;
            int waystoneZ = zMin + 2;
            int biomeId = world.getBiomeGenForCoords(waystoneX, waystoneZ).biomeID;

            int autoVariant = Waystones.varInstanceCommon.isMossyWaystonePathBlock(platformBlock)
                ? TileWaystone.VARIANT_MOSSY
                : (Waystones.varInstanceCommon.isSandyWaystonePathBlock(platformBlock) ? TileWaystone.VARIANT_SANDSTONE
                    : TileWaystone.VARIANT_STONE);
            int variant = Waystones.varInstanceCommon
                .resolveStructureWaystoneVariant(STRUCTURE_ID, autoVariant, world, biomeId, rand);
            if (variant < 0) {
                return true;
            }

            Block waystoneBlock = Waystones.getWaystoneBlock(variant);

            // Lower block
            world.setBlock(waystoneX, waystoneY, waystoneZ, waystoneBlock, 2, 2);
            // Upper block
            world.setBlock(waystoneX, waystoneY + 1, waystoneZ, waystoneBlock, ForgeDirection.UNKNOWN.ordinal(), 2);

            // Initialize TileEntity
            TileWaystone tile = (TileWaystone) world.getTileEntity(waystoneX, waystoneY, waystoneZ);

            if (tile != null && !world.isRemote) {
                tile.setVillageWaystone(true);
                String autoName = null;

                if (Waystones.getConfig().villageNamesCompat && Loader.isModLoaded("VillageNames")) {
                    autoName = VillageNamesCompat.ensureVillageName(world, waystoneX, waystoneY, waystoneZ);
                }

                String resolvedName = Waystones.varInstanceCommon.resolveStructureWaystoneName(STRUCTURE_ID, autoName);
                if (resolvedName != null && !resolvedName.isEmpty()) {
                    tile.setWaystoneName(resolvedName);
                }

                if (Waystones.varInstanceCommon.shouldForceGlobalStructureWaystone(STRUCTURE_ID)
                    && tile.getWaystoneName() != null
                    && !tile.getWaystoneName()
                        .isEmpty()) {
                    if (Waystones.varInstanceCommon.shouldAutoActivateGlobalStructureWaystone(STRUCTURE_ID)) {
                        WaystoneManager.addServerWaystone(new WaystoneEntry(tile));
                    } else {
                        tile.setForceGlobalOnActivation(true);
                    }
                }
            }

            return true;
        }

    }

    public static class CreationHandler implements VillagerRegistry.IVillageCreationHandler {

        // @Override
        public StructureVillagePieces.PieceWeight getVillagePieceWeight_old(Random random, int size) {
            return new StructureVillagePieces.PieceWeight(VillageWaystonePiece.class, 1, 1);
        }

        @Override
        public StructureVillagePieces.PieceWeight getVillagePieceWeight(Random random, int size) {
            return new StructureVillagePieces.PieceWeight(VillageWaystonePiece.class, 1, 1);
        }

        @Override
        public Class<?> getComponentClass() {
            return VillageWaystonePiece.class;
        }

        @Override
        public StructureComponent buildComponent(StructureVillagePieces.PieceWeight villagePiece,
            StructureVillagePieces.Start startPiece, List pieces, Random random, int p1, int p2, int p3,
            int coordBaseMode, int type) {
            return VillageWaystonePiece.buildComponent(startPiece, pieces, random, p1, p2, p3, coordBaseMode, type);
        }
    }
}
