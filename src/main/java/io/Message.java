package io;

import java.util.UUID;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/18/17
 */
public class Message<T> {
    private String  playerName;
    private Integer id;
    private UUID sessionId;
    private T       data;


    public Message() {
    }

    public Message(String playerName, Integer id, UUID sessionId, T data) {
        this.playerName = playerName;
        this.id = id;
        this.sessionId = sessionId;
        this.data = data;
    }

    public static <T> Message<T> createBroadcastMessage(T data) {
        return new Message<>(null, -1, null, data);
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
}
