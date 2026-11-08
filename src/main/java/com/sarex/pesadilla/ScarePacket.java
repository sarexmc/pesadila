package com.sarex.pesadilla;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** type 0 = cara jumpscare, 1 = apagon de pantalla, 2 = flash subliminal */
public class ScarePacket {
    public final int type;

    public ScarePacket(int type) {
        this.type = type;
    }

    public static void encode(ScarePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.type);
    }

    public static ScarePacket decode(FriendlyByteBuf buf) {
        return new ScarePacket(buf.readVarInt());
    }

    public static void handle(ScarePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientScare.start(msg.type)));
        ctx.get().setPacketHandled(true);
    }
}
