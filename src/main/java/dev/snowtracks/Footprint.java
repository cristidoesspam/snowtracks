package dev.snowtracks;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

final class Footprint extends SingleQuadParticle {
	private static final int WIPE_SPEEDUP = 3;
	static final double LIFT = 0.015;

	private final Quaternionf rotation;
	private final BlockPos ground;
	private final BlockState surface;
	private final float baseAlpha;
	private int ticks;
	private boolean wiping;

	Footprint(ClientLevel level, double x, double z, Surface.Spot spot, TextureAtlasSprite sprite, double headX, double headZ,
			float tile, float alpha, int lifetime) {
		super(level, x, spot.top() + LIFT, z, sprite);
		this.hasPhysics = false;
		this.ground = spot.pos();
		this.surface = spot.state();
		this.lifetime = lifetime;
		this.quadSize = tile / 2f;
		this.baseAlpha = alpha;
		this.alpha = alpha;
		this.setColor(spot.surface().red, spot.surface().green, spot.surface().blue);
		this.rotation = new Quaternionf().rotateY((float) Math.atan2(-headX, -headZ)).rotateX(-Mth.HALF_PI);
	}

	@Override
	public void tick() {
		age += wiping ? 1 + WIPE_SPEEDUP : 1;
		if (age >= lifetime) {
			remove();
			return;
		}
		if ((++ticks & 7) == 0) {
			refresh();
		}
		alpha = baseAlpha * Mth.clamp((1f - age / (float) lifetime) / 0.75f, 0f, 1f);
	}

	private void refresh() {
		if (level.getBlockState(ground) != surface) {
			remove();
			return;
		}
		wiping = level.isRaining() && level.canSeeSky(ground.above())
			&& level.getBiome(ground).value().getPrecipitationAt(ground, level.getSeaLevel()) != Biome.Precipitation.NONE;
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partialTickTime) {
		extractRotatedQuad(state, camera, rotation, partialTickTime);
	}

	@Override
	protected Layer getLayer() {
		return Layer.TRANSLUCENT;
	}
}
