package net.blay09.mods.waystones.network.message;

import net.blay09.mods.waystones.util.WaystoneEntry;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class MessageAtlasMarker implements IMessage {

    private WaystoneEntry entry;
    private boolean mark;

    public MessageAtlasMarker() {}

    public MessageAtlasMarker(WaystoneEntry entry, boolean mark) {
        this.entry = entry;
        this.mark = mark;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entry = WaystoneEntry.read(buf);
        mark = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        entry.write(buf);
        buf.writeBoolean(mark);
    }

    public WaystoneEntry getEntry() {
        return entry;
    }

    public boolean isMark() {
        return mark;
    }
}
