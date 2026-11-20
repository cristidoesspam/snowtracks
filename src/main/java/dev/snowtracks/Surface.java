package dev.snowtracks;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

public enum Surface {
	SNOW("crisp", 0.80f, 0.88f, 1.00f, 1.0f, 1.0f),
	POWDER("soft", 0.78f, 0.84f, 0.95f, 1.35f, 0.6f),
	SAND("crisp", 0.72f, 0.58f, 0.36f, 1.0f, 1.0f),
	MUD("wet", 0.60f, 0.52f, 0.48f, 1.1f, 1.5f);

	public record Spot(Surface surface, BlockPos pos, BlockState state, double top) {}

	public final String style;
	public final float red;
	public final float green;
	public final float blue;
	public final float size;
	public final float life;

	Surface(String style, float red, float green, float blue, float size, float life) {
		this.style = style;
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.size = size;
		this.life = life;
	}

	public static Surface of(BlockState state) {
		if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
			return SNOW;
		} else if (state.is(Blocks.POWDER_SNOW)) {
			return POWDER;
		} else if (state.is(BlockTags.SAND)) {
			return SAND;
		} else if (state.is(Blocks.MUD)) {
			return MUD;
		}
		return null;
	}

	public static Spot probe(BlockGetter level, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y + 1.0E-4, z);
		BlockState state = level.getBlockState(pos);
		Surface surface = of(state);
		if (surface == null) {
			pos = pos.below();
			state = level.getBlockState(pos);
			surface = of(state);
		}
		if (surface == null) {
			return null;
		}
		double height = state.is(Blocks.SNOW) ? state.getValue(SnowLayerBlock.LAYERS) / 8.0 : 1.0;
		return new Spot(surface, pos, state, pos.getY() + height);
	}
}
