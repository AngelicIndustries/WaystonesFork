package net.blay09.mods.waystones.network.handler;

import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.compat.AntiqueAtlasMarkers;
import net.blay09.mods.waystones.network.message.MessageAtlasMarker;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class HandlerAtlasMarker implements IMessageHandler<MessageAtlasMarker, IMessage> {

    @Override
    public IMessage onMessage(final MessageAtlasMarker message, final MessageContext ctx) {
        Waystones.proxy.addScheduledTask(() -> {
            if (!Waystones.hasAntiqueAtlas) {
                return;
            }
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            // Only waystones the player knows, and global ones while they aren't marked for everyone
            WaystoneEntry waystone = WaystoneManager.resolveWarpTarget(player, message.getEntry());
            if (waystone == null || waystone.isGlobal() && Waystones.getConfig().antiqueAtlasGlobalMarkers) {
                return;
            }
            if (message.isMark()) {
                AntiqueAtlasMarkers.mark(player, waystone);
            } else {
                AntiqueAtlasMarkers.unmark(player, waystone);
            }
            WaystoneManager.sendPlayerWaystones(player);
        });
        return null;
    }
}
