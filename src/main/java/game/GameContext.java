package game;

import com.corundumstudio.socketio.SocketIOServer;
import io.DispatchRoleMsg;
import io.JoinGameMsg;
import io.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/18/17
 */
public class GameContext {
    private static final int SLEEP_TIME = 500;

    private final SocketIOServer server;
    private final List<Person>   players;
    private final List<Wolf>     wolves;

    private final Set<UUID> sessionIDSet;
    private final Map<UUID, Person> sessionIdToPlayer;

    private Logger logger = LoggerFactory.getLogger(GameContext.class);

    private Status status;

    private int maxNumOfPerson;
    private int numOfPerson;
    private int numOfAliveFolk;
    private int numOfAliveWolf;

    private KillCounter killCounter;

    public GameContext(int maxNumOfPerson, final SocketIOServer server) {
        this.numOfPerson = 0;
        this.maxNumOfPerson = maxNumOfPerson;
        this.server = server;
        this.players = new ArrayList<>(maxNumOfPerson);
        this.wolves = new ArrayList<>();
        this.sessionIDSet = new HashSet<>();
        this.sessionIdToPlayer = new HashMap<>();
        this.killCounter = KillCounter.createInstance();

        this.status = Status.Init;
    }

    private void dispatchRoles() {
        int numOfWolves = maxNumOfPerson / 4;

        if (numOfWolves == 0) numOfWolves = 1;
        Collections.shuffle(this.players);
        for (int i = 0; i < numOfWolves; i++) {
            this.wolves.add(players.get(i).decorateToWolf());
        }
        (this.players).sort(Comparator.comparingInt(Person::getId));

//        int _villager = 0;
//        int _wolf = 1;
//        this.wolves.add(players.get(_wolf).decorateToWolf());


        for (Person p : this.players) {
            this.server.getClient(p.getSessionID())
                       .sendEvent("dispatchRole",
                                  new Message<>(p.getId(), p.getPlayerName(),
                                                p.getSessionID(),
                                                new DispatchRoleMsg(p.getRole(), this.numOfPerson,
                                                                    p.getRole() == Person.FOLK ? "villager" : "wolf")));

            logger.info("current id " + p.getId() + "; role " + (p.getRole() == 0 ? "Villager" : "Wolf"));
        }

        this.numOfAliveWolf = numOfWolves;
        this.numOfAliveFolk = numOfPerson - numOfWolves;
    }

    public void startGame() throws InterruptedException {
        this.server.start();

        this.status = Status.Wait;

        this.server.addEventListener("joinGame", Message.class, (client, data, ackSender) -> {
            synchronized (this.players) {
                if (!sessionIDSet.contains(client.getSessionId()) &&
                    this.players.size() < maxNumOfPerson) {

                    Person p = new Person(numOfPerson++,            // ID
                                          client.getSessionId(),    // session ID
                                          data.getPlayerName());    // player name
                    this.players.add(p);
                    this.sessionIDSet.add(client.getSessionId());
                    this.sessionIdToPlayer.put(client.getSessionId(), p);

                    logger.info("Player Info " + p.toString());

                    client.sendEvent("joinGame", new Message<>(p.getId(), data.getPlayerName(),
                                                               client.getSessionId(), new JoinGameMsg(true)));
                } else {
                    client.sendEvent("joinGame", new Message<>(null, data.getPlayerName(),
                                                               client.getSessionId(), new JoinGameMsg(false)));
                }
            }
        });

        // wait until enough users to start the game
        while (numOfPerson < maxNumOfPerson) {
            TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
        }

        server.removeAllListeners("joinGame");
        this.status = Status.Dispatching;
        dispatchRoles();


        int round = 0;

        while (!isGameOver()) {

            round++;

            sendSystemMessage("Round: " + round + "Time: " + this.status.msg);

            /* ----    Night: Wolves will choose a player to kill    ------*/
            this.status = Status.Night;
            logger.info("Bind listener on who to kill");
            addKillListener();
            logger.info("Ask wolves to kill people and villagers to wait");
            server.getBroadcastOperations()
                  .sendEvent("night", Message.createBroadcastMessage("round: " + round));
//            for (Wolf wolf : this.wolves) {
//                server.getClient(wolf.getSessionID())
//                      .sendEvent("night", new Message<>(wolf.getId(), wolf.getPlayerName(), wolf.getSessionID(), "round: " + round));
//            }
//
//            logger.info("Ask villagers to wait");
//            for (Person person : this.players) {
//                if (person.getRole() != Person.WOLF) {
//                    server.getClient(person.getSessionID())
//                          .sendEvent("night", new Message<>(person.getId(), person.getPlayerName(),
//                                                            person.getSessionID(), "round: " + round));
//                }
//            }

            while (killCounter.size() < numOfAliveWolf) {
                TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
            }
            int killed = decideWhoToKill();
            removeKillListener();
            logger.info("[Round " + round + "] Player " + killed + " is killed by wolves");
            /* ----    Night End     ------*/


            /* ----    Daytime: notifying who's dead
                                --> make statement in turn
                                --> vote a player to death
             */
            this.status = Status.Notifying;
            sendSystemMessage("[Round " + round + "] Time: " + this.status.msg);
            server.getBroadcastOperations()
                  .sendEvent("killDecision", Message.createBroadcastMessage(killed));


            this.status = Status.Stating;
            sendSystemMessage("[Round " + round + "] Time: " + this.status.msg);

            Deque<Integer> playersFinishedStatement = new ArrayDeque<>();
            server.addEventListener("finishStatement", Message.class,
                                    (client, data, ackSender) -> {
                int finishedID = data.getId();
                synchronized (playersFinishedStatement) {
                    playersFinishedStatement.offer(finishedID);
                }
            });
            for (Person player : players) {
                if (player.isAlive()) {
                    // broadcast to all players who is making statement
                    Message<String> stateMsg = Message.createBroadcastMessage("round: " + round);
                    stateMsg.setId(player.getId());
                    stateMsg.setPlayerName(player.getPlayerName());
                    stateMsg.setSessionId(player.getSessionID());
                    server.getBroadcastOperations().sendEvent("makeStatement", stateMsg);

                    // wait until current person finish making statement
                    while (playersFinishedStatement.isEmpty() ||
                           playersFinishedStatement.peekLast() != player.getId()) {
                        TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
                    }
                }
            }


            this.status = Status.Voting;
            sendSystemMessage("Round: " + round + "Time: " + this.status.msg);

            addKillListener();
            server.getBroadcastOperations()
                  .sendEvent("vote", Message.createBroadcastMessage(null));
            while (killCounter.size() != numOfAliveWolf + numOfAliveFolk) {
                TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
            }
            killed = decideWhoToKill();
            logger.info("[Round " + round + "] Player " + killed + " is killed by vote");
            server.getBroadcastOperations()
                  .sendEvent("killDecision", Message.createBroadcastMessage(killed));
            removeKillListener();

            logger.info("[Round " + round + "] Round ends");
        }   // end of while loop

        this.status = Status.End;
        sendSystemMessage("After " + round + ", " + this.status.msg);
        onGameOver();
    }

    private void addKillListener() {
        server.addEventListener("kill", Message.class, (client, data, ackSender) -> {
            Message<Integer> msg = (Message<Integer>) data;
            int toKillId = msg.getData();

            logger.info("Client " + sessionIdToPlayer.get(client.getSessionId()).getId() +
                        " choose to kill id: " + toKillId);

            killCounter.addID(toKillId);
        });
    }

    private int decideWhoToKill() {
        int id = killCounter.playerToKill();
        Person p = this.players.get(id);
        p.setStatus(Person.DEAD);
        if (p.getRole() == Person.FOLK) {
            numOfAliveFolk--;
        } else {
            numOfAliveWolf--;
        }

        logger.info("Killed id " + id);
        logger.info("Number of alive wolves " + numOfAliveWolf);
        logger.info("Number of alive villagers " + numOfAliveFolk);

        return id;
    }

    private void removeKillListener() {
        server.removeAllListeners("kill");
        killCounter.clear();
    }


    private void sendSystemMessage(String sysInfo) {
        server.getBroadcastOperations()
              .sendEvent("systemInfo", Message.createBroadcastMessage(sysInfo));
    }

    private boolean isGameOver() {
        return numOfAliveFolk == 0 || numOfAliveWolf == 0;
    }

    private void onGameOver() {
        this.server.stop();
    }

    private enum Status {
        Init("Initializing game"),
        Wait("Waiting for other players"),
        Dispatching("Dispatching roles"),
        Night("Night"),
        Notifying("Notifying"),
        Stating("Players make statements"),
        Voting("Players vote one player to death"),
        End("Game over");

        private String msg;

        Status(String msg) {
            this.msg = msg;
        }
    }
}
