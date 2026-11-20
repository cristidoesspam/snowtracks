package dev.snowtracks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public final class Tracker {
	private static final double RANGE_SQ = 48.0 * 48.0;

	private record Gait(Shape shape, double stride, double lateral, float tile, float alpha, boolean animal) {}

	private static final class Walker {
		int seen;
		boolean init;
		double lastX;
		double lastZ;
		double dist;
		double headX;
		double headZ;
		boolean left;
		boolean air;
		double airX;
		double airZ;
		int airTicks;
		boolean dragging;
		double dragX;
		double dragZ;
	}

	private static final Map<Integer, Walker> WALKERS = new HashMap<>();
	private static ClientLevel lastLevel;
	private static int tick;

	private Tracker() {}

	public static void reset() {
		WALKERS.clear();
		Prints.clear();
	}

	public static void tick(ClientLevel level) {
		if (level != lastLevel) {
			lastLevel = level;
			reset();
		}
		TrackConfig config = TrackConfig.get();
		LocalPlayer me = Minecraft.getInstance().player;
		if (!config.enabled || me == null) {
			reset();
			return;
		}

		tick++;
		for (Entity e : level.entitiesForRendering()) {
			if (e.distanceToSqr(me) > RANGE_SQ) {
				continue;
			}
			Walker w = WALKERS.computeIfAbsent(e.getId(), id -> new Walker());
			w.seen = tick;
			if (e instanceof Player p) {
				if (p == me || config.showOtherPlayers) {
					walk(level, e, w, humanGait(p));
				}
			} else if (config.showMobs && e instanceof LivingEntity) {
				Track track = Track.of(e.getType());
				if (track == Track.RABBIT) {
					hop(level, e, w);
				} else if (track != Track.NONE) {
					walk(level, e, w, mobGait(track, e));
				}
			}
			if (e instanceof Leashable leashed) {
				drag(level, e, w, leashed, me, config);
			}
		}
		if (tick % 100 == 0) {
			WALKERS.values().removeIf(w -> w.seen < tick - 40);
		}
	}

	private static Gait humanGait(Player p) {
		if (p.isCrouching()) {
			return new Gait(Shape.BOOT, 0.5, 0.09, 0.38f, 0.6f, false);
		}
		return p.isSprinting()
			? new Gait(Shape.BOOT, 1.15, 0.12, 0.50f, 1f, false)
			: new Gait(Shape.BOOT, 0.75, 0.15, 0.50f, 1f, false);
	}

	private static Gait mobGait(Track track, Entity e) {
		float s = Mth.clamp(e.getBbWidth() / 0.6f, 0.5f, 2.5f);
		return switch (track) {
			case PAW -> new Gait(Shape.PAW, 0.5 * s, 0.08 * s, 0.24f * s, 1f, true);
			case HOOF -> new Gait(Shape.HOOF, 0.7 * s, 0.1 * s, 0.17f * s, 1f, true);
			default -> new Gait(Shape.BIRD, Math.max(0.3, 0.45 * s), 0.04 * s, Math.max(0.16f, 0.24f * s), 1f, true);
		};
	}

	private static boolean grounded(Entity e) {
		if (!e.onGround() || e.isPassenger() || e.isInWater() || e.isSwimming() || e.isSpectator() || e.isRemoved()) {
			return false;
		}
		return !(e instanceof LivingEntity living && (living.isFallFlying() || living.isDeadOrDying()));
	}

	private static void walk(ClientLevel level, Entity e, Walker w, Gait gait) {
		double x = e.getX();
		double z = e.getZ();
		double dx = x - w.lastX;
		double dz = z - w.lastZ;
		boolean fresh = !w.init;
		w.init = true;
		w.lastX = x;
		w.lastZ = z;
		double d = Math.hypot(dx, dz);
		if (fresh || d > 4.0) {
			w.dist = 0;
			w.headX = 0;
			w.headZ = 0;
			return;
		}
		if (d < 1.0E-3 || !grounded(e)) {
			return;
		}
		w.dist += d;
		w.headX += dx;
		w.headZ += dz;
		if (w.dist < gait.stride()) {
			return;
		}
		double len = Math.hypot(w.headX, w.headZ);
		double fx = w.headX / len;
		double fz = w.headZ / len;
		w.dist = 0;
		w.headX = 0;
		w.headZ = 0;
		if (len < 0.05) {
			return;
		}
		double side = (w.left ? -1 : 1) * gait.lateral();
		w.left = !w.left;
		Prints.put(level, gait.shape(), x - fz * side, e.getY(), z + fx * side, fx, fz, gait.tile(), gait.alpha(), gait.animal());
	}

	private static void hop(ClientLevel level, Entity e, Walker w) {
		double x = e.getX();
		double z = e.getZ();
		if (!grounded(e)) {
			if (!w.air) {
				w.air = true;
				w.airX = x;
				w.airZ = z;
				w.airTicks = 0;
			}
			w.airTicks++;
			return;
		}
		if (!w.air) {
			return;
		}
		w.air = false;
		if (w.airTicks < 3) {
			return;
		}
		double dx = x - w.airX;
		double dz = z - w.airZ;
		double len = Math.hypot(dx, dz);
		if (len < 0.2) {
			float yaw = e.getYRot() * Mth.DEG_TO_RAD;
			dx = -Mth.sin(yaw);
			dz = Mth.cos(yaw);
			len = 1;
		}
		Prints.put(level, Shape.RABBIT, x, e.getY(), z, dx / len, dz / len, 0.5f * Mth.clamp(e.getBbWidth() / 0.4f, 0.6f, 1.5f), 1f, true);
	}

	private static void drag(ClientLevel level, Entity e, Walker w, Leashable leashed, LocalPlayer me, TrackConfig config) {
		Entity holder = leashed.getLeashHolder();
		boolean pulled = holder != null && (holder == me || config.showOtherPlayers && holder instanceof Player)
			&& e.onGround() && !e.isPassenger()
			&& leashed.leashDistanceTo(holder) > leashed.leashElasticDistance() - holder.getBbWidth() - e.getBbWidth();
		if (!pulled) {
			w.dragging = false;
			return;
		}
		double x = e.getX();
		double z = e.getZ();
		if (!w.dragging) {
			w.dragging = true;
			w.dragX = x;
			w.dragZ = z;
			return;
		}
		double dx = x - w.dragX;
		double dz = z - w.dragZ;
		double d = Math.hypot(dx, dz);
		float tile = Mth.clamp(0.5f + 0.3f * e.getBbWidth(), 0.6f, 1.0f);
		if (d > 4.0) {
			w.dragX = x;
			w.dragZ = z;
		} else if (d >= tile * 0.7) {
			Prints.put(level, Shape.DRAG, w.dragX + dx / 2, e.getY(), w.dragZ + dz / 2, dx / d, dz / d, tile, 0.9f, false);
			w.dragX = x;
			w.dragZ = z;
		}
	}
}
