package game;

import java.util.UUID;

/**
 * Wolf is <b>Decorator</b> of Person
 *
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/18/17
 */
public class Wolf extends Person {
    private Person person;

    public Wolf(Person person) {
        this.person = person;
    }

    @Override
    public void state() {
        this.person.state();
    }

    @Override
    public UUID getSessionID() {
        return person.getSessionID();
    }

    @Override
    public int getId() {
        return person.getId();
    }

    @Override
    public int getRole() {
        return person.getRole();
    }

    @Override
    public String getPlayerName() {
        return person.getPlayerName();
    }

    @Deprecated
    public void kill(Person player) {

    }
}
