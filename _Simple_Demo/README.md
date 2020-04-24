# Simple_Scarlet_WebSocket
Guide on how to set up an app using Scarlet. \
It's much easier to read than the demo app. It could be a good starting point for understanding the library.
## Requirements
To follow this guide you need to know: 
- The basics of ViewModel (check out my guide: https://github.com/lolloz98/Android_ViewModel) 
- The basics of databinding (https://developer.android.com/topic/libraries/data-binding) 
- (Very basics of the navigation component) 
- install nodejs on your computer https://nodejs.org/it/download/ 
- Create a ngrok account and install the program on your computer (https://dashboard.ngrok.com/signup)
## Description
This is a really simple app: we create a connection with our own server and we send and receive messages after having established a connection.
![](/img/Global_understanding.png)
## Setup server
In \_Simple\_Demo folder you find simple_server. \
Using nodejs you can launch the script server.js: 
```node server.js``` \
Remember to install the dependecies: 
```npm install ws``` \
If you open a command window you can use: 
```
ngrok http 8080
```
Now you have access to your computer from the web. You will use the http address given but, you will have to switch protocol (you just need to change http:// with wss://). \
Your server is good to go :)
## Setup Android app
Before starting our app remember to change the BASE_URL (with: wss://your_ngrok_http.ngrok.io) in network.MyWebSocketAPI file. \
If you don't do that, the app won't be able to connect to the server.
## Important notes
Bonus: I use Timber to log messages. https://github.com/JakeWharton/timber
## References
https://github.com/Tinder/Scarlet
