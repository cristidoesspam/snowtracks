package dev.snowtracks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public enum Track {
	NONE,
	PAW,
	HOOF,
	BIRD,
	RABBIT;

	private static final Map<EntityType<?>, Track> CACHE = new HashMap<>();

	public static Track of(EntityType<?> type) {
		return CACHE.computeIfAbsent(type, t -> switch (BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath()) {
			case "wolf", "cat", "ocelot", "fox", "polar_bear", "panda", "armadillo", "sniffer" -> PAW;
			case "horse", "donkey", "mule", "skeleton_horse", "zombie_horse", "camel", "camel_husk", "llama", "trader_llama",
				"cow", "mooshroom", "sheep", "pig", "goat", "hoglin", "zoglin", "ravager" -> HOOF;
			case "chicken", "parrot" -> BIRD;
			case "rabbit" -> RABBIT;
			default -> NONE;
		});
	}
}
