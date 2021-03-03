# Bot Detector Plugin (V1 - SNAPSHOT)

WARNING: THERE MIGHT BE LAG IN SENDING LARGE FILE SIZES DEPENDING ON THE STRENGTH OF YOUR MACHINE. This is basically Early Access for the program, so do not manually send names in dangerous situations. 

This plugin pulls surrounding Player Names from Old School RuneScape. It then sends those names to a Python server that looks up those names on the hiscores - and runs a neural network analysis on the returned player stats to analyze bot-like behavior. Bot-like players are then separately reported to Jagex. 

--HOW TO USE--
1. Turn on the Plugin
2. Run around and collect player names automatically
3. Turn off the Plugin (This sends the player name list to the server)
4. Restart RuneLite in order to send a new list and repeat from step 1. (This will be changed soon)

--------------------------------------------------------------------------------------------------------

Planned Changes: 
- Improve/Add a UI
- Add an automatic player-name sending opt-in feature
- Allow for multiple name list sends without having to restart RuneLite.
- Let the player know when names have been sent/failed to send by a toggle Overlay.

Roadmap For Future Updates:
- Allow for players to recieve updates on the names that they have identified/come across in game.
- Highlight players that have already been scanned in-game
- Mark player group conditions
- Allow for access to a 3rd Party Website end of the plugin for processed data
- Add a 'Player Lookup' feature
