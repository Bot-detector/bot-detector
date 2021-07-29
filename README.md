# Bot Detector Plugin [![](https://img.shields.io/endpoint?url=https://i.pluginhub.info/shields/rank/plugin/bot-detector)](https://runelite.net/plugin-hub) [![](https://img.shields.io/endpoint?url=https://i.pluginhub.info/shields/installs/plugin/bot-detector)](https://runelite.net/plugin-hub)

Identifies bots by sending nearby players' information to a third-party machine learning algorithm, trained with data provided by Jagex Moderators.

![chart](https://user-images.githubusercontent.com/5789682/115799298-296f7100-a3a6-11eb-863d-3cf7e9d32234.png)

# Connect With Us!
| Site         | Link                                  |
|:-------------|:--------------------------------------|
| Discord      | https://discord.com/invite/JCAGpcjbfP |
| Our Website  | https://www.osrsbotdetector.com       |
| Patreon      | https://www.patreon.com/bot_detector  |
| GitHub       | https://github.com/Bot-detector       |

# Getting Started
## Installing the Plugin
1. Open RuneLite
2. Navigate to the Configuration wrench on the Top Right of the Sidebar.
3. Click on Plugin Hub at the very bottom.
4. Type in Bot Detector in the search bar.
5. Click Install
6. Accept the warning message. (See the FAQ below if you have questions about this)
7. Done!

## Using the Plugin
### The Plugin's Settings
| Setting | Description |
|:--------|:------------|
| Send Names Only After Logout | Recommended for players with low bandwidth. |
| Attempt Send on Close | Attempts to upload names when closing Runelite while being logged in. Take note that enabling this option may cause the client to take a long time to close if our servers are being unresponsive. |
| Send Names Every *x* mins | Determines how often the information collected by the plugin is flushed to our servers. The maximum rate is once per 5 minutes. | 
| Enable Chat Status Messages | Inserts chat messages in your chatbox to inform you about your uploads being sent. |
| '!bdstats' Chat Command Detail Level | Enable processing the '!bdstats' command when it appears in the chatbox, which will fetch the message author's plugin stats and display them. Disable to reduce spam. |
| Right-click 'Predict' Players | Allows you to right-click predict players, instead of having to type their name in the Plugin's panel manually. |
| 'Predict' on Right-click 'Report' | If you right-click Report someone via Jagex's official in-game report system, the player will be automatically predicted in the Plugin's Panel. |
| 'Predict' Copy Name to Clipboard | Copies the predicted player's name to your clipboard when right-click predicting. |
| Highlight 'Predict' Option | Highlights the 'Predict' option in red so that you can easily spot it on the in-game menu. |
| Prediction Autocomplete | Allows for prediction auto-completion for dialogue box entry.
| Show Feedback Textbox | Adds a textbox to the prediction feedback panel, so you can explain your choice in up to 250 characters. Make sure you type your feedback *before* you make your choice! |
| Panel Default Stats Tab | Sets panel default stats tab when the Plugin turns on. |
| Panel Font Size | Sets the font size of most of the Plugin's Panel elements. |
| Anonymous Uploading | When enabled, your username will not be sent with your uploads. You also cannot manually flag players. |

### ⚠️ Anonymous Mode ⚠️
**Anonymous Mode** is enabled by default, which means your username will not be sent with your uploads. However, we cannot tally your uploads, and you will not be able to manually flag players.

### Understanding The Plugin Panel
#### The Player Statistics Panel
![stats_panel](https://user-images.githubusercontent.com/45152844/123029711-88a22f80-d3af-11eb-92a9-99f7fde75506.png)
| Label           | Description |
|:----------------|:------------|
| Plugin Version  | Indicates the Bot Detector Plugin version you are currently running. |
| Current Uploads | Tallies the number of uploads the plugin has performed for the current RuneLite session. |
| Total Uploads   | Tallies the number of unique Names you've uploaded so far. |
| Feedback Sent   | Tallies the number of prediction feedbacks you've sent to us so far. |
| Possible Bans   | Tallies the number of unique Names you've uploaded that may have been banned. This is usually because a given Name stopped appearing on the official Hiscores. |
| Confirmed Bans  | Tallies the number of unique Names you've uploaded confirmed to have been banned by Jagex Moderators. |
| Incorrect Flags | **(Manual Tab Only)** Tallies the number of unique Names you've flagged as bots that ended up being confirmed as real players by Jagex Moderators. |
| Flag Accuracy   | **(Manual Tab Only)** Indicates your personal accuracy when manually flagging, determined by the previous two fields. |

**Note for ⚠️ Anonymous Users ⚠️**: Except for *Plugin Version* and *Current Uploads*, all of these fields are disabled.

#### The Prediction Panels
![prediction_panels](https://user-images.githubusercontent.com/45152844/123030745-424dd000-d3b1-11eb-8e62-28830a589a32.png)
| Panel                | Description |
|:---------------------|:------------|
| Primary Prediction   | Displays the primary prediction label and confidence for the predicted player. |
| Prediction Breakdown | Displays a breakdown of confidences for other labels our algorithm considered. These are not considered in our manual reviews. |
| Prediction Feedback  | Allows the user to send in a feedback for the **Primary Prediction**. |
| Manual Flagging      | Allows the user to upload the most recent sighting for the predicted player with an extra flag to tell us to pay more attention to that player. |

**Note for ⚠️ Anonymous Users ⚠️**: The *Manual Flagging* panel is disabled.

### Understanding How the Plugin Works
You, as a plugin user, automatically and passively send some information about every player you come across in-game to our servers.
The information sent, all collected at the time of the player spawning on screen, includes:
- The display name of the player
- The exact world location of the player
- The world number
- The exact time of the sighting
- The visibly equipped gear of the player
- The GE gear value of the player

We use this information to train one of our many machine learning models to identify patterns in accounts.
Example: Smithing bots are accounts which have a very high Smithing level, wear no gear, and are typically seen in Edgeville and at the Blast Furnace.
- We take their levels: High Smithing to Total ratio
- We take their locations: Edgeville & Blast Furnace
- We take their gear: Nothing
- We take their timestamp: Usually on between 16:00 UTC and 22:00 UTC
- Then we build a 'pattern' or label for those accounts. In this case, this would be labeled as `Smithing_bot`.

Note: We usually have to gather data for several weeks or months in order to get enough viable training data! That way our estimations are quite close to the actual status of the accounts we are attempting to evaluate.

After building enough accounts into a label, we then test the label on a variety of Machine Learning Models, and choose the best one to be used in evaluation. Here is an example output below:

![labels](https://user-images.githubusercontent.com/45152844/123032193-893cc500-d3b3-11eb-86b7-9e371c1734bd.png)

Once we are happy with these results, we will then output the labels to you whenever you use the Plugin to predict a player.

We will then await your feedback to see if our label is accurate enough to be used viably. If the label scores poorly, it will be pulled from the plugin and be reevaluated.

Once we are confident a given label performs well, we search our database for all accounts matching that label prediction with a very high confidence (typically above 90%) and send those names to Jagex for evaluation.

Jagex then lets us know if they're banned accounts or real players. If they're banned, they're marked as confirmed bans. If they're real players, they're marked as confirmed players. We then retain and reinforce the model from these confirmed ban/player marks.

# Frequently Asked Questions
## Is this Plugin malicious?
The Plugin can be installed through RuneLite's [Plugin Hub](https://runelite.net/plugin-hub) system, which is curated by the RuneLite developers to not allow anything that's even mildly suspect. If you have any doubt about the Plugin's inner workings, all of its components' source codes are available in this GitHub! You may also contact us through our Discord server if you have any questions you'd like answered.

## Why do I need a RuneLite Client/Plugin to capture OSRS names?
RuneLite's APIs allow us to easily collect nearby players' location, gear and gear cost information, all of which prove to be viable in detecting potential bots. While we could simply scrape the entirety of the Runescape Hiscores for "bot-like" accounts, it would be quite inefficient. The combined information of active player names along with the aforementioned collected data gives our system much more power, especially for differentiating between bots and alt accounts.

## When I try to install the Plugin, I get a warning about it needing my IP address. What is this about?
That is simply how the Internet works. As the Plugin needs to connect to our servers to send and receive data, your IP comes along for the ride. We do not log your IP address and **Anonymous Mode** is enabled by default, which means that we will not receive your Runescape Name with the data you send. You may however turn off the option yourself in the Plugin's settings to access extra features.

## I installed the Plugin, and it works, but it's not tallying my uploads? I also can't manually flag players as bots!
**Anonymous Mode** is enabled by default. If it is enabled, we cannot associate your uploads with your Runescape Name. If you wish to tally your uploads, you'll need to disable Anonymous Mode in the Plugin's settings. In addition, we've disabled manual flagging for Anonymous users.

## I changed my Runescape Name and my tallies got reset!
The only way we can easily attribute uploads is through the sender's Runescape Name. If you have a way of confirming to us your previous name, come talk to us on our Discord server where we may be able to transfer your tallies. If you've used our account linking feature on our Discord with your previous name, this process should be even easier for us.

## How does Bot Detector identifies potential bots?
We use several machine learning algorithms, and variations within, in order to identify potential botters and legitimate players. We do realise that sometimes we can get it wrong, but thankfully by your feedback, as well as Jagex's, we are able to improve over time!

## How accurate is Bot Detector at identifying bots?
We use player feedback to measure the perceived accuracy of our systems in addition to Jagex's confirming bots and real players for us. Lately (as of June 22nd 2021), we have increased our accuracy from 80% to 90% in a short period of time, as shown below.

![feedback](https://user-images.githubusercontent.com/45152844/123023792-07926a80-d3a6-11eb-8851-6ceefc2568a2.png)

## How can I contribute to the project?
There are several ways you can contribute to the Bot Detector Project:
- Install and use this Plugin: As we increase our number of users, we are able to better train our model and detect bots, thanks to the extra data we can collect.
- Donate to our Patreon: All that data has to go somewhere! All money donated through our Patreon goes towards our server upkeep.
- Join the Development Team: Come join us on our Discord server and ask an admin for the **New Developer** rank, we'll get you set up!

## Do I have to manually predict and flag players for them to be sent to the Bot Detector server?
No, the plugin will grab the data for every player that renders on your screen passively. Manually flagging a player uploads the same data as the passive collection does, but tells us to pay more attention to that data when training our models.

## Does this replace manual in-game reporting?
No, reporting players in-game sends a ton of data which we cannot collect directly to Jagex and we urge you to continue using that feature whenever you are confident someone is botting! Also, please keep in mind that even if our plugin predicts a player as a bot, it does not mean it is true! Investigate the players yourself before making your decision to report. Please be kind to these players and do not falsely accuse or harrass them based upon a prediction that could be faulty.

## I checked myself with the Plugin and my Primary Prediction says I'm a bot! Will I get banned?
No, at least, not by that alone. We take into account not only the Machine Learning's response but also review the names manually prior to sending to Jagex. All we send over are the names of suspected bot accounts and are treated no differently than any other user would whenever submitting leads to tipoff@jagex.com in Jagex's decision process. If you know you didn't bot, you have nothing to worry about.

## My Primary Prediction says I'm a Real Player, but it isn't 100% confident and includes several bot labels in the breakdown. What is going on?
The model's ability to evaluate a prediction must take into account a variety of factors. Stats, gear, location, and more are used to determine a predictive score. It is very unlikely, and uncommon, to be rated as a 100% Real Player. Usually if the primary prediction label is at any Real Player percent, the account will not show up in our manual reviews.

## Can you ban accounts?
No, as said before, all we do is send over lists of suspicious accounts as you would through tipoff@jagex.com.

## Can you unban accounts?
No, for that you will have to file an appeal with [Jagex](http://jgx.game/Ban).

## I've seen some of these cool heatmaps, how can I generate more of them?
Generating a heatmap is currently only available to our Patrons. You could ask one to generate a map for you, but we do ask for your consideration in supporting us on our Patreon. If you do, you will have access to an exclusive channel on our Discord server where you can execute the heatmap command at any time. Our Patrons are what make this all possible! 

## This FAQ did not answer my questions.
Come talk to us on our Discord server, look for the **Plugin Issues** and **Plugin Discussion** channels.

# Thanks for your continued support! ❤️ The Bot Detector Team ❤️
![wyvern_extreme_pogging][w_e_p]![wyvern_extreme_pogging][w_e_p]![wyvern_extreme_pogging][w_e_p]![wyvern_extreme_pogging][w_e_p]![wyvern_extreme_pogging][w_e_p]

[w_e_p]: https://user-images.githubusercontent.com/45152844/116952387-8ac1fa80-ac58-11eb-8a31-5fe0fc6f5f88.gif
