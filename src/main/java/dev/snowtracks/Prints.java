package dev.snowtracks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayDeque;

public final class Prints {
	private static final ArrayDeque<Footprint> PEOPLE = new ArrayDeque<>();
	private static final ArrayDeque<Footprint> ANIMALS = new ArrayDeque<>();

	private Prints() {}

	public static void clear() {
		PEOPLE.forEach(Footprint::remove);
		ANIMALS.forEach(Footprint::remove);
		PEOPLE.clear();
		ANIMALS.clear();
	}

	public static void put(ClientLevel level, Shape shape, double x, double y, double z, double headX, double headZ,
			float tile, float alpha, boolean animal) {
		Surface.Spot spot = Surface.probe(level, x, y, z);
		if (spot == null || spot.surface() == Surface.POWDER && level.getBlockState(spot.pos().above()).is(Blocks.POWDER_SNOW)) {
			return;
		}
		double reach = tile * 0.35;
		for (int i = 0; i < 4; i++) {
			double dx = (i & 1) == 0 ? -reach : reach;
			double dz = (i & 2) == 0 ? -reach : reach;
			Surface.Spot corner = Surface.probe(level, x + dx, spot.top() - 0.05, z + dz);
			if (corner == null || Math.abs(corner.top() - spot.top()) > 1.0E-3) {
				return;
			}
		}

		TrackConfig config = TrackConfig.get();
		Surface surface = spot.surface();
		Minecraft mc = Minecraft.getInstance();
		TextureAtlasSprite sprite = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(shape.sprite(surface));
		int life = Math.round(config.lifetimeSeconds * 20 * surface.life * (0.85f + 0.3f * level.getRandom().nextFloat()));
		Footprint print = new Footprint(level, x, z, spot, sprite, headX, headZ, tile * surface.size,
			Mth.clamp(config.darkness / 100f * alpha, 0f, 1f), life);

		(animal ? ANIMALS : PEOPLE).add(print);
		int cap = config.maxPrints;
		if (PEOPLE.size() + ANIMALS.size() > cap) {
			PEOPLE.removeIf(p -> !p.isAlive());
			ANIMALS.removeIf(p -> !p.isAlive());
		}
		while (PEOPLE.size() + ANIMALS.size() > cap) {
			(ANIMALS.size() > cap / 3 || PEOPLE.isEmpty() ? ANIMALS : PEOPLE).poll().remove();
		}
		mc.particleEngine.add(print);
	}
}
