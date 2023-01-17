package com.gmail.smaglenko.service;

import com.gmail.smaglenko.model.Message;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/chat/{username}", decoders = MessageDecoder.class,
        encoders = MessageEncoder.class)
public class ChatEndpoint {
    private final Set<ChatEndpoint> chatEndpoints = new CopyOnWriteArraySet<>();
    private final HashMap<String, String> users = new HashMap<>();
    private Session session;

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username) {
        this.session = session;
        chatEndpoints.add(this);
        users.put(session.getId(), username);
        Message message = new Message();
        message.setUser(username);
        message.setContent("Connected!");
        broadcast(message);
    }

    @OnMessage
    public void onMessage(Session session, Message message) {
        message.setUser(users.get(session.getId()));
        if (message.getContent().contains("Change name")) {
            String[] splitContent = message.getContent().split(" ");
            String newUserMane = splitContent[3];
            message.setUser(newUserMane);
        }
        broadcast(message);
    }

    @OnClose
    public void onClose(Session session) {
        chatEndpoints.remove(this);
        Message message = new Message();
        message.setUser(users.get(session.getId()));
        message.setContent("Disconnected!");
        broadcast(message);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
    }

    private void broadcast(Message message) {
        chatEndpoints.forEach(endpoint -> {
            synchronized (endpoint) {
                try {
                    endpoint.session.getBasicRemote()
                            .sendObject(message);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
