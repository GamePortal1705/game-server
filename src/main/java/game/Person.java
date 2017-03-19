package game;

import java.util.UUID;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/18/17
 */
public class Person {
    private static final int FOLK = 0;
    private static final int WOLF = 0;

    private int id;
    private UUID sessionID;

    private int role;

    public Person() {
    }

    public Person(int id, UUID sessionID) {
        this.id = id;
        this.sessionID = sessionID;
        this.role = FOLK;
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

    public Wolf decorateToWolf() {
        this.role = WOLF;
        return new Wolf(this);
    }

    public void state() {
    }
}
