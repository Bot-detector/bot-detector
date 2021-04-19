package com.botdetector.ui;

import com.botdetector.BotDetectorPlugin;
import java.awt.Dimension;
import javax.swing.ImageIcon;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;

public class Icons
{
	public static final Dimension ICON_SIZE = new Dimension(32, 32);
	public static final int TOOLBAR_BUTTON_SIZE = 20;
	private static final int OPTION_DOT_WIDTH = 6;
	private static final int OPTION_DOT_HEIGHT = 6;

	public static final ImageIcon GITHUB_ICON;
	public static final ImageIcon DISCORD_ICON;
	public static final ImageIcon PATREON_ICON;
	public static final ImageIcon WEB_ICON;
	public static final ImageIcon WARNING_ICON;

	static
	{
		final BufferedImage githubIcon = ImageUtil.loadImageResource(BotDetectorPlugin.class, "/github.png");
		GITHUB_ICON = new ImageIcon(githubIcon);

		final BufferedImage discordIcon = ImageUtil.loadImageResource(BotDetectorPlugin.class, "/discord.png");
		DISCORD_ICON = new ImageIcon(discordIcon);

		final BufferedImage patreonIcon = ImageUtil.loadImageResource(BotDetectorPlugin.class, "/patreon.png");
		PATREON_ICON = new ImageIcon(patreonIcon);

		final BufferedImage webIcon = ImageUtil.loadImageResource(BotDetectorPlugin.class, "/web.png");
		WEB_ICON = new ImageIcon(webIcon);

		final BufferedImage warningIcon = ImageUtil.loadImageResource(BotDetectorPlugin.class, "/warning.png");
		WARNING_ICON = new ImageIcon(warningIcon);
	}
}
