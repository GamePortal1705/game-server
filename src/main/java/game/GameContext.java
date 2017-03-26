package game;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.BroadcastAckCallback;
import com.corundumstudio.socketio.SocketIOClient;
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
                       .sendEvent("dispatchRole", new Message<>(p.getPlayerName(), p.getId(),
                                                                p.getSessionID(),
                                                                new DispatchRoleMsg(p.getRole(), this.numOfPerson)));
        }

        this.numOfAliveWolf = numOfWolves;
        this.numOfAliveFolk = numOfPerson - numOfWolves;
    }

    public void startGame() throws InterruptedException {
        this.server.start();

        this.status = Status.Wait;

        this.server.addEventListener("joinGame", Message.class, ((client, data, ackSender) -> {
            synchronized (this.players) {
                if (this.players.size() < maxNumOfPerson) {
                    Person p = new Person(numOfPerson++,            // ID
                                          client.getSessionId(),    // session ID
                                          data.getPlayerName());    // player name
                    this.players.add(p);
                    logger.info("Player number: " + numOfPerson);
                    client.sendEvent("joinGame", new Message<>(data.getPlayerName(), p.getId(),
                                                               client.getSessionId(), new JoinGameMsg(true)));
                } else {
                    client.sendEvent("joinGame", new Message<>(data.getPlayerName(), null,
                                                               client.getSessionId(), new JoinGameMsg(false)));
                }
            }
        }));

        // wait until enough users to start the game
        while (numOfPerson < maxNumOfPerson) {
            TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
        }

        server.removeAllListeners("joinGame");
        this.status = Status.Dispatching;
        dispatchRoles();


        //TODO night and daytime logic
        int round = 0;
        List<Integer> toKillIds = new ArrayList<>();
        Map<Integer, Integer> killCounter = new HashMap<>();
        while (!isGameOver()) {
            round++;

            sendSystemMessage("Round: " + round + "Time: " + this.status.msg);

            this.status = Status.Night;

            toKillIds.clear();
            killCounter.clear();
            logger.info("Ask wolves to kill people");
            for (Wolf wolf : this.wolves) {
                server.getClient(wolf.getSessionID())
                      .sendEvent("night", new AckCallback<Integer>(Integer.class) {
                          @Override
                          public void onSuccess(Integer result) {
                              toKillIds.add(result);
                              killCounter.put(result, killCounter.getOrDefault(result, 0) + 1);
                          }
                      }, new Message<>(wolf.getPlayerName(), wolf.getId(),
                                       wolf.getSessionID(), round));
            }

            logger.info("Ask villagers to wait");
            for (Person person : this.players) {
                if (person.getRole() != Person.WOLF) {
                    this.server.getClient(person.getSessionID())
                               .sendEvent("night", new Message<>(person.getPlayerName(), person.getId(),
                                                                 person.getSessionID(), round));
                }
            }

            while (toKillIds.size() < numOfAliveWolf) {
                TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
            }

            int killed = killByVote(killCounter);
            logger.info("ID: " + killed + " is killed by wolves");

            this.status = Status.Notifying;
            sendSystemMessage("Round: " + round + "Time: " + this.status.msg);
            // assume all clients will receive kill decision message
            this.server.getBroadcastOperations()
                       .sendEvent("killDecision", Message.createBroadcastMessage(killed));

            this.status = Status.Stating;
            sendSystemMessage("Round: " + round + "Time: " + this.status.msg);

            Deque<Integer> playersFinishedStatement = new ArrayDeque<>();
            for (Person person : players) {
                if (person.isAlive()) {
                    server.getClient(person.getSessionID())
                          .sendEvent("makeStatement", new AckCallback<Boolean>(Boolean.class, 60 + 20) {
                              @Override
                              public void onSuccess(Boolean result) {
                                  if (result) {
                                      playersFinishedStatement.offer(person.getId());
                                  }
                              }

                              @Override
                              public void onTimeout() {
                                  playersFinishedStatement.offer(person.getId());
                              }
                          }, new Message<String>(person.getPlayerName(), person.getId(), person.getSessionID(), null));

                    Message<String> tmpMsg = Message.createBroadcastMessage(null);
                    tmpMsg.setPlayerName(person.getPlayerName());
                    server.getBroadcastOperations().sendEvent("makeStatement", server.getClient(person.getSessionID()), tmpMsg);
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
            server.getBroadcastOperations()
                  .sendEvent("vote",
                             Message.createBroadcastMessage(null),
                             new BroadcastAckCallback<Integer>(Integer.class) {
                                 @Override
                                 public void onClientSuccess(SocketIOClient client, Integer result) {
                                     logger.info("sessionID: " + client.getSessionId() + " killed " + result);
                                     toKillIds.add(result);
                                     killCounter.put(result, killCounter.getOrDefault(result, 0) + 1);
                                 }
                             });
            while (toKillIds.size() != numOfAliveWolf + numOfAliveFolk) {
                TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
            }
            killed = killByVote(killCounter);
            logger.info("ID: " + killed + " is killed by vote");
            server.getBroadcastOperations()
                  .sendEvent("killDecision", Message.createBroadcastMessage(killed));

        }   // end of while loop

        this.status = Status.End;
        sendSystemMessage("After " + round + ", " + this.status.msg);
        onGameOver();
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

    public void onGameOver() {
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
