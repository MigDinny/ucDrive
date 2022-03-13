package server;

/**
 *
 * @author Edgar Duarte
 * @author Miguel Dinis
 */
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

//Creates a connection for each client that tries to connect to the server
public class Server {

    private static int serverPort = 6000;
    private static String folderName = "home";

    private static boolean init() {
        
        File folder = new File(folderName);
        if (!folder.exists()) {
            if (folder.mkdir()) {
                System.out.println("Folder " + folderName + " created.");
                return true;
            } else return false;
        }
        
        return true;
    }
    
    public static void main(String[] args) {

        if (!init()) {
            System.err.println("Erro ao inicializar o servidor.");
        }
        

        int n_thread = 0;

        Config confF = new Config();

        try (ServerSocket listenSocket = new ServerSocket(serverPort)) {

            while (true) {
                Socket clientSocket = listenSocket.accept();
                n_thread++;
                new Connection(clientSocket, n_thread, confF, folderName);
            }

        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }

    }

}
