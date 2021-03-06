# Bot Detector Plugin (V1 - SNAPSHOT)

WARNING: THERE MIGHT BE LAG IN SENDING LARGE FILE SIZES DEPENDING ON THE STRENGTH OF YOUR MACHINE. This is basically Early Access for the program, so do not manually send names in dangerous situations. 

NOTICE: WE ARE CURRENTLY IN THE DATA-COLLECTION PHASE IN ORDER TO TRAIN THE FIRST VERSION OF THE KNN NETWORK. CURRENTLY WE'LL NEED 100K UNIQUE NAMES AT LEAST :) Thank you for sending names to the server everyone!

--------------------------------------------------------------------------------------------------------

This plugin pulls surrounding Player Names from Old School RuneScape. It then sends those names to a Python server that looks up those names on the hiscores - and runs a neural network analysis on the returned player stats to analyze bot-like behavior. Bot-like players are then separately reported to Jagex. 

--HOW TO USE--
1. Turn on the Plugin
2. Run around and collect player names automatically
3. Turn off the plugin - or allow for automatic sending in Bot-Detector > Config
4. Have notifications on in RuneLite > Notification Settings: Enable Tray Notifications & Game Message Notifications in order to know when your names have sent!

--------------------------------------------------------------------------------------------------------

Planned Changes: 
- Improve/Add a UI

Roadmap For Future Updates:
- Allow for players to recieve updates on the names that they have identified/come across in game.
- Highlight players that have already been scanned in-game
- Mark player group conditions
- Allow for access to a 3rd Party Website end of the plugin for processed data
- Add a 'Player Lookup' feature
