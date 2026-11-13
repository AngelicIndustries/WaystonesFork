package net.blay09.mods.waystones.network.handler;

import net.blay09.mods.waystones.WaystoneManager;
import net.blay09.mods.waystones.Waystones;
import net.blay09.mods.waystones.network.message.MessageRequestWaystones;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class HandlerRequestWaystones implements IMessageHandler<MessageRequestWaystones, IMessage> {

    @Override
    public IMessage onMessage(final MessageRequestWaystones message, final MessageContext ctx) {
        Waystones.proxy
            .addScheduledTask(() -> WaystoneManager.sendPlayerWaystones(ctx.getServerHandler().playerEntity));
        return null;
    }
}
