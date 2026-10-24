package net.blay09.mods.waystones.core;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    WAYSTONES(new MixinBuilder()
        .addCommonMixins(
            "minecraft.AccessorStructureComponent",
            "minecraftforge.AccessorDimensionManager",
            "minecraft.MixinStructureVillagePieces",
            "minecraft.MixinComponentScatteredFeaturePiecesDesertPyramid",
            "minecraft.MixinComponentScatteredFeaturePiecesJunglePyramid",
            "minecraft.MixinStructureStrongholdPiecesCrossing",
            "minecraft.MixinStructureNetherBridgePiecesPiece",
            "minecraft.MixinWorldGenSpikes")
        .addClientMixins(
            "minecraft.AccessorGuiContainer",
            "minecraft.AccessorGuiScreen",
            "minecraft.AccessorResourcePackRepository",
            "minecraft.AccessorResourcePackRepositoryEntry",
            "minecraft.MixinResourcePackRepository")
        .setPhase(Phase.EARLY));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
