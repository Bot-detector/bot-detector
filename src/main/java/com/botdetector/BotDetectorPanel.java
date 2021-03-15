package com.botdetector;

import com.google.inject.Inject;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import lombok.SneakyThrows;
import okhttp3.*;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.Notifier;
import net.runelite.client.events.SessionClose;
import net.runelite.client.events.SessionOpen;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.info.JRichTextPane;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import okhttp3.*;

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

    JLabel uploads;
    JLabel playerName;
    JLabel playerGroupID;

    @Inject
    public BotDetectorPanel(@Nullable Client client)
    {
        loading = false;

        statsPanel = new JPanel();
        playerInfoPanel =  new JPanel();

        uploads  = new JLabel(htmlLabel("Names Uploaded: ", "0", "#a5a5a5", "white"));
        playerName = new JLabel(htmlLabel("Player Name: ", "---", "#a5a5a5", "white"));
        playerGroupID = new JLabel(htmlLabel("Group ID: ", "---", "#a5a5a5", "white"));

        searchBar = new IconTextField();
        searchBar.setIcon(IconTextField.Icon.SEARCH);
        searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        searchBar.setMinimumSize(new Dimension(0, 30));
        searchBar.addActionListener(e -> {
            try {
                lookupPlayer();
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
                    lookupPlayer(localPlayer.getName());
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
        add(playerInfoPanel, constraints);
        constraints.gridy++;
        add(statsPanel, constraints);
        constraints.gridy++;

        playerInfoPanel.add(dataTitle);
        playerInfoPanel.add(playerName);
        playerInfoPanel.add(playerGroupID);

        statsPanel.add(statsTitle);
        statsPanel.add(uploads);

        eventBus.register(this);

    }

    private static String htmlLabel(String key, String value, String keyColor, String valueColor)
    {
        return "<html><body style = 'color:"+ keyColor + "'>" + key +
                "<span style = 'color:" + valueColor + "'>" + value +
                "</span></body></html>";
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

    public void lookupPlayer(String rsn) throws IOException {
        searchBar.setText(rsn);
        lookupPlayer();
    }

    private void lookupPlayer() throws IOException {
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

        getPlayerData(sanitizedRSN, BotDetectorPlugin.okClient);

    }

    public void getPlayerData(String rsn, OkHttpClient okClient) throws IOException {

        String url = "http://osrsbot-detector.ddns.net:8080/user/" +
                rsn.replace( " ", "%20");;

        Request request = new Request.Builder()
                .url(url)
                .build();

        Call call = okClient.newCall(request);
        call.enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("FAILURE! Could not locate player data.");
                notifier.notify("Could not locate player data.");

                updatePlayerData("Server Error", "---", true);

                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.isSuccessful()) {

                    String groupID = response.body().string();

                    if (groupID.equals("-1"))
                    {
                        updatePlayerData(rsn, "Indeterminable", true);
                    }
                    else
                    {
                        updatePlayerData(rsn, groupID, false);
                    }

                } else {
                    System.out.println("Bad Response. Could not locate player data.");
                    notifier.notify("Could not locate player data.");

                    updatePlayerData("Server Error", "---", true);

                    response.close();
                    call.cancel();
                }
            }
        });
    }

    private static String sanitizeText(String rsn)
    {
        return rsn.replace('\u00A0', ' ');
    }


}
