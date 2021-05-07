// Really simple server
// If you need other examples check out:
// https://github.com/websockets/ws
// to install necessary lib, run: 
// npm install ws
const WebSocket = require('ws');

const portN = 8080

// Creating server which listens on port: 8080
const wss = new WebSocket.Server({ port: portN });

console.log("server opened on port: " + portN.toString())

wss.on('connection', function connection(ws) {
  console.log("connected");
  ws.on('message', function incoming(message) {
    msg = JSON.parse(message);
    console.log('received: %s', msg.message);

    //we echo back the message
    ws.send(JSON.stringify({"message": "I am the server, I received: " + msg.message}))
  });

  ws.onclose = function(){
    console.log("Connection closed");
  }

  ws.send(JSON.stringify({"message": "I am the server, Connection opened"}))
});