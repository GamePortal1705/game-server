import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import game.GameContext;

/**
 * @author : Siyadong Xiong (sx225@cornell.edu)
 * @version : 3/18/17
 */
public class GameLauncher {
    public static void main(String[] args) throws InterruptedException {
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(3000);

        final SocketIOServer server = new SocketIOServer(config);

        GameContext game = new GameContext(2, server);
        game.startGame();
    }
}
