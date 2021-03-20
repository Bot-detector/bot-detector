package com.botdetector;

import com.google.inject.Inject;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import lombok.SneakyThrows;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.Notifier;
import net.runelite.client.events.SessionClose;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

public class BotDetectorPanel extends PluginPanel {

    @Inject
    @Nullable
    private Client client;

    @Inject
    private EventBus eventBus;

    @Inject
    private Notifier notifier;


    private final Font boldFont = FontManager.getRunescapeBoldFont();
    private static final int MAX_RSN_LENGTH = 12;
    private boolean active;
    private boolean loading;

    private final IconTextField searchBar;

    private JPanel statsPanel;
    private JPanel playerInfoPanel;

    public PlayerStats ps;

    JLabel uploads;
    JLabel numReports;
    JLabel numBans;
    JLabel accuracy;

    JLabel playerName;
    JLabel playerGroupID;
    JLabel reportBtnTitle = new JLabel("<html><body style = 'color: #a5a5a5'>"
                                + "Report as Bot?"
                                +"</body></html>");

    JButton refreshStatsBtn;
    JButton reportBtn;
    JButton denyBtn;

    @Subscribe
    public void onSessionOpen(SessionOpen sessionOpen)
    {

    }

    @Subscribe
    public void onSessionClose(SessionClose e)
    {
    }

    @SneakyThrows
    @Override
    public void onActivate()
    {
        super.onActivate();
        searchBar.requestFocusInWindow();
    }

    @Override
    public void onDeactivate()
    {
        active = false;
    }

    @Inject
    public BotDetectorPanel(@Nullable Client client)
    {
        loading = false;
        ps = new PlayerStats();

        //Panels
        statsPanel = new JPanel();
        playerInfoPanel =  new JPanel();

        //Buttons
        reportBtn = new JButton("Report");
        reportBtn.createToolTip();
        reportBtn.setToolTipText("Submit account as a probable offender.");
        reportBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BotDetectorPlugin.http.reportPlayer(
                  playerName.getText()
                );
            }
        });

        denyBtn = new JButton("Don't Report");
        denyBtn.createToolTip();
        denyBtn.setToolTipText("Player is real and not a rule-breaker.");
        denyBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                removeReportButtons();
            }
        });

        //Stats Panel Items
        uploads = new JLabel(htmlLabel("Names Uploaded: ", "0", "#a5a5a5", "white"));
        uploads.createToolTip();
        uploads.setToolTipText("Number of names uploaded during this RuneLite session.");
        numReports = new JLabel(htmlLabel("Reports Made: ", "", "#a5a5a5", "white"));
        numReports.createToolTip();
        numReports.setToolTipText("How many reports you have made.");
        numBans = new JLabel(htmlLabel("Confirmed Bans: ", "", "#a5a5a5", "white"));
        numBans.createToolTip();
        numBans.setToolTipText("How many of your reports that have resulted in a player ban.");
        accuracy = new JLabel(htmlLabel("Accuracy: ", "", "#a5a5a5", "white"));
        accuracy.createToolTip();
        accuracy.setToolTipText("% of reports that resulted in a ban.");

        //Player Info Panel
        playerName = new JLabel(htmlLabel("Player Name: ", "---", "#a5a5a5", "white"));
        playerGroupID = new JLabel(htmlLabel("Group ID: ", "---", "#a5a5a5", "white"));

        //Search Bar Setup
        searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        searchBar.setMinimumSize(new Dimension(0, 30));
        searchBar.addActionListener(e -> {
            try {
                lookupPlayer(true);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        searchBar.addMouseListener(new MouseAdapter()
        {
            @SneakyThrows
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() != 2)
                {
                    return;
                }
                if (client == null)
                {
                    return;
                }

                Player localPlayer = client.getLocalPlayer();

                if (localPlayer != null)
                {
                    lookupPlayer(localPlayer.getName(), false);
                }
            }
        });
        searchBar.addClearListener(() ->
        {
            searchBar.setIcon(IconTextField.Icon.SEARCH);
            searchBar.setEditable(true);
            loading = false;
        });

        add(searchBar);

    }

    void init()
    {
        setLayout(new GridBagLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 10, 0);

        playerInfoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        playerInfoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        playerInfoPanel.setLayout(new GridLayout(0, 1));

        statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        statsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        statsPanel.setLayout(new GridLayout(0, 1));

        JLabel dataTitle =  new JLabel(htmlLabel("Player Data: ", "", "#a5a5a5", "white"));
        dataTitle.setFont((boldFont));

        JLabel statsTitle =  new JLabel(htmlLabel("Statistics: ", "", "#a5a5a5", "white"));
        statsTitle.setFont(boldFont);

        add(searchBar, constraints);
        constraints.gridy++;
        add(statsPanel, constraints);
        constraints.gridy++;
        add(playerInfoPanel, constraints);
        constraints.gridy++;

        statsPanel.add(statsTitle);
        statsPanel.add(uploads);
        statsPanel.add(numReports);
        statsPanel.add(numBans);
        statsPanel.add(accuracy);

        playerInfoPanel.add(dataTitle);
        playerInfoPanel.add(playerName);
        playerInfoPanel.add(playerGroupID);

        eventBus.register(this);

    }

    void updateUploads()
    {
        uploads.setText(htmlLabel("Names Uploaded: ",
                String.valueOf(BotDetectorPlugin.numNamesSubmitted),
                "#a5a5a5", "white"));
    }


    void updatePlayerData(String rsn, String groupID, boolean error)
    {
        if(error)
        {
            playerName.setText(htmlLabel("Player Name: ", rsn , "#a5a5a5", "red"));
            playerGroupID.setText(htmlLabel("Group ID: ", groupID, "#a5a5a5", "red"));
            loading = false;
            searchBar.setEditable(true);
            searchBar.setIcon(IconTextField.Icon.ERROR);
        }
        else
        {
            playerName.setText(htmlLabel("Player Name: ",  sanitizeText(rsn), "#a5a5a5", "white"));
            playerGroupID.setText(htmlLabel("Group ID: ", groupID, "#a5a5a5", "white"));
            searchBar.setEditable(true);
            searchBar.setIcon(IconTextField.Icon.SEARCH);
        }
    }

    void addReportButtons() {
        playerInfoPanel.add(reportBtnTitle);
        playerInfoPanel.add(reportBtn);
        playerInfoPanel.add(denyBtn);

        playerInfoPanel.revalidate();
        playerInfoPanel.repaint();
    }

    void removeReportButtons() {
        playerInfoPanel.remove(reportBtnTitle);
        playerInfoPanel.remove(reportBtn);
        playerInfoPanel.remove(denyBtn);

        playerInfoPanel.revalidate();
        playerInfoPanel.repaint();
    }

    public void lookupPlayer(String rsn, boolean reportable) throws IOException {
        searchBar.setText(rsn);
        lookupPlayer(reportable);
    }

    private void lookupPlayer(boolean reportable) throws IOException {
        removeReportButtons();

        String sanitizedRSN = sanitizeText(searchBar.getText());

        if(sanitizedRSN.length() <= 0)
        {
            return;
        }

        if(sanitizedRSN.length() > MAX_RSN_LENGTH)
        {
            searchBar.setIcon(IconTextField.Icon.ERROR);
            loading = false;
            return;
        }

        searchBar.setIcon(IconTextField.Icon.LOADING_DARKER);
        searchBar.setEditable(false);
        loading = true;

        BotDetectorPlugin.http.getPlayerData(sanitizedRSN, reportable);

    }

    public void updatePlayerStats() {
        numReports.setText("Reports Made: " + ps.getReports());
        numBans.setText("Confirmed Bans: " + ps.getBans());
        accuracy.setText("Accuracy: " + ps.getAccuracy() + "%");
    }

    public void resetPlayerStats() {
        numReports.setText("Reports Made: ");
        numBans.setText("Confirmed Bans: ");
        accuracy.setText("Accuracy: ");
    }

    public void setPlayerStats(PlayerStats stats) throws IOException {
        this.ps = stats;
        updatePlayerStats();
    }

    private static String sanitizeText(String rsn)
    {
        return rsn.replace('\u00A0', ' ');
    }

    private static String htmlLabel(String key, String value, String keyColor, String valueColor)
    {
        return "<html><body style = 'color:"+ keyColor + "'>" + key +
                "<span style = 'color:" + valueColor + "'>" + value +
                "</span></body></html>";
    }
}
