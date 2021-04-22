package com.botdetector.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;

public class BotDetectorPanel extends PluginPanel
{
	private static final int MAX_RSN_LENGTH = 12;
	private final Font boldFont = FontManager.getRunescapeBoldFont();

	private final IconTextField searchBar;
	private final JPanel linksPanel;
	private final JPanel reportingStatsPanel;
	private final JPanel primaryPredictionPanel;
	private final JPanel predictionBreakdownPanel;

	@Inject
	private EventBus eventBus;

	private Client client;

	private boolean searchBarLoading;

	@Inject
	public BotDetectorPanel(Client client)
	{
		this.client = client;

		searchBar = playerSearchBar();
		linksPanel = linksPanel();
		reportingStatsPanel = reportingStatsPanel();
		primaryPredictionPanel = primaryPredictionPanel();
		predictionBreakdownPanel = predictionBreakdownPanel();


		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
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

				eventBus.register(this);
			}
		});
	}

	private JPanel linksPanel()
	{
		JLabel title = new JLabel("Connect With Us: ");

		JLabel webIcon = new JLabel(Icons.WEB_ICON);
		webIcon.setToolTipText("Our Website");
		webIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				LinkBrowser.browse("https://www.osrsbotdetector.com/");
			}
		});

		JLabel githubIcon = new JLabel(Icons.GITHUB_ICON);
		githubIcon.setToolTipText("Check Out Our Souce Code");
		githubIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				LinkBrowser.browse("https://github.com/Bot-detector");
			}
		});

		JLabel discordIcon = new JLabel(Icons.DISCORD_ICON);
		discordIcon.setToolTipText("Join Our Discord!");
		discordIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				LinkBrowser.browse("https://discord.com/invite/JCAGpcjbfP");
			}
		});

		JLabel patreonIcon = new JLabel(Icons.PATREON_ICON);
		patreonIcon.setToolTipText("Help Keep Us Going");
		patreonIcon.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				LinkBrowser.browse("https://www.patreon.com/bot_detector");
			}
		});

		JPanel linksPanel = new JPanel();
		linksPanel.setBorder(new EmptyBorder(0, 6, 0, 0));
		linksPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		linksPanel.add(title);
		linksPanel.add(webIcon);
		linksPanel.add(discordIcon);
		linksPanel.add(githubIcon);
		linksPanel.add(patreonIcon);

		return linksPanel;
	}

	private JPanel reportingStatsPanel()
	{
		JLabel label;

		JPanel reportingStatsPanel = new JPanel();
		reportingStatsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		reportingStatsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		reportingStatsPanel.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;

		label = new JLabel(htmlLabel("white", "Reporting Statistics"));
		label.setFont(boldFont);
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = .5;
		constraints.weighty = 1;
		constraints.ipady = 5;
		reportingStatsPanel.add(label, constraints);

		label = new JLabel(htmlLabel("#a5a5a5", "Names Uploaded:"));
		constraints.gridy = 1;
		constraints.gridy++;
		constraints.ipady = 3;
		reportingStatsPanel.add(label, constraints);

		label = new JLabel(htmlLabel("#a5a5a5", "Reports Made:"));
		constraints.gridy++;
		reportingStatsPanel.add(label, constraints);

		label = new JLabel(htmlLabel("#a5a5a5", "Confirmed Bans:"));
		constraints.gridy++;
		reportingStatsPanel.add(label, constraints);

		label = new JLabel(htmlLabel("#a5a5a5", "Probable Bans:"));
		constraints.gridy++;
		reportingStatsPanel.add(label, constraints);

		return reportingStatsPanel;
	}

	private IconTextField playerSearchBar()
	{
		IconTextField searchBar = new IconTextField();
		searchBar.setIcon(IconTextField.Icon.SEARCH);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
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
		primaryPredictionPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		primaryPredictionPanel.setLayout(new GridBagLayout());
		primaryPredictionPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.HORIZONTAL;

		label = new JLabel(htmlLabel("white", "Primary Prediction"));
		label.setFont(boldFont);
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = .5;
		constraints.weighty = 1;
		constraints.ipady = 5;
		primaryPredictionPanel.add(label, constraints);

		label = new JLabel(htmlLabel("#a5a5a5", "Player Name:"));
		constraints.gridy++;
		constraints.ipady = 3;
		primaryPredictionPanel.add(label, constraints);

		label = new JLabel(htmlLabel("#a5a5a5", "Prediction:"));
		constraints.gridy++;
		primaryPredictionPanel.add(label, constraints);

		label = new JLabel(htmlLabel("#a5a5a5", "Confidence:"));
		constraints.gridy++;
		primaryPredictionPanel.add(label, constraints);

		return primaryPredictionPanel;
	}

	private JPanel predictionBreakdownPanel()
	{
		JLabel label;

		JPanel predictionBreakdownPanel = new JPanel();
		predictionBreakdownPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		predictionBreakdownPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		predictionBreakdownPanel.setLayout(new GridLayout(0, 1));

		label = new JLabel(htmlLabel("white", "Prediction Breakdown"));
		label.setFont(boldFont);
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

	private String htmlLabel(String color, String text)
	{
		return "<html><body style = 'color:" + color + "'>" +
			text +
			"</body></html>";
	}
}