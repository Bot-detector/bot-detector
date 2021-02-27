package net.runelite.client.plugins.botdetector;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("botdetector")
public interface BotDetectorConfig extends Config
{
	@ConfigItem(
			position = 1,
			keyName = "nameCollect",
			name = "Dump All Names To Server",
			description = "Sends names to the server. Restart RuneLite to send a new list."
	)
	default boolean nameCollect() { return false; }
}