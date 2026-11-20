package dev.snowtracks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TrackConfig {
	public static final int MIN_SECONDS = 5;
	public static final int MAX_SECONDS = 600;
	public static final int MIN_PRINTS = 50;
	public static final int MAX_PRINTS = 3000;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static TrackConfig current = new TrackConfig();

	public boolean enabled = true;
	public int lifetimeSeconds = 60;
	public int darkness = 80;
	public boolean showOtherPlayers = true;
	public boolean showMobs = true;
	public int maxPrints = 600;

	public static TrackConfig get() {
		return current;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("snowtracks.json");
	}

	public static void load() {
		Path file = file();
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				TrackConfig loaded = GSON.fromJson(reader, TrackConfig.class);
				if (loaded != null) {
					current = loaded;
					current.lifetimeSeconds = Mth.clamp(current.lifetimeSeconds, MIN_SECONDS, MAX_SECONDS);
					current.darkness = Mth.clamp(current.darkness, 10, 100);
					current.maxPrints = Mth.clamp(current.maxPrints, MIN_PRINTS, MAX_PRINTS);
				}
			} catch (Exception e) {
				SnowTracks.LOGGER.warn("Could not read {}, using defaults", file, e);
			}
		}
		save();
	}

	public static void save() {
		Path file = file();
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(current, writer);
			}
		} catch (IOException e) {
			SnowTracks.LOGGER.warn("Could not write {}", file, e);
		}
	}
}
