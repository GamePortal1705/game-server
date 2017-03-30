package io;

import game.Person;

import java.util.List;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/30/17
 */
public class GameOverMsg {
    private List<Person> players;
    private int winner;

    public GameOverMsg(List<Person> players, int winner) {
        this.players = players;
        this.winner = winner;
    }

    public List<Person> getPlayers() {
        return players;
    }

    public void setPlayers(List<Person> players) {
        this.players = players;
    }

    public int getWinner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }
}
