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

    public GameContext(int maxNumOfPerson, final SocketIOServer server) {
        this.numOfPerson = 0;
        this.maxNumOfPerson = maxNumOfPerson;
        this.server = server;
        this.players = new ArrayList<>(maxNumOfPerson);
        this.wolves = new ArrayList<>();
        this.sessionIDSet = new HashSet<>();
        this.sessionIdToPlayer = new HashMap<>();

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

        for (Person p : this.players) {
            this.server.getClient(p.getSessionID())
                       .sendEvent("dispatchRole", new Message<>(p.getId(), p.getPlayerName(),
                                                                p.getSessionID(),
                                                                new DispatchRoleMsg(p.getRole(), this.numOfPerson)));
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
        List<Integer> toKillIds = new ArrayList<>();
        Map<Integer, Integer> killCounter = new HashMap<>();
        while (!isGameOver()) {
            round++;

            sendSystemMessage("Round: " + round + "Time: " + this.status.msg);

            this.status = Status.Night;

            toKillIds.clear();
            killCounter.clear();



            logger.info("Bind listener on who to kill");
            addKillListener(toKillIds, killCounter);
            logger.info("Ask wolves to kill people");
            for (Wolf wolf : this.wolves) {
                server.getClient(wolf.getSessionID())
                      .sendEvent("night", new Message<>(wolf.getId(), wolf.getPlayerName(), wolf.getSessionID(), round));
            }

            logger.info("Ask villagers to wait");
            for (Person person : this.players) {
                if (person.getRole() != Person.WOLF) {
                    server.getClient(person.getSessionID())
                          .sendEvent("night", new Message<>(person.getId(), person.getPlayerName(),
                                                            person.getSessionID(), round));
                }
            }

            while (toKillIds.size() < numOfAliveWolf) {
                TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
            }
            int killed = killByVote(killCounter);
            removeKillListener();
            logger.info("ID: " + killed + " is killed by wolves at round " + round);


            this.status = Status.Notifying;
            sendSystemMessage("Round: " + round + "Time: " + this.status.msg);
            server.getBroadcastOperations()
                  .sendEvent("killDecision", Message.createBroadcastMessage(killed));


            this.status = Status.Stating;
            sendSystemMessage("Round: " + round + "Time: " + this.status.msg);

            Deque<Integer> playersFinishedStatement = new ArrayDeque<>();
            server.addEventListener("finishStatement", Message.class, (client, data, ackSender) -> {
                int finishedID = data.getId();
                playersFinishedStatement.offer(finishedID);
            });
            for (Person person : players) {
                if (person.isAlive()) {
                    // send message to who will make statement
                    server.getClient(person.getSessionID())
                          .sendEvent("makeStatement", new Message<String>(person.getId(),
                                                                          person.getPlayerName(),
                                                                          person.getSessionID(),
                                                                          null));

                    // notify others who is making statement
                    Message<String> tmpMsg = Message.createBroadcastMessage(null);
                    tmpMsg.setPlayerName(person.getPlayerName());
                    tmpMsg.setId(person.getId());
                    server.getBroadcastOperations().sendEvent("makeStatement", server.getClient(person.getSessionID()), tmpMsg);


                    // wait until current person finish making statement
                    while (playersFinishedStatement.isEmpty() ||
                           playersFinishedStatement.peekLast() != person.getId()) {
                        TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
                    }
                }
            }


            this.status = Status.Voting;
            sendSystemMessage("Round: " + round + "Time: " + this.status.msg);

            toKillIds.clear();
            killCounter.clear();
            addKillListener(toKillIds, killCounter);
            server.getBroadcastOperations()
                  .sendEvent("vote", Message.createBroadcastMessage(null));
            while (toKillIds.size() != numOfAliveWolf + numOfAliveFolk) {
                TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
            }
            killed = killByVote(killCounter);
            logger.info("ID: " + killed + " is killed by vote");
            server.getBroadcastOperations()
                  .sendEvent("killDecision", Message.createBroadcastMessage(killed));
            removeKillListener();
        }   // end of while loop

        this.status = Status.End;
        sendSystemMessage("After " + round + ", " + this.status.msg);
        onGameOver();
    }

    private void addKillListener(List<Integer> toKillIds, Map<Integer, Integer> killCounter) {
        server.addEventListener("kill", Message.class, (client, data, ackSender) -> {
            Message<Integer> msg = (Message<Integer>) data;
            int toKillId = msg.getData();

            logger.info("clientID " + sessionIdToPlayer.get(client.getSessionId()).getId() +
                        "choose to kill id: " + toKillId);

            toKillIds.add(toKillId);
            killCounter.put(toKillId, killCounter.getOrDefault(toKillId, 0) + 1);
        });
    }

    private void removeKillListener() {
        server.removeAllListeners("kill");
    }


    private void sendSystemMessage(String sysInfo) {
        server.getBroadcastOperations()
              .sendEvent("systemInfo", Message.createBroadcastMessage(sysInfo));
    }

    private int killByVote(Map<Integer, Integer> killCounter) {
        int max = -1;
        int id = -1;
        for (Map.Entry<Integer, Integer> entry : killCounter.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                id = entry.getKey();
            }
        }
        Person p = this.players.get(id);
        p.setStatus(Person.DEAD);
        if (p.getRole() == Person.FOLK) {
            numOfAliveFolk--;
        } else {
            numOfAliveWolf--;
        }
        return id;
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
