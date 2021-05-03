/*
 * Copyright (c) 2021, Ferrariic, Seltzer Bro, Cyborger1
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
	public static final ImageIcon TWITTER_ICON;
	public static final ImageIcon WARNING_ICON;
	public static final ImageIcon ERROR_ICON;

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

		final BufferedImage twitterIcon = ImageUtil.loadImageResource(BotDetectorPlugin.class, "/twitter.png");
		TWITTER_ICON = new ImageIcon(twitterIcon);

		final BufferedImage warningIcon = ImageUtil.loadImageResource(BotDetectorPlugin.class, "/warning.png");
		WARNING_ICON = new ImageIcon(warningIcon);

		final BufferedImage errorIcon = ImageUtil.loadImageResource(BotDetectorPlugin.class, "/error.png");
		ERROR_ICON = new ImageIcon(errorIcon);
	}
}
