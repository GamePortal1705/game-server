package game;

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

    public void kill(Person player) {
        //TODO
    }
}
