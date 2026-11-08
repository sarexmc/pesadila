package com.sarex.pesadilla;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PesadillaMod.MODID)
public class TerrorCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("terror")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("on").executes(c -> {
                    ScareDirector.enabled = true;
                    return msg(c.getSource(), "Terror ACTIVADO. Que dios te acompane.");
                }))
                .then(Commands.literal("off").executes(c -> {
                    ScareDirector.enabled = false;
                    return msg(c.getSource(), "Terror desactivado (por ahora...).");
                }))
                .then(Commands.literal("intensidad")
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(1, 5)).executes(c -> {
                            ScareDirector.intensity = IntegerArgumentType.getInteger(c, "nivel");
                            return msg(c.getSource(), "Intensidad: " + ScareDirector.intensity + "/5");
                        })))
                .then(Commands.literal("letal")
                        .then(Commands.argument("valor", BoolArgumentType.bool()).executes(c -> {
                            ScareDirector.lethal = BoolArgumentType.getBool(c, "valor");
                            return msg(c.getSource(), "El Acechador letal: " + ScareDirector.lethal);
                        })))
                .then(Commands.literal("susto").executes(c -> {
                    ScareDirector.randomScare(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("jumpscare")
                        .executes(c -> {
                            ScareDirector.jumpscare(c.getSource().getPlayerOrException(), 0);
                            return 1;
                        })
                        .then(Commands.argument("jugador", EntityArgument.player()).executes(c -> {
                            ServerPlayer t = EntityArgument.getPlayer(c, "jugador");
                            ScareDirector.jumpscare(t, 0);
                            return 1;
                        })))
                .then(Commands.literal("acechador").executes(c -> {
                    boolean ok = ScareDirector.spawnStalker(c.getSource().getPlayerOrException(), true);
                    return msg(c.getSource(), ok ? "Algo te observa..." : "No encontre sitio (o ya hay uno cerca).");
                }))
                .then(Commands.literal("vigilante").executes(c -> {
                    boolean ok = ScareDirector.spawnStalker(c.getSource().getPlayerOrException(), false);
                    return msg(c.getSource(), ok ? "Hay una figura a lo lejos..." : "No encontre sitio (o ya hay uno cerca).");
                })));
    }

    private static int msg(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal(text), true);
        return 1;
    }
}
