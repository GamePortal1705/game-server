package io;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/18/17
 */
public class DispatchRoleMsg {
    private int role;
    private int numOfPlayers;

    public DispatchRoleMsg(int role, int numOfPlayers) {
        this.role = role;
        this.numOfPlayers = numOfPlayers;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public int getNumOfPlayers() {
        return numOfPlayers;
    }

    public void setNumOfPlayers(int numOfPlayers) {
        this.numOfPlayers = numOfPlayers;
    }
}
