package com.botdetector.ui;

import com.botdetector.BotDetectorPlugin;
import com.botdetector.http.BotDetectorClient;
import com.google.common.collect.ImmutableList;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;

public class BotDetectorPanel extends PluginPanel
{
	@Getter
	@AllArgsConstructor
	private static enum WebLink
	{
		WEBSITE(Icons.WEB_ICON, "Our website", "https://www.osrsbotdetector.com/"),
		DISCORD(Icons.DISCORD_ICON, "Join our Discord!", "https://discord.com/invite/JCAGpcjbfP"),
		GITHUB(Icons.GITHUB_ICON, "Check out the project's source code", "https://github.com/Bot-detector"),
		PATREON(Icons.PATREON_ICON, "Help keep us going!", "https://www.patreon.com/bot_detector")
		;

		private final ImageIcon image;
		private final String tooltip;
		private final String link;
	}

	private static final int MAX_RSN_LENGTH = 12;
	private static final Font BOLD_FONT = FontManager.getRunescapeBoldFont();
	private static final Font NORMAL_FONT = FontManager.getRunescapeFont();
	private static final Font SMALL_FONT = FontManager.getRunescapeSmallFont();

	private static final Color BACKGROUND_COLOR = ColorScheme.DARK_GRAY_COLOR;
	private static final Color SUB_BACKGROUND_COLOR = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color LINK_HEADER_COLOR = ColorScheme.LIGHT_GRAY_COLOR;
	private static final Color HEADER_COLOR = Color.WHITE;
	private static final Color TEXT_COLOR = ColorScheme.LIGHT_GRAY_COLOR;

	private static final List<WebLink> LINKS = ImmutableList.of(
		WebLink.WEBSITE,
		WebLink.DISCORD,
		WebLink.GITHUB,
		WebLink.PATREON);

	private final IconTextField searchBar;
	private final JPanel linksPanel;
	private final JPanel reportingStatsPanel;
	private final JPanel primaryPredictionPanel;
	private final JPanel predictionBreakdownPanel;

	private final Client client;
	private final BotDetectorPlugin plugin;
	private final BotDetectorClient detectorClient;

	private boolean searchBarLoading;

	@Inject
	public BotDetectorPanel(@Nullable Client client, BotDetectorPlugin plugin, BotDetectorClient detectorClient)
	{
		this.client = client;
		this.plugin = plugin;
		this.detectorClient = detectorClient;

		setBorder(new EmptyBorder(18, 10, 0, 10));
		setBackground(BACKGROUND_COLOR);
		setLayout(new GridBagLayout());

		searchBar = playerSearchBar();
		linksPanel = linksPanel();
		reportingStatsPanel = reportingStatsPanel();
		primaryPredictionPanel = primaryPredictionPanel();
		predictionBreakdownPanel = predictionBreakdownPanel();

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1;
		constraints.weighty = 0;
		constraints.insets = new Insets(0, 0, 10, 0);

		add(linksPanel, constraints);
		constraints.gridy++;
		add(reportingStatsPanel, constraints);
		constraints.gridy++;
		add(searchBar, constraints);
		constraints.gridy++;
		add(primaryPredictionPanel, constraints);
		constraints.gridy++;
		add(predictionBreakdownPanel, constraints);
		constraints.gridy++;
	}

	private JPanel linksPanel()
	{
		JPanel linksPanel = new JPanel();
		linksPanel.setBorder(new EmptyBorder(0, 6, 0, 0));
		linksPanel.setBackground(SUB_BACKGROUND_COLOR);

		JLabel title = new JLabel("Connect With Us: ");
		title.setForeground(LINK_HEADER_COLOR);
		title.setFont(NORMAL_FONT);

		linksPanel.add(title);

		for (WebLink w : LINKS)
		{
			JLabel link = new JLabel(w.getImage());
			link.setToolTipText(w.getTooltip());
			link.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					LinkBrowser.browse(w.getLink());
				}
			});

			linksPanel.add(link);
		}

		return linksPanel;
	}

	private JPanel reportingStatsPanel()
	{
		JLabel label;

		JPanel reportingStatsPanel = new JPanel();
		reportingStatsPanel.setBackground(SUB_BACKGROUND_COLOR);
		reportingStatsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		reportingStatsPanel.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;

		label = new JLabel("Reporting Statistics");
		label.setFont(BOLD_FONT);
		label.setForeground(HEADER_COLOR);

		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = .5;
		constraints.weighty = 1;
		constraints.ipady = 5;
		reportingStatsPanel.add(label, constraints);

		label = new JLabel("Names Uploaded:");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);

		constraints.gridy = 1;
		constraints.gridy++;
		constraints.ipady = 3;
		reportingStatsPanel.add(label, constraints);

		label = new JLabel("Reports Made:");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		constraints.gridy++;
		reportingStatsPanel.add(label, constraints);

		label = new JLabel("Confirmed Bans:");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		constraints.gridy++;
		reportingStatsPanel.add(label, constraints);

		label = new JLabel("Probable Bans:");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		constraints.gridy++;
		reportingStatsPanel.add(label, constraints);

		return reportingStatsPanel;
	}

	private IconTextField playerSearchBar()
	{
		IconTextField searchBar = new IconTextField();
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchBar.setBackground(SUB_BACKGROUND_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		searchBar.setMinimumSize(new Dimension(0, 30));
		searchBar.addActionListener(e -> lookupPlayer());
		searchBar.addMouseListener(new MouseAdapter()
		{
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
			searchBarLoading = false;
		});

		return searchBar;
	}

	private JPanel primaryPredictionPanel()
	{
		JLabel label;

		JPanel primaryPredictionPanel = new JPanel();
		primaryPredictionPanel.setBackground(SUB_BACKGROUND_COLOR);
		primaryPredictionPanel.setLayout(new GridBagLayout());
		primaryPredictionPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;

		label = new JLabel("Primary Prediction");
		label.setFont(BOLD_FONT);
		label.setForeground(HEADER_COLOR);
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = .5;
		constraints.weighty = 1;
		constraints.ipady = 5;
		primaryPredictionPanel.add(label, constraints);

		label = new JLabel("Player Name:");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		constraints.gridy++;
		constraints.ipady = 3;
		primaryPredictionPanel.add(label, constraints);

		label = new JLabel("Prediction:");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		constraints.gridy++;
		primaryPredictionPanel.add(label, constraints);

		label = new JLabel("Confidence:");
		label.setFont(SMALL_FONT);
		label.setForeground(TEXT_COLOR);
		constraints.gridy++;
		primaryPredictionPanel.add(label, constraints);

		return primaryPredictionPanel;
	}

	private JPanel predictionBreakdownPanel()
	{
		JLabel label;

		JPanel predictionBreakdownPanel = new JPanel();
		predictionBreakdownPanel.setBackground(SUB_BACKGROUND_COLOR);
		predictionBreakdownPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		predictionBreakdownPanel.setLayout(new GridLayout(0, 1));

		label = new JLabel("Prediction Breakdown");
		label.setFont(BOLD_FONT);
		label.setForeground(HEADER_COLOR);
		predictionBreakdownPanel.add(label);

		return predictionBreakdownPanel;
	}

	private void updateReportStatsPanel()
	{
		//TODO Update contribution stats after names upload/manual report.
	}

	private void updatePredictionPanel()
	{
		//TODO Add prediction upon prediction receipt.
	}

	private void updatePredictionBreakdownPanel()
	{
		//TODO Add breakdown upon prediction receipt.
	}

	public void lookupPlayer(String rsn)
	{
		searchBar.setText(rsn);
		lookupPlayer();
	}

	private void lookupPlayer()
	{
		String sanitizedRSN = Text.sanitize(searchBar.getText());

		if (sanitizedRSN.length() <= 0)
		{
			return;
		}

		if (sanitizedRSN.length() > MAX_RSN_LENGTH)
		{
			searchBar.setIcon(IconTextField.Icon.ERROR);
			searchBarLoading = false;
			return;
		}

		searchBar.setIcon(IconTextField.Icon.LOADING_DARKER);
		searchBar.setEditable(false);
		searchBarLoading = true;

		//TODO Trigger prediction lookup here
	}

	// TODO: Change this to return a color object
	private String getPredictionColor(String pred_conf)
	{
		double conf = Double.parseDouble(pred_conf);

		if (conf >= .8)
		{
			return "green";
		}
		else if (conf >= .60)
		{
			return "orange";
		}
		else
		{
			return "red";
		}
	}
}