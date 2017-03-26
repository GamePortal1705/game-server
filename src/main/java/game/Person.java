package game;

import java.util.UUID;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/18/17
 */
public class Person {
    static final int FOLK = 0;
    static final int WOLF = 1;

    static final int ALIVE = 0;
    static final int DEAD  = 1;

    private String playerName;
    private int    id;
    private UUID   sessionID;
    private int    status;

    private int role;

    public Person() {
    }

    public Person(int id, UUID sessionID, String playerName) {
        this.id = id;
        this.sessionID = sessionID;
        this.playerName = playerName;
        this.role = FOLK;
        this.status = ALIVE;
    }

    public UUID getSessionID() {
        return sessionID;
    }

    public int getId() {
        return id;
    }

    public int getRole() {
        return role;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isAlive() {
        return this.status == Person.ALIVE;
    }

    public Wolf decorateToWolf() {
        this.role = WOLF;
        return new Wolf(this);
    }

    public void state() {
    }
}
