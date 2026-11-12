package net.blay09.mods.waystones.network.handler;

import net.blay09.mods.waystones.PlayerWaystoneData;
import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.client.gui.GuiWarpStone;
import net.blay09.mods.waystones.compat.JourneyMapCompat;
import net.blay09.mods.waystones.compat.XaeroMinimapCompat;
import net.blay09.mods.waystones.network.message.MessageWaystoneRenamed;
import net.blay09.mods.waystones.util.WaystoneEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class HandlerWaystoneRenamed implements IMessageHandler<MessageWaystoneRenamed, IMessage> {

    @Override
    public IMessage onMessage(final MessageWaystoneRenamed message, final MessageContext ctx) {
        Waystones.proxy.addScheduledTask(() -> {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player == null) return;

            // Update in player's known waystones NBT
            NBTTagCompound tagCompound = PlayerWaystoneData.getOrCreateWaystonesTag(player);
            NBTTagList tagList = tagCompound.getTagList(PlayerWaystoneData.WAYSTONE_LIST, Constants.NBT.TAG_COMPOUND);

            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound entryCompound = tagList.getCompoundTagAt(i);
                WaystoneEntry entry = WaystoneEntry.read(entryCompound);
                if (entry.getDimensionId() == message.getDimensionId() && entry.getPos()
                    .equals(message.getPos())) {
                    entryCompound.setString("Name", message.getNewName());
                    break;
                }
            }

            // Update pinned waystones if this one was pinned
            NBTTagList pinnedList = tagCompound
                .getTagList(PlayerWaystoneData.PINNED_WAYSTONES, Constants.NBT.TAG_STRING);
            for (int i = 0; i < pinnedList.tagCount(); i++) {
                if (pinnedList.getStringTagAt(i)
                    .equals(message.getOldName())) {
                    pinnedList.removeTag(i);
                    pinnedList.appendTag(new net.minecraft.nbt.NBTTagString(message.getNewName()));
                    break;
                }
            }
            tagCompound.setTag(PlayerWaystoneData.PINNED_WAYSTONES, pinnedList);

            // Update known waystones map
            WaystoneEntry known = WaystoneManager.getKnownWaystone(message.getOldName());
            if (known != null && known.getDimensionId() == message.getDimensionId()
                && known.getPos()
                    .equals(message.getPos())) {
                WaystoneManager.removeKnownWaystone(message.getOldName());
                WaystoneEntry renamed = new WaystoneEntry(
                    message.getNewName(),
                    message.getDimensionId(),
                    message.getPos());
                renamed.setVillage(known.isVillage());
                WaystoneManager.addKnownWaystone(renamed);
            }

            // Update server waystones if global
            WaystoneEntry serverEntry = WaystoneManager.getServerWaystone(message.getOldName());
            if (serverEntry != null && serverEntry.getDimensionId() == message.getDimensionId()
                && serverEntry.getPos()
                    .equals(message.getPos())) {
                WaystoneManager.removeServerWaystoneByName(message.getOldName());
                WaystoneEntry newEntry = new WaystoneEntry(
                    message.getNewName(),
                    message.getDimensionId(),
                    message.getPos());
                newEntry.setGlobal(true);
                newEntry.setVillage(serverEntry.isVillage());
                WaystoneManager.addServerWaystoneDirectly(newEntry);
            }

            JourneyMapCompat
                .renameWaypoint(message.getOldName(), message.getNewName(), message.getDimensionId(), message.getPos());
            XaeroMinimapCompat
                .renameWaypoint(message.getOldName(), message.getNewName(), message.getDimensionId(), message.getPos());

            if (Minecraft.getMinecraft().currentScreen instanceof GuiWarpStone) {
                ((GuiWarpStone) Minecraft.getMinecraft().currentScreen).refreshEntries();
            }
        });
        return null;
    }
}
