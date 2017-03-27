package io;

import java.util.UUID;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/18/17
 */
public class Message<T> {
    private Integer id;
    private String  playerName;
    private UUID sessionId;
    private T       data;


    public Message() {
    }

    public Message(Integer id, String playerName, UUID sessionId, T data) {
        this.id = id;
        this.playerName = playerName;
        this.sessionId = sessionId;
        this.data = data;
    }

    public static <T> Message<T> createBroadcastMessage(T data) {
        return new Message<>(-1, null, null, data);
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
