package game;

import java.util.UUID;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/18/17
 */
public class Person {

    // TODO
    /* use Role enum to support more roles */
    enum Role {
        FOLK(0),
        WOLF(1),
        FORESEER(3);

        private int val;
        Role(int val) {
            this.val = val;
        }
        int getVal() {
            return val;
        }

        @Override
        public String toString() {
            switch (val) {
                case 0: return "Folk";
                case 1: return "Wolf";
                case 2: return "Foreseer";
                default: return "";
            }
        }
    }

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append("id: ").append(id).append(", ");
        sb.append("playerName: ").append(playerName).append(", ");
        sb.append("sessionID: ").append(sessionID).append(", ");
        sb.append("role: ").append(role == FOLK ? "Villager" : "Wolf").append(", ");
        sb.append("status: ").append(status == ALIVE ? "alive" : "dead").append(']');
        return sb.toString();
    }
}
