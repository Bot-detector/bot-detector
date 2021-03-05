package com.botdetector;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import net.runelite.api.Client;
import net.runelite.client.events.SessionClose;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.account.SessionManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.info.JRichTextPane;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

public class BotDetectorPanel extends PluginPanel {

    @Inject
    @Nullable
    private Client client;

    @Inject
    private EventBus eventBus;

    private boolean active;

    private JPanel statsPanel = new JPanel();
    JLabel uploads = new JLabel(htmlLabel("Names Uploaded: ", "0"));

    @Override
    public void onActivate()
    {
        active = true;

    }

    @Override
    public void onDeactivate()
    {
        active = false;
    }

    void init()
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));


        statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        statsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        statsPanel.setLayout(new GridLayout(0, 1));

        final Font boldFont = FontManager.getRunescapeBoldFont();

        JLabel title =  new JLabel(htmlLabel("Statistics: ", ""));
        title.setFont(boldFont);

        statsPanel.add(title);
        statsPanel.add(uploads);


        add(statsPanel, BorderLayout.NORTH);
        eventBus.register(this);

    }

    private static String htmlLabel(String key, String value)
    {
        return "<html><body style = 'color:#a5a5a5'>" + key + "<span style = 'color:white'>" + value + "</span></body></html>";
    }


    @Subscribe
    public void onSessionOpen(SessionOpen sessionOpen)
    {
    }

    @Subscribe
    public void onSessionClose(SessionClose e)
    {
    }

    void updateUploads()
    {
        uploads.setText(htmlLabel("Names Uploaded: ",
                String.valueOf(BotDetectorPlugin.numNamesSubmitted)));
        statsPanel.updateUI();
    }


}
