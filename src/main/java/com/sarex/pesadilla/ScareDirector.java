package com.sarex.pesadilla;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cerebro del miedo: decide cuando y como asustarte. */
@Mod.EventBusSubscriber(modid = PesadillaMod.MODID)
public class ScareDirector {
    public static boolean enabled = true;
    public static int intensity = 2;           // 1..5
    public static boolean lethal = false;      // el Acechador puede matarte
    public static boolean PUT_OUT_TORCHES = true;

    // {min, max} ticks entre sustos segun intensidad
    private static final int[][] INTERVALS = {
            {2400, 4800}, {1500, 3000}, {900, 2000}, {500, 1200}, {240, 600}};

    private static final SoundEvent[] CREEPY = {
            SoundEvents.ZOMBIE_AMBIENT, SoundEvents.PHANTOM_AMBIENT, SoundEvents.GHAST_AMBIENT,
            SoundEvents.ENDERMAN_STARE, SoundEvents.WARDEN_NEARBY_CLOSEST, SoundEvents.SKELETON_AMBIENT,
            SoundEvents.CAT_HISS, SoundEvents.WITHER_AMBIENT, SoundEvents.ELDER_GUARDIAN_AMBIENT,
            SoundEvents.ZOMBIE_VILLAGER_AMBIENT};

    private static final Map<UUID, Long> NEXT = new HashMap<>();
    private static final List<Task> TASKS = new ArrayList<>();

    private record Task(long due, Runnable run) { }

    // ------------------------------------------------------------ eventos
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        final int now = server.getTickCount();

        if (!TASKS.isEmpty()) {
            List<Task> due = new ArrayList<>();
            TASKS.removeIf(t -> {
                if (t.due() <= now) {
                    due.add(t);
                    return true;
                }
                return false;
            });
            due.forEach(t -> t.run().run());
        }

        if (!enabled || now % 20 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isCreative() || p.isSpectator() || !p.isAlive()) continue;
            long next = NEXT.computeIfAbsent(p.getUUID(), k -> (long) now + 1200 + p.getRandom().nextInt(1200));
            if (now >= next) {
                randomScare(p);
                NEXT.put(p.getUUID(), (long) now + interval(p));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        NEXT.remove(event.getEntity().getUUID());
    }

    private static int interval(ServerPlayer p) {
        int[] r = INTERVALS[Math.max(1, Math.min(5, intensity)) - 1];
        return r[0] + p.getRandom().nextInt(r[1] - r[0]);
    }

    // ------------------------------------------------------------ sustos
    public static void randomScare(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos pos = p.blockPosition();
        boolean underground = !level.canSeeSky(pos);
        boolean dark = level.getMaxLocalRawBrightness(pos) < 7;
        int i = intensity;
        int[] w = {
                14,                                            // 0 pasos a tu espalda
                14,                                            // 1 sonido detras
                8,                                             // 2 puerta
                PUT_OUT_TORCHES ? 6 : 0,                       // 3 antorchas se apagan
                6 + i,                                         // 4 apagon
                10,                                            // 5 vigilante
                (dark || underground) ? 4 + i * 3 : 2 + i,     // 6 cazador
                4 + i,                                         // 7 flash subliminal
                5,                                             // 8 falso "Herobrine se unio"
                3 + i * 2                                      // 9 jumpscare directo
        };
        int total = 0;
        for (int x : w) total += x;
        int roll = p.getRandom().nextInt(total);
        int pick = 0;
        for (; pick < w.length; pick++) {
            roll -= w[pick];
            if (roll < 0) break;
        }
        switch (pick) {
            case 0 -> footsteps(p);
            case 1 -> soundBehind(p);
            case 2 -> door(p);
            case 3 -> { if (!putOutTorches(p)) soundBehind(p); }
            case 4 -> blackout(p);
            case 5 -> { if (!spawnStalker(p, false)) soundBehind(p); }
            case 6 -> { if (!spawnStalker(p, true)) soundBehind(p); }
            case 7 -> jumpscare(p, 2);
            case 8 -> fakeJoin(p);
            default -> jumpscare(p, 0);
        }
    }

    public static void jumpscare(ServerPlayer p, int type) {
        ModNetwork.sendTo(p, new ScarePacket(type));
        if (type == 0) {
            playOn(p, SoundEvents.ENDER_DRAGON_GROWL, 1.0F, 0.7F);
            playOn(p, SoundEvents.ENDERMAN_SCREAM, 1.0F, 0.4F);
            playOn(p, SoundEvents.GHAST_SCREAM, 1.0F, 0.5F);
        } else if (type == 2) {
            playOn(p, SoundEvents.GHAST_SCREAM, 0.35F, 0.3F);
        }
    }

    private static void footsteps(ServerPlayer p) {
        int steps = 6 + p.getRandom().nextInt(4);
        for (int i = 0; i < steps; i++) {
            final int idx = i;
            schedule(p, i * 10, () -> {
                if (p.isRemoved() || !p.isAlive()) return;
                double dist = Math.max(2.5D, 9.0D - idx * 0.9D);
                SoundEvent step = p.serverLevel().getBlockState(p.blockPosition().below()).getSoundType().getStepSound();
                playAt(p, step, behind(p, dist), 0.9F, 0.85F);
            });
        }
        schedule(p, steps * 10 + 10, () -> {       // al final, a veces aparece alguien
            if (p.isAlive() && p.getRandom().nextInt(100) < 35) spawnStalker(p, false);
        });
    }

    private static void soundBehind(ServerPlayer p) {
        SoundEvent s = CREEPY[p.getRandom().nextInt(CREEPY.length)];
        float pitch = 0.5F + p.getRandom().nextFloat() * 0.4F;
        playAt(p, s, behind(p, 6.0D), 1.0F, pitch);
    }

    private static void door(ServerPlayer p) {
        Vec3 pos = behind(p, 5.0D);
        playAt(p, SoundEvents.WOODEN_DOOR_OPEN, pos, 1.0F, 0.8F);
        schedule(p, 50, () -> playAt(p, SoundEvents.WOODEN_DOOR_CLOSE, pos, 1.0F, 0.8F));
    }

    private static boolean putOutTorches(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos c = p.blockPosition();
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-8, -4, -8), c.offset(8, 4, 8))) {
            BlockState st = level.getBlockState(pos);
            if (st.is(Blocks.TORCH) || st.is(Blocks.WALL_TORCH)) {
                level.setBlock(pos.immutable(), Blocks.AIR.defaultBlockState(), 3);
                count++;
            }
        }
        if (count > 0) {
            playOn(p, SoundEvents.FIRE_EXTINGUISH, 1.0F, 0.6F);
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false, false));
            schedule(p, 40, () -> footsteps(p));
        }
        return count > 0;
    }

    private static void blackout(ServerPlayer p) {
        jumpscare(p, 1);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0, false, false, false));
        for (int i = 0; i < 5; i++) {
            schedule(p, 10 + i * 22, () -> playOn(p, SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F));
        }
        schedule(p, 90, () -> playAt(p, SoundEvents.ENDERMAN_STARE, behind(p, 3.0D), 1.0F, 0.6F));
    }

    private static void fakeJoin(ServerPlayer p) {
        p.sendSystemMessage(Component.literal("\u00A7eHerobrine joined the game"));
        schedule(p, 400, () -> p.sendSystemMessage(Component.literal("\u00A7eHerobrine left the game")));
    }

    // ------------------------------------------------------------ enemigo
    public static boolean spawnStalker(ServerPlayer p, boolean hunter) {
        ServerLevel level = p.serverLevel();
        if (!level.getEntitiesOfClass(StalkerEntity.class, p.getBoundingBox().inflate(100.0D)).isEmpty()) {
            return false;                                  // solo uno a la vez
        }
        Vec3 spot = hunter ? findSpot(level, p, 22, 32, true) : findSpot(level, p, 26, 40, false);
        if (spot == null) return false;
        StalkerEntity s = ModEntities.STALKER.get().create(level);
        if (s == null) return false;
        s.moveTo(spot.x, spot.y, spot.z, level.random.nextFloat() * 360.0F, 0.0F);
        s.setHunter(hunter);
        level.addFreshEntity(s);
        return true;
    }

    /** Busca un sitio donde quepa el Acechador: detras del jugador (behind) o delante, a min..max bloques. */
    public static Vec3 findSpot(ServerLevel level, ServerPlayer p, double min, double max, boolean behind) {
        double yaw = Math.toRadians(p.getYRot());
        double spread = behind ? 70.0D : 30.0D;
        for (int i = 0; i < 24; i++) {
            double a = yaw + Math.toRadians((level.random.nextDouble() * 2 - 1) * spread) + (behind ? Math.PI : 0.0D);
            double dist = min + level.random.nextDouble() * (max - min);
            double x = p.getX() - Math.sin(a) * dist;
            double z = p.getZ() + Math.cos(a) * dist;
            BlockPos floor = findFloor(level, BlockPos.containing(x, p.getY(), z));
            if (floor != null) {
                return new Vec3(floor.getX() + 0.5D, floor.getY(), floor.getZ() + 0.5D);
            }
        }
        return null;
    }

    private static BlockPos findFloor(ServerLevel level, BlockPos start) {
        if (!level.isLoaded(start)) return null;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dy = 6; dy >= -8; dy--) {
            m.set(start.getX(), start.getY() + dy, start.getZ());
            if (level.getBlockState(m).isAir()
                    && level.getBlockState(m.above()).isAir()
                    && level.getBlockState(m.above(2)).isAir()
                    && level.getBlockState(m.below()).isFaceSturdy(level, m.below(), Direction.UP)) {
                return m.immutable();
            }
        }
        return null;
    }

    // ------------------------------------------------------------ utilidades
    private static Vec3 behind(ServerPlayer p, double dist) {
        double yaw = Math.toRadians(p.getYRot());
        return p.position().add(Math.sin(yaw) * dist, 0.0D, -Math.cos(yaw) * dist);
    }

    public static void playOn(ServerPlayer p, SoundEvent s, float vol, float pitch) {
        p.playNotifySound(s, SoundSource.MASTER, vol, pitch);
    }

    public static void playAt(ServerPlayer p, SoundEvent s, Vec3 pos, float vol, float pitch) {
        p.connection.send(new ClientboundSoundPacket(Holder.direct(s), SoundSource.HOSTILE,
                pos.x, pos.y, pos.z, vol, pitch, p.getRandom().nextLong()));
    }

    private static void schedule(ServerPlayer p, int delayTicks, Runnable r) {
        TASKS.add(new Task((long) p.getServer().getTickCount() + delayTicks, r));
    }
}
