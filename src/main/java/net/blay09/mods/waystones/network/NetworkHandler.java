package net.blay09.mods.waystones.network;

import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.network.handler.HandlerAtlasMarker;
import net.blay09.mods.waystones.network.handler.HandlerConfig;
import net.blay09.mods.waystones.network.handler.HandlerDimensionNames;
import net.blay09.mods.waystones.network.handler.HandlerFreeWarpReturn;
import net.blay09.mods.waystones.network.handler.HandlerJourneyMapWaypoint;
import net.blay09.mods.waystones.network.handler.HandlerPinWaystone;
import net.blay09.mods.waystones.network.handler.HandlerRenameWaystone;
import net.blay09.mods.waystones.network.handler.HandlerRequestWaystones;
import net.blay09.mods.waystones.network.handler.HandlerTeleportEffect;
import net.blay09.mods.waystones.network.handler.HandlerUnlearnWaystone;
import net.blay09.mods.waystones.network.handler.HandlerWarpStone;
import net.blay09.mods.waystones.network.handler.HandlerWaystoneActivationEffect;
import net.blay09.mods.waystones.network.handler.HandlerWaystoneName;
import net.blay09.mods.waystones.network.handler.HandlerWaystoneRenamed;
import net.blay09.mods.waystones.network.handler.HandlerWaystones;
import net.blay09.mods.waystones.network.message.MessageAtlasMarker;
import net.blay09.mods.waystones.network.message.MessageConfig;
import net.blay09.mods.waystones.network.message.MessageDimensionNames;
import net.blay09.mods.waystones.network.message.MessageJourneyMapWaypoint;
import net.blay09.mods.waystones.network.message.MessagePinWaystone;
import net.blay09.mods.waystones.network.message.MessageRenameWaystone;
import net.blay09.mods.waystones.network.message.MessageRequestWaystones;
import net.blay09.mods.waystones.network.message.MessageTeleportEffect;
import net.blay09.mods.waystones.network.message.MessageUnlearnWaystone;
import net.blay09.mods.waystones.network.message.MessageWarpReturn;
import net.blay09.mods.waystones.network.message.MessageWarpStone;
import net.blay09.mods.waystones.network.message.MessageWaystoneActivationEffect;
import net.blay09.mods.waystones.network.message.MessageWaystoneName;
import net.blay09.mods.waystones.network.message.MessageWaystoneRenamed;
import net.blay09.mods.waystones.network.message.MessageWaystones;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class NetworkHandler {

    private static int discriminator = 0;
    public static final SimpleNetworkWrapper channel = NetworkRegistry.INSTANCE.newSimpleChannel(Waystones.MODID);

    public static void init() {
        channel.registerMessage(HandlerConfig.class, MessageConfig.class, discriminator++, Side.CLIENT);
        channel.registerMessage(HandlerWaystones.class, MessageWaystones.class, discriminator++, Side.CLIENT);
        channel.registerMessage(
            HandlerJourneyMapWaypoint.class,
            MessageJourneyMapWaypoint.class,
            discriminator++,
            Side.CLIENT);
        channel.registerMessage(HandlerFreeWarpReturn.class, MessageWarpReturn.class, discriminator++, Side.SERVER);
        channel.registerMessage(HandlerWaystoneName.class, MessageWaystoneName.class, discriminator++, Side.SERVER);
        channel.registerMessage(HandlerWarpStone.class, MessageWarpStone.class, discriminator++, Side.SERVER);
        channel.registerMessage(HandlerTeleportEffect.class, MessageTeleportEffect.class, discriminator++, Side.CLIENT);
        channel
            .registerMessage(HandlerUnlearnWaystone.class, MessageUnlearnWaystone.class, discriminator++, Side.SERVER);
        channel.registerMessage(HandlerPinWaystone.class, MessagePinWaystone.class, discriminator++, Side.SERVER);
        channel.registerMessage(HandlerDimensionNames.class, MessageDimensionNames.class, discriminator++, Side.CLIENT);
        channel.registerMessage(HandlerRenameWaystone.class, MessageRenameWaystone.class, discriminator++, Side.SERVER);
        if (FMLCommonHandler.instance()
            .getSide() == Side.CLIENT) {
            channel.registerMessage(
                HandlerWaystoneRenamed.class,
                MessageWaystoneRenamed.class,
                discriminator++,
                Side.CLIENT);
        } else {
            // Preventing id mismatch. IIRC just incrementing won't work.
            // HandlerWaystoneRenamed has some client classes
            channel.registerMessage((message, ctx) -> null, MessageWaystoneRenamed.class, discriminator++, Side.CLIENT);
        }
        channel.registerMessage(
            HandlerWaystoneActivationEffect.class,
            MessageWaystoneActivationEffect.class,
            discriminator++,
            Side.CLIENT);
        channel.registerMessage(HandlerAtlasMarker.class, MessageAtlasMarker.class, discriminator++, Side.SERVER);
        channel.registerMessage(
            HandlerRequestWaystones.class,
            MessageRequestWaystones.class,
            discriminator++,
            Side.SERVER);
    }
}
