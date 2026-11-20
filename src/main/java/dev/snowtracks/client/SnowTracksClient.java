package dev.snowtracks.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.snowtracks.SnowTracks;
import dev.snowtracks.TrackConfig;
import dev.snowtracks.Tracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class SnowTracksClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY =
		KeyMapping.Category.register(Identifier.fromNamespaceAndPath(SnowTracks.MOD_ID, SnowTracks.MOD_ID));

	@Override
	public void onInitializeClient() {
		TrackConfig.load();

		ClientTickEvents.END_LEVEL_TICK.register(Tracker::tick);
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
			Identifier.fromNamespaceAndPath(SnowTracks.MOD_ID, "reset"),
			(ResourceManagerReloadListener) manager -> Tracker.reset());

		KeyMapping openSettingsKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.snowtracks.settings", InputConstants.UNKNOWN.getValue(), CATEGORY));
		KeyMapping toggleKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.snowtracks.toggle", InputConstants.UNKNOWN.getValue(), CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				TrackConfig config = TrackConfig.get();
				config.enabled = !config.enabled;
				TrackConfig.save();
				if (client.player != null) {
					client.player.sendOverlayMessage(Component.translatable(
						config.enabled ? "snowtracks.toggle.on" : "snowtracks.toggle.off"));
				}
			}
			while (openSettingsKey.consumeClick()) {
				if (client.player != null) {
					client.gui.setScreen(new SnowTracksScreen(client.gui.screen()));
				}
			}
		});
	}
}
