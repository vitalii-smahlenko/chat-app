package com.gmail.smaglenko.service;

import com.gmail.smaglenko.model.Message;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.websocket.EncodeException;
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
    private static final Set<ChatEndpoint> CHAT_ENDPOINTS = new CopyOnWriteArraySet<>();
    private static final HashMap<String, String> USERS = new HashMap<>();
    private Session session;

    @OnOpen
    public void onOpen(Session session, @PathParam("username") String username)
            throws IOException, EncodeException {

        this.session = session;
        CHAT_ENDPOINTS.add(this);
        USERS.put(session.getId(), username);

        Message message = new Message();
        message.setFrom(username);
        message.setContent("Connected!");
        broadcast(message);
    }

    @OnMessage
    public void onMessage(Session session, Message message) throws IOException, EncodeException {
        message.setFrom(USERS.get(session.getId()));
        if (message.getContent().contains("Change name")) {
            String[] splitContent = message.getContent().split(" ");
            String newUserMane = splitContent[3];
            message.setFrom(newUserMane);
        }
        broadcast(message);
    }

    @OnClose
    public void onClose(Session session) throws IOException, EncodeException {
        CHAT_ENDPOINTS.remove(this);
        Message message = new Message();
        message.setFrom(USERS.get(session.getId()));
        message.setContent("Disconnected!");
        broadcast(message);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
    }

    private static void broadcast(Message message) throws IOException, EncodeException {
        CHAT_ENDPOINTS.forEach(endpoint -> {
            synchronized (endpoint) {
                try {
                    endpoint.session.getBasicRemote()
                            .sendObject(message);
                } catch (IOException | EncodeException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
