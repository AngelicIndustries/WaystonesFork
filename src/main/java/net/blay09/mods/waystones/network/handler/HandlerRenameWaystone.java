package net.blay09.mods.waystones.network.handler;

import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.block.TileWaystone;
import net.blay09.mods.waystones.compat.AntiqueAtlasMarkers;
import net.blay09.mods.waystones.network.NetworkHandler;
import net.blay09.mods.waystones.network.message.MessageRenameWaystone;
import net.blay09.mods.waystones.network.message.MessageWaystoneRenamed;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class HandlerRenameWaystone implements IMessageHandler<MessageRenameWaystone, IMessage> {

    @Override
    public IMessage onMessage(final MessageRenameWaystone message, final MessageContext ctx) {
        Waystones.proxy.addScheduledTask(() -> {
            EntityPlayer player = ctx.getServerHandler().playerEntity;
            WaystoneEntry entry = message.getEntry();
            String newName = message.getNewName();
            String oldName = entry.getName();

            if (!player.capabilities.isCreativeMode) {
                player.addChatComponentMessage(new ChatComponentTranslation("waystones:renameCreativeOnly"));
                return;
            }

            World world = MinecraftServer.getServer()
                .worldServerForDimension(entry.getDimensionId());
            TileEntity tileEntity = world.getTileEntity(
                entry.getPos()
                    .getX(),
                entry.getPos()
                    .getY(),
                entry.getPos()
                    .getZ());

            if (!(tileEntity instanceof TileWaystone)) {
                return;
            }

            TileWaystone waystone = (TileWaystone) tileEntity;

            // Check if new name is already taken by a global waystone
            WaystoneEntry existingGlobal = WaystoneManager.getServerWaystone(newName);
            if (existingGlobal != null && !existingGlobal.equals(entry)) {
                player.addChatComponentMessage(new ChatComponentTranslation("waystones:nameOccupied", newName));
                return;
            }

            // Check if new name is already taken by a player waystone
            WaystoneEntry[] playerWaystones = PlayerWaystoneData.fromPlayer(player)
                .getWaystones();
            for (WaystoneEntry playerEntry : playerWaystones) {
                if (playerEntry.getName()
                    .equals(newName) && !playerEntry.equals(entry)) {
                    player.addChatComponentMessage(new ChatComponentTranslation("waystones:nameOccupied", newName));
                    return;
                }
            }

            boolean wasGlobal = WaystoneManager.getServerWaystone(oldName) != null;
            if (wasGlobal) {
                WaystoneManager.removeServerWaystone(entry);
            }
            waystone.setWaystoneName(newName);

            if (wasGlobal) {
                WaystoneManager.addServerWaystone(new WaystoneEntry(waystone));
            }

            if (Waystones.hasAntiqueAtlas) {
                WaystoneEntry renamed = new WaystoneEntry(waystone);
                for (EntityPlayerMP obj : MinecraftServer.getServer()
                    .getConfigurationManager().playerEntityList) {
                    AntiqueAtlasMarkers.relabel(obj, renamed);
                }
            }

            MessageWaystoneRenamed renameMsg = new MessageWaystoneRenamed(
                entry.getDimensionId(),
                entry.getPos(),
                oldName,
                newName);
            for (EntityPlayerMP obj : MinecraftServer.getServer()
                .getConfigurationManager().playerEntityList) {
                NetworkHandler.channel.sendTo(renameMsg, obj);
            }
        });
        return null;
    }
}
