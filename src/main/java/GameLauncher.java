import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import game.GameContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/18/17
 */
public class GameLauncher {

    private static Logger logger = LoggerFactory.getLogger(GameLauncher.class);

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) throw new IllegalArgumentException();
        int N = Integer.parseInt(args[0]);

        logger.info("Number of players: " + N);

        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(3000);

        final SocketIOServer server = new SocketIOServer(config);

        Signal.handle(new Signal("INT"), signal -> {
            logger.info("Stopped by external SIGNAL");
            server.stop();
            System.exit(-1);
        });

        GameContext game = new GameContext(N, server);
        game.startGame();
    }
}
