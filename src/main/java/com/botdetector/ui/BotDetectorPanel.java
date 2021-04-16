package com.botdetector.ui;

import com.botdetector.BotDetectorConfig;
import com.botdetector.BotDetectorPlugin;
import com.botdetector.model.PlayerStats;
import com.botdetector.ui.Icons;
import com.google.inject.Inject;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Set;
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
import net.runelite.client.util.LinkBrowser;

public class BotDetectorPanel extends PluginPanel {

    @Inject
    @Nullable
    private Client client;

    @Inject
    private BotDetectorConfig config;

    @Inject
    private EventBus eventBus;

    @Inject
    private Notifier notifier;


    private final Font boldFont = FontManager.getRunescapeBoldFont();
    private static final int MAX_RSN_LENGTH = 12;
    private boolean active;
    private boolean loading;
    private boolean feedbackBtnsActive;
    private boolean reportBtnsActive;

    private final IconTextField searchBar;

    private JPanel linksPanel;
    private JPanel statsPanel;
    private JPanel playerInfoPanel;
    private JPanel additionalPredictionsPanel;

    public PlayerStats ps;

    JSeparator btnSpacer;
    JLabel anonymousWarning;

    JLabel uploads;
    JLabel numReports;
    JLabel numBans;
    JLabel numPossibleBans;
    JLabel accuracy;

    String currRSN;
    JLabel playerName;
    JLabel playerGroupLabel;
    JLabel playerGroupConfidence;

    JButton reportBtn;
    JButton dontReportBtn;
    JLabel reportBtnTitle = new JLabel("<html><body style = 'color: #a5a5a5'>"
                                + "Report as Bot?"
                                +"</body></html>");

    JButton correctBtn;
    JButton incorrectBtn;
    JLabel feedbackTitle = new JLabel("<html><body style = 'color: #a5a5a5'>"
            + "Is The Prediction Correct?"
            +"</body></html>");

    JButton refreshStatsBtn;



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

        btnSpacer = new JSeparator();

        //Panels
        statsPanel = new JPanel();
        playerInfoPanel =  new JPanel();
        additionalPredictionsPanel = new JPanel();
        additionalPredictionsPanel.setVisible(false);


        //Buttons
        reportBtn = new JButton("Report");
        reportBtn.createToolTip();
        reportBtn.setToolTipText("Submit account as a probable offender.");
        reportBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Reporting: " + currRSN);
                BotDetectorPlugin.http.reportPlayer(
                  currRSN
                );
            }
        });

        dontReportBtn = new JButton("Don't Report");
        dontReportBtn.createToolTip();
        dontReportBtn.setToolTipText("Player is real and not a rule-breaker.");
        dontReportBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                removeReportButtons();
            }
        });

        correctBtn = new JButton("It's Correct!");
        correctBtn.createToolTip();
        correctBtn.setToolTipText("Let us know if our prediction seems correct.");
        correctBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BotDetectorPlugin.http.sendPredictionFeedback(1);
            }
        });

        incorrectBtn = new JButton("I Think It's Wrong");
        incorrectBtn.createToolTip();
        incorrectBtn.setToolTipText("Let us know if our prediction seems incorrect.");
        incorrectBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BotDetectorPlugin.http.sendPredictionFeedback(-1);
            }
        });

        //UI Components
        anonymousWarning = new JLabel(" Anonymous Reporting Active");
        anonymousWarning.setIcon(Icons.WARNING_ICON);
        anonymousWarning.createToolTip();
        anonymousWarning.setToolTipText("Your reports will not be added to your tallies.");

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
        numPossibleBans = new JLabel(htmlLabel("Probable Bans: ", "", "#a5a5a5", "white"));
        numPossibleBans.createToolTip();
        numPossibleBans.setToolTipText("How many of your reports we think could result in bans.");
        accuracy = new JLabel(htmlLabel("Accuracy: ", "", "#a5a5a5", "white"));
        accuracy.createToolTip();
        accuracy.setToolTipText("% of reports that resulted in a ban.");

        //Player Info Panel
        playerName = new JLabel(htmlLabel("Player Name: ", "---", "#a5a5a5", "white"));
        playerGroupLabel = new JLabel(htmlLabel("Prediction: ", "---", "#a5a5a5", "white"));
        playerGroupConfidence = new JLabel(htmlLabel("Confidence: ", "---", "#a5a5a5", "white"));

        //Search Bar Setup
        searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
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

    public void init()
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

        playerInfoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
        playerInfoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        playerInfoPanel.setLayout(new GridLayout(0, 1));

        statsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
        statsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        statsPanel.setLayout(new GridLayout(0, 1));

        playerInfoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
        playerInfoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        playerInfoPanel.setLayout(new GridLayout(0, 1));

        additionalPredictionsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
        additionalPredictionsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        additionalPredictionsPanel.setLayout(new GridLayout(0, 1));

        JLabel dataTitle =  new JLabel(htmlLabel("Player Data: ", "", "#a5a5a5", "white"));
        dataTitle.setFont((boldFont));

        JLabel statsTitle =  new JLabel(htmlLabel("Reporting Statistics: ", "", "#a5a5a5", "white"));
        statsTitle.setFont(boldFont);


        add(linksPanel(), constraints);
        constraints.gridy++;
        add(statsPanel, constraints);
        constraints.gridy++;
        add(searchBar, constraints);
        constraints.gridy++;
        add(playerInfoPanel, constraints);
        constraints.gridy++;
        add(additionalPredictionsPanel, constraints);
        constraints.gridy++;

        statsPanel.add(statsTitle);
        statsPanel.add(uploads);
        statsPanel.add(numReports);
        statsPanel.add(numBans);
        statsPanel.add(numPossibleBans);

        if(config.enableAnonymousReporting()) {
            statsPanel.add(anonymousWarning);
        }
        //TODO Have accuracy reflect manual reports only
        //statsPanel.add(accuracy);

        playerInfoPanel.add(dataTitle);
        playerInfoPanel.add(playerName);
        playerInfoPanel.add(playerGroupLabel);
        playerInfoPanel.add(playerGroupConfidence);

        eventBus.register(this);

    }

    private JPanel linksPanel() {

        JLabel title = new JLabel("Connect With Us: ");

        JLabel webIcon = new JLabel(Icons.WEB_ICON);
        webIcon.setToolTipText("Our Website");
        webIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                LinkBrowser.browse("https://www.osrsbotdetector.com/");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                webIcon.setIcon(Icons.WEB_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                webIcon.setIcon(Icons.WEB_ICON);
            }
        });

        JLabel githubIcon = new JLabel(Icons.GITHUB_ICON);
        githubIcon.setToolTipText("Check Out Our Souce Code");
        githubIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                LinkBrowser.browse("https://github.com/Belieal/flipping-utilities");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                githubIcon.setIcon(Icons.GITHUB_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                githubIcon.setIcon(Icons.GITHUB_ICON);
            }
        });

        JLabel discordIcon = new JLabel(Icons.DISCORD_ICON);
        discordIcon.setToolTipText("Join Our Discord!");
        discordIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                LinkBrowser.browse("https://discord.com/invite/JCAGpcjbfP");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                discordIcon.setIcon(Icons.DISCORD_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                discordIcon.setIcon(Icons.DISCORD_ICON);
            }
        });

        JLabel patreonIcon = new JLabel(Icons.PATREON_ICON);
        patreonIcon.setToolTipText("Help Keep Us Going");
        patreonIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                LinkBrowser.browse("https://www.patreon.com/bot_detector");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                patreonIcon.setIcon(Icons.PATREON_ICON);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                patreonIcon.setIcon(Icons.PATREON_ICON);
            }
        });

        JPanel linksPanel = new JPanel();
        linksPanel.setBorder(new EmptyBorder(0,6,0,0));
        linksPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
        linksPanel.add(title);
        linksPanel.add(webIcon);
        linksPanel.add(discordIcon);
        linksPanel.add(githubIcon);
        linksPanel.add(patreonIcon);

        return linksPanel;
    }

    public void updateUploads()
    {
        uploads.setText(htmlLabel("Names Uploaded: ",
                String.valueOf(BotDetectorPlugin.numNamesSubmitted),
                "#a5a5a5", "white"));
    }


    //You only get here if something went wrong.
    public void updatePlayerData(String rsn, boolean error)
    {
        currRSN = rsn;

        playerName.setText(htmlLabel("Player Name: ", rsn , "#a5a5a5", "red"));
        playerGroupLabel.setText(htmlLabel("Prediction: ", "-----", "#a5a5a5", "red"));
        playerGroupConfidence.setText(htmlLabel("Confidence: ", "-----", "#a5a5a5", "red"));
        loading = false;
        searchBar.setEditable(true);
        searchBar.setIcon(IconTextField.Icon.ERROR);

    }

    public void updatePlayerData(Hashtable<String, String> predictionData, boolean error)
    {
        currRSN = predictionData.get("player_name");
        String predictionLabel = predictionData.get("prediction_label");
        Float confidencePer = (Float.parseFloat(predictionData.get("prediction_confidence"))  * 100);

        if(error)
        {
            playerName.setText(htmlLabel("Player Name: ", currRSN , "#a5a5a5", "red"));
            playerGroupLabel.setText(htmlLabel("Prediction: ", "-----", "#a5a5a5", "red"));
            playerGroupConfidence.setText(htmlLabel("Confidence: ", "-----", "#a5a5a5", "red"));
            loading = false;
            searchBar.setEditable(true);
            searchBar.setIcon(IconTextField.Icon.ERROR);
        }
        else
        {
            playerName.setText(htmlLabel("Player Name: ",  sanitizeText(currRSN),
                    "#a5a5a5", "white"));
            playerGroupLabel.setText(htmlLabel("Prediction: ", predictionLabel,
                    "#a5a5a5", "white"));
            playerGroupConfidence.setText(htmlLabel("Confidence: ", String.format("%.2f", confidencePer) + "%",
                    "#a5a5a5", "white"));
            searchBar.setEditable(true);
            searchBar.setIcon(IconTextField.Icon.SEARCH);
        }
    }

    public void updateAdditionalPredictions(LinkedHashMap<String, String> predictions, boolean error) {

        if(error == false) {
            additionalPredictionsPanel.removeAll();
            additionalPredictionsPanel.setVisible(true);

            JLabel title = new JLabel(htmlLabel("Prediction Breakdown: ",
                    "",
                    "#a5a5a5",
                    "white"));

            title.setFont(boldFont);

            additionalPredictionsPanel.add(title);

            Set<String> keys = predictions.keySet();

            for (String key : keys) {
                additionalPredictionsPanel.add(
                        new JLabel(htmlLabel(key + ": ",
                                String.format("%.2f", Float.parseFloat(predictions.get(key)) * 100) + "%",
                                "#a5a5a5",
                                getPredictionColor(predictions.get(key))
                        ))
                );
            }

            additionalPredictionsPanel.revalidate();
            additionalPredictionsPanel.repaint();
        }

    }

    //TODO Make the colors more dynamic in range
    public String getPredictionColor(String pred_conf) {

        Float conf = Float.parseFloat(pred_conf);

        System.out.print("CONF: " + conf);

        if(conf >= .8) {
            return "green";
        } else if(conf >= .60) {
            return "orange";
        } else {
            return "red";
        }
    }

    public void addFeedbackButtons() {
        feedbackBtnsActive = true;

        playerInfoPanel.add(feedbackTitle);
        playerInfoPanel.add(correctBtn);
        playerInfoPanel.add(incorrectBtn);
        playerInfoPanel.add(btnSpacer);

        playerInfoPanel.revalidate();
        playerInfoPanel.repaint();
    }

    public void removeFeedbackButtons() {

        if(feedbackBtnsActive == true) {
            playerInfoPanel.remove(feedbackTitle);
            playerInfoPanel.remove(correctBtn);
            playerInfoPanel.remove(incorrectBtn);
            playerInfoPanel.remove(btnSpacer);

            playerInfoPanel.revalidate();
            playerInfoPanel.repaint();
        }

        feedbackBtnsActive = false;
    }

    public void addReportButtons() {
        reportBtnsActive = true;

        playerInfoPanel.add(reportBtnTitle);
        playerInfoPanel.add(reportBtn);
        playerInfoPanel.add(dontReportBtn);

        playerInfoPanel.revalidate();
        playerInfoPanel.repaint();
    }

    public void removeReportButtons() {
        if(reportBtnsActive == true) {
            playerInfoPanel.remove(reportBtnTitle);
            playerInfoPanel.remove(reportBtn);
            playerInfoPanel.remove(dontReportBtn);

            playerInfoPanel.revalidate();
            playerInfoPanel.repaint();
        }

        reportBtnsActive = false;
    }

    public void toggleAnonymousWarning() {
        if(config.enableAnonymousReporting()) {
            statsPanel.add(anonymousWarning);
        }else{
            statsPanel.remove(anonymousWarning);
        }

        statsPanel.revalidate();
        statsPanel.repaint();
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

        BotDetectorPlugin.http.getPlayerPrediction(sanitizedRSN, reportable);

    }

    public void updatePlayerStats() {
        numReports.setText("Reports Made: " + ps.getReports());
        numBans.setText("Confirmed Bans: " + ps.getBans());
        numPossibleBans.setText("Probable Bans: " + ps.getPossible_bans());
        accuracy.setText("Accuracy: " + ps.getAccuracy() + "%");
    }

    public void resetPlayerStats() {
        numReports.setText("Reports Made: ");
        numBans.setText("Confirmed Bans: ");
        numPossibleBans.setText("Probable Bans: ");
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
