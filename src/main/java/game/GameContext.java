package game;

import com.corundumstudio.socketio.SocketIOServer;
import io.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/18/17
 */
public class GameContext {
    private final SocketIOServer server;
    private final List<Person>   players;
    private final List<Wolf> wolves;

    private Logger logger = LoggerFactory.getLogger(GameContext.class);

    private Status status;

    private int maxNumOfPerson;
    private int numOfPerson;

    public GameContext(int maxNumOfPerson, final SocketIOServer server) {
        this.numOfPerson = 0;
        this.maxNumOfPerson = maxNumOfPerson;
        this.server = server;
        this.players = new ArrayList<>(maxNumOfPerson);
        this.wolves = new ArrayList<>();

        this.status = Status.INIT;
    }

    private void dispatchRoles() {
        int numOfWolves = maxNumOfPerson / 4;
        Collections.shuffle(this.players);
        for (int i = 0; i < numOfWolves; i++) {
            this.wolves.add(players.get(i).decorateToWolf());
        }
        (this.players).sort(Comparator.comparingInt(Person::getId));

        for (Person p : this.players) {
            this.server.getClient(p.getSessionID())
                       .sendEvent("dispatchRole", p.getRole());
        }
    }

    public void startGame() throws InterruptedException {
        this.server.start();

        this.status = Status.WAIT;

        this.server.addEventListener("joinGame", Message.class, ((client, data, ackSender) -> {
            synchronized (this.players) {
                if (this.players.size() < maxNumOfPerson) {
                    this.players.add(new Person(numOfPerson++, client.getSessionId()));
                    logger.info("Player number: " + numOfPerson);
                    ackSender.sendAckData(Boolean.TRUE);
                } else {
                    ackSender.sendAckData(Boolean.FALSE);
                }
            }
        }));

        // wait until enough users to start the game
        while (numOfPerson < maxNumOfPerson) {
            TimeUnit.MILLISECONDS.sleep(500);
        }

        this.server.removeAllListeners("joinGame");
        this.status = Status.DISPATCHING;
        dispatchRoles();


        //TODO night and daytime logic


    }

    public void stopGame() {
        this.server.stop();
    }

    private enum Status {
        INIT("Initializing game"),
        WAIT("Waiting for other players"),
        DISPATCHING("Dispatching roles"),
        NIGHT("Wolves are killing folks"),
        STATING("Players make statements"),
        VOTING("Players vote to kill someone"),
        END("Game over");

        private String msg;

        Status(String msg) {
            this.msg = msg;
        }
    }
}
