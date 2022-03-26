package server;

/**
 *
 * @author Edgar Duarte
 * @author Miguel Dinis!
 */
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

//Creates a connection for each client that tries to connect to the server
public class Server {

    private static int serverPort = 6000; // primary server
    private static int serverPortSecondary = 6001; // secondary server
    private static int serverPortPing = 6002;
    private static int udpSecondaryPortFileTransfer = 6003;
    private static String udpSecondaryLocation = "localhost";
    private static String folderName = "home";
    private static String folderNameSecondary = "home2";
    private static Scanner sc;

    public static DatagramSocket udpAnswerPing;
    public static DatagramSocket udpSecondarySendPing;

    private static boolean init() {

        File folder = new File(folderName);
        if (!folder.exists()) {
            if (folder.mkdir()) {
                System.out.println("Folder " + folderName + " created.");
                return true;
            } else {
                return false;
            }
        }

        return true;
    }

    private static void runPrimary() {

        byte[] ping = {(byte) 0x1}; // 0x1 expected value on pings

        // open UDP socket in order to answer pings. answer one ping before beginning running primary
        // create thread to answer pings
        try {
            System.out.println("What port is secondary server in");
            int port_secondary = Integer.parseInt(sc.nextLine());
            
            

            udpAnswerPing = new DatagramSocket(serverPortPing);

            DatagramPacket incomingPing = new DatagramPacket(ping, ping.length);
            udpAnswerPing.receive(incomingPing);

            System.out.println("Primary received ping: " + Arrays.toString(incomingPing.getData()));

            DatagramPacket reply = new DatagramPacket(incomingPing.getData(), incomingPing.getLength(), incomingPing.getAddress(), incomingPing.getPort());
            udpAnswerPing.send(reply);

        } catch (SocketException ex) {
            System.out.println("EFWErf");
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            System.out.println("EFWErf2");
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, e);
        }
        
        // open thread to read queue once in T seconds and send the files waiting in queue
        //Queue<String> fileQueue = new FileUDPPrimarySend(udpSecondaryLocation, udpSecondaryPortFileTransfer).queueToSend;
        
        
        // heartbeat controller: answers every ping 

        new HeartbeatController(udpAnswerPing, serverPortPing);
        // accept incoming connections and create threads for them
        int n_thread = 0;

        Config confF = new Config();

        try ( ServerSocket listenSocket = new ServerSocket(serverPort)) {

            while (true) {
                Socket clientSocket = listenSocket.accept();
                n_thread++;
                new Connection(clientSocket, n_thread, confF, folderName);
            }

        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }

    }
    
    private static void runPrimaryThroughSecondary() {
        
        // accept incoming connections and create threads for them
        int n_thread = 0;

        Config confF = new Config();

        try ( ServerSocket listenSocket = new ServerSocket(serverPortSecondary)) {

            while (true) {
                Socket clientSocket = listenSocket.accept();
                n_thread++;
                new Connection(clientSocket, n_thread, confF, folderNameSecondary);
            }

        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }
        
    }

    private static void runSecondary() throws InterruptedException, IOException {

        byte[] ping = {(byte) 0x1}; // 0x1 expected value on pings
        DatagramPacket pingPacket;

        // @TODO ask for primary server location
        System.out.println("What is the primary location");
        String location = sc.nextLine();
        System.out.println("What port is primary server in");
        int port_primary = Integer.parseInt(sc.nextLine());
        

        
        // @TODO create socket to receive files, create associated Thread (which save files on home2/ )
        
        new FileUDPSecondaryReceive(udpSecondaryPortFileTransfer);
        
        
        // heartbeat to the primary server, catching SocketTimeoutException when timeout is reached.
        udpSecondarySendPing = new DatagramSocket();
        udpSecondarySendPing.setSoTimeout(2500);

        InetAddress host = InetAddress.getByName(location);
        pingPacket = new DatagramPacket(ping, ping.length, host, serverPortPing);

        while (true) {
            try {

                udpSecondarySendPing.send(pingPacket);
                System.out.println("Secondary: Ping sent!");

                DatagramPacket reply = new DatagramPacket(ping, ping.length);
                udpSecondarySendPing.receive(reply);
                System.out.println("Secondary: ping round-trip received!");

            } catch (SocketTimeoutException e) {
               
                // socket timeout 
                System.out.println("Heartbeat failed! Take control of the primary server.");
                
                // kill FileReceiveThread, and close socket
                
                // take control
                runPrimaryThroughSecondary();
                
                break;
                
            }

            Thread.sleep(2500);
        }

    }

    public static void main(String[] args) {

        if (!init()) {
            System.err.println("Erro ao inicializar o servidor.");
        }

        System.out.println("Role: 0 - Servidor Primário || 1 - Servidor Secundário");

        sc = new Scanner(System.in);

        int op = Integer.parseInt(sc.nextLine());

        if (op == 0) {
            // servidor primário
            runPrimary();
        } else if (op == 1){
            try {
                // servidor secundário
                runSecondary();
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else{
            System.out.println("Wrong value given");
        }

    }

}
