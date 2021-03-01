# Bot Detector Plugin
This plugin pulls surrounding Player Names from Old School RuneScape. It then sends those names to a Python server that looks up those names on the hiscores - and runs a neural network analysis on the returned player stats to analyze bot-like behavior. Bot-like players are then separately reported to Jagex. 

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
