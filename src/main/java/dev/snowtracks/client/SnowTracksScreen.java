package dev.snowtracks.client;

import dev.snowtracks.TrackConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public class SnowTracksScreen extends OptionsSubScreen {
	private static final String KEY = "snowtracks.option.";

	private final TrackConfig config = TrackConfig.get();

	public SnowTracksScreen(Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable("snowtracks.screen.title"));
	}

	@Override
	protected void addOptions() {
		list.addBig(toggle("enabled", config.enabled, v -> config.enabled = v));
		list.addBig(slider("lifetime", TrackConfig.MIN_SECONDS, TrackConfig.MAX_SECONDS, config.lifetimeSeconds,
			v -> Component.translatable(KEY + "seconds.value", Component.translatable(KEY + "lifetime"), v),
			v -> config.lifetimeSeconds = v));
		list.addBig(slider("darkness", 10, 100, config.darkness,
			v -> Component.translatable(KEY + "percent.value", Component.translatable(KEY + "darkness"), v),
			v -> config.darkness = v));
		list.addSmall(
			toggle("other_players", config.showOtherPlayers, v -> config.showOtherPlayers = v),
			toggle("mobs", config.showMobs, v -> config.showMobs = v));
		list.addBig(slider("max_prints", TrackConfig.MIN_PRINTS, TrackConfig.MAX_PRINTS, config.maxPrints,
			v -> Component.translatable(KEY + "number.value", Component.translatable(KEY + "max_prints"), v),
			v -> config.maxPrints = v));
	}

	@Override
	public void removed() {
		TrackConfig.save();
		super.removed();
	}

	private static <T> OptionInstance.TooltipSupplier<T> tooltip(String name) {
		Tooltip tip = Tooltip.create(Component.translatable(KEY + name + ".tooltip"));
		return value -> tip;
	}

	private static OptionInstance<Boolean> toggle(String name, boolean value, Consumer<Boolean> setter) {
		return OptionInstance.createBoolean(KEY + name, tooltip(name), value, setter::accept);
	}

	private static OptionInstance<Integer> slider(String name, int min, int max, int value, IntFunction<Component> label, IntConsumer setter) {
		return new OptionInstance<>(KEY + name, tooltip(name), (caption, v) -> label.apply(v),
			new OptionInstance.IntRange(min, max), value, setter::accept);
	}
}
