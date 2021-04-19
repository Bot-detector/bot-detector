package com.botdetector.ui.GameOverlays;

import com.botdetector.BotDetectorConfig;
import com.botdetector.BotDetectorPlugin;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import net.runelite.api.Client;
import net.runelite.api.Player;
import javax.inject.Inject;
import java.util.List;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

public class BotDetectorTileOverlay extends Overlay
{
	private final BotDetectorConfig config;
	private final Client client;
	private final BotDetectorPlugin plugin;

	private List<Player> playersToTile = new ArrayList<>();

	private boolean playersHaveChanged;

	@Inject
	private BotDetectorTileOverlay(BotDetectorConfig config, Client client, BotDetectorPlugin plugin)
	{
		this.config = config;
		this.client = client;
		this.plugin = plugin;
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.MED);

		playersHaveChanged = false;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableTileLabels())
		{
			return null;
		}

		if (playersHaveChanged)
		{
			playersToTile = getPlayersToTile();
		}

		if (playersToTile.size() == 0)
		{
			return null;
		}

		Color color = config.getTileColor();

		for (Player player : playersToTile)
		{

			if (player == null || player.getName() == null)
			{
				continue;
			}

			final Polygon poly = player.getCanvasTilePoly();

			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, color);
			}
		}

		setPlayersHaveChanged(false);

		return null;
	}

	public List<Player> getPlayersToTile()
	{
		List<Player> toTile = new ArrayList<>();
		List<String> reportedPlayers = plugin.getSeenReportedPlayers();

		for (Player player : client.getPlayers())
		{
			if (reportedPlayers.contains(player.getName()))
			{
				toTile.add(player);
			}
		}

		return toTile;
	}

	public void setPlayersHaveChanged(boolean playersHaveChanged)
	{
		this.playersHaveChanged = playersHaveChanged;
	}
}

