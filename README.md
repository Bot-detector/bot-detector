# Bot Detector Plugin

# DISCORD -  https://discord.com/invite/JCAGpcjbfP
--------------------------------------------------------------------------------------------------------

![image](https://user-images.githubusercontent.com/5789682/115799298-296f7100-a3a6-11eb-863d-3cf7e9d32234.png)

**Website**: https://www.osrsbotdetector.com/

**Patreon**: https://www.patreon.com/bot_detector

**GitHub**: https://github.com/Bot-detector/bot-detector

Identifies bots by sending nearby players' information to a third-party machine learning algorithm, trained with data provided by Jagex Moderators. 

--README--
1. The files within this repo are used in conjunction with https://github.com/Bot-detector/Bot-Detector-Core-Files. 

FAQ:

Q: "Why do you need my IP?"

A: I don't, and it's not stored anywhere. But we live in the year 2021 and computers have to talk to eachother so unfortunately your IP comes along for the ride with the Data.


Q: "This plugin is worthless"

A: Not really a question - but hopefully it'll help Jagex a little bit in solving their botting crisis - and maybe even repair the OSRS economy.


Q: "How can I help contribute to the plugin?"

A: "Fork and pull request, I'll approve if it's not malicious"


Q: "My bots got banned because of this plugin"

A: "Yay!"


Q: "Is this plugin malicious? How can I be sure that it's not malicious?"

A: The only part that connects to your RuneLite client is the RuneLite plugin which is available here: https://github.com/Bot-detector/bot-detector. The RuneLite developers won't allow anything that's even mildly suspect to enter the Plugin Hub - which is pretty great.


Q: "I still don't understand why you need to use a RuneLite client plugin to capture OSRS names, what's the point?"

A: If I could have access to the OSRS database for Hiscores - this would take far less time. However, the API for Jagex's Hiscore pulling system calls only every 1-3 seconds, which means it would take over 600 days to process every single name through the API. Basically, by the time OSRS 2 and RS4 came out we'd have only scratched the surface of processing the names into a usable format - nevermind even doing the math and other nonsense to detect who is a bot.


Q: "So, how do you detect who is a bot?"

A: Well, we could use a variety of different methods - the one that I chose was to group every player together that has similar stats, and if that group gets banned more frequently than other groups then it's probably likely that the rest of the group is pretty bot-like or suspicious. This could also include gold-farmers, RWTers, etc. Any group with a high ban rate is suspicious and would be reported directly to our resident Jagex Moderators.
