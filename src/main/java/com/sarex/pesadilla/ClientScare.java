package com.sarex.pesadilla;

import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Estado del susto en el cliente (sin clases de render, seguro en servidor). */
@Mod.EventBusSubscriber(modid = PesadillaMod.MODID, value = Dist.CLIENT)
public class ClientScare {
    public static int type = -1;
    public static int ticks = 0;
    public static int total = 1;
    public static final RandomSource RNG = RandomSource.create();

    public static void start(int t) {
        type = t;
        total = switch (t) {
            case 0 -> 50;
            case 1 -> 50;
            default -> 4;
        };
        ticks = total;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticks > 0) {
            ticks--;
            if (ticks == 0) type = -1;
        }
    }
}
