package com.botdetector.ui;

import com.botdetector.BotDetectorPlugin;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Icons {

    public static final Dimension ICON_SIZE = new Dimension(32, 32);
    public static final int TOOLBAR_BUTTON_SIZE = 20;
    private static final int OPTION_DOT_WIDTH = 6;
    private static final int OPTION_DOT_HEIGHT = 6;

    public static final ImageIcon GITHUB_ICON;
    public static final ImageIcon GITHUB_ICON_HOVER;

    public static final ImageIcon DISCORD_ICON;
    public static final ImageIcon DISCORD_ICON_HOVER;

    static{

        final BufferedImage githubIcon = ImageUtil.getResourceStreamFromClass(BotDetectorPlugin.class, "/github.png");
        final BufferedImage githubIconHover = ImageUtil.getResourceStreamFromClass(BotDetectorPlugin.class, "/githubon.png");
        GITHUB_ICON = new ImageIcon(githubIcon);
        GITHUB_ICON_HOVER = new ImageIcon(githubIconHover);

        final BufferedImage discordIcon = ImageUtil.getResourceStreamFromClass(BotDetectorPlugin.class, "/discord.png");
        final BufferedImage discordIconHover = ImageUtil.getResourceStreamFromClass(BotDetectorPlugin.class, "/discordon.png");
        DISCORD_ICON = new ImageIcon(discordIcon);
        DISCORD_ICON_HOVER = new ImageIcon(discordIconHover);



    }
}
