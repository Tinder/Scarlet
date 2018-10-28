/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.socketio

import com.corundumstudio.socketio.AckCallback
import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.VoidAckCallback
import com.corundumstudio.socketio.listener.DataListener
import org.junit.Test

class SocketIoClientTest {

    @Test
    fun test() {
        val config = Configuration()
        config.setHostname("localhost")
        config.setPort(9092)

        val server = SocketIOServer(config)
        server.addEventListener(
            "ackevent1",
            String::class.java,
            object : DataListener<ChatObject>() {
                fun onData(client: SocketIOClient, data: ChatObject, ackRequest: AckRequest) {

                    // check is ack requested by client,
                    // but it's not required check
                    if (ackRequest.isAckRequested()) {
                        // send ack response with data to client
                        ackRequest.sendAckData("client message was delivered to server!", "yeah!")
                    }

                    // send message back to client with ack callback WITH data
                    val ackChatObjectData = ChatObject(data.getUserName(), "message with ack data")
                    client.sendEvent("ackevent2", object : AckCallback<String>(String::class.java) {
                        fun onSuccess(result: String) {
                            System.out.println("ack from client: " + client.getSessionId() + " data: " + result)
                        }
                    }, ackChatObjectData)

                    val ackChatObjectData1 = ChatObject(data.getUserName(), "message with void ack")
                    client.sendEvent("ackevent3", object : VoidAckCallback() {

                        protected fun onSuccess() {
                            System.out.println("void ack from: " + client.getSessionId())
                        }

                    }, ackChatObjectData1)
                }
            })

        server.start()

        Thread.sleep(Integer.MAX_VALUE.toLong())

        server.stop()

    }
}