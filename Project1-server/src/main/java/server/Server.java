package server;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.parser.ParseException;

/*
    Main class Server.

    Initializes the server and accepts incoming client connections.
 */
public class Server {

    private static int serverPort = 6000; // primary server
    private static int serverPortSecondary = 6001; // secondary server
    private static final int serverPortPing = 6002; // answer ping port primary
    private static final int udpSecondaryPortFileTransfer = 6003; // secondary port open for file transfers
    private static String udpSecondaryLocation = "localhost";
    private static final String folderName = "home";
    private static final String folderNameSecondary = "home2";

    public static DatagramSocket udpAnswerPing;
    public static DatagramSocket udpSecondarySendPing;
    private static Scanner sc;

    // initializes the server, makes sure homedir exists
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

    // run primary method -> this is called by the primary server
    private static void runPrimary() throws IOException, ParseException {

        byte[] ping = {(byte) 0x1}; // 0x1 expected value on pings

        // open UDP socket in order to answer pings. answer one ping before beginning running primary
        // create thread to answer pings
        System.out.println("Primary Server console port (6000): ");
        serverPort = Integer.parseInt(sc.nextLine());

        System.out.println("Secondary Server location (localhost): ");
        udpSecondaryLocation = sc.nextLine();

        udpAnswerPing = new DatagramSocket(serverPortPing);

        DatagramPacket incomingPing = new DatagramPacket(ping, ping.length);
        udpAnswerPing.receive(incomingPing);

        System.out.println("Primary received ping: " + Arrays.toString(incomingPing.getData()));

        DatagramPacket reply = new DatagramPacket(incomingPing.getData(), incomingPing.getLength(), incomingPing.getAddress(), incomingPing.getPort());
        udpAnswerPing.send(reply);

        // open thread to read queue once in T seconds and send the files waiting in queue
        Queue<String> fileQueue = new FileUDPPrimarySend(udpSecondaryLocation, udpSecondaryPortFileTransfer).queueToSend;

        // heartbeat controller: answers every ping 
        new HeartbeatController(udpAnswerPing);
        // accept incoming connections and create threads for them
        int n_thread = 0;

        Config confF = new Config();

        ServerSocket listenSocket = new ServerSocket(serverPort);

        while (true) {
            Socket clientSocket = listenSocket.accept();
            n_thread++;
            new Connection(clientSocket, n_thread, fileQueue, confF, folderName);
        }

    }

    // run primary through secondary when primary fails -> this is called by the secondary server to act as a primary one
    private static void runPrimaryThroughSecondary() throws IOException, ParseException {

        // accept incoming connections and create threads for them
        int n_thread = 0;

        Config confF = new Config();

        ServerSocket listenSocket = new ServerSocket(serverPortSecondary);

        while (true) {
            Socket clientSocket = listenSocket.accept();
            n_thread++;
            new Connection(clientSocket, n_thread, confF, folderNameSecondary);
        }

    }

    // run secondary -> this is called by the secondary
    private static void runSecondary() throws InterruptedException, IOException, ParseException {

        byte[] ping = {(byte) 0x1}; // 0x1 expected value on pings
        DatagramPacket pingPacket;

        // @TODO ask for primary server location
        System.out.println("Primary Server Location (localhost): ");
        String location = sc.nextLine();
        System.out.println("Secondary Server client console port (6001): ");
        serverPortSecondary = Integer.parseInt(sc.nextLine());

        // open thread to receive file copies Primary > Secondary
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

    /*
    Main method. Initializes the server.
     */
    public static void main(String[] args) {

        if (!init()) {
            System.err.println("Error initializing the server.");
        }

        System.out.println("Role: 0 - Servidor Primário || 1 - Servidor Secundário");

        sc = new Scanner(System.in);

        int op = Integer.parseInt(sc.nextLine());

        try {
            switch (op) {
                case 0 -> // servidor primário
                    runPrimary();
                case 1 -> // servidor secundário
                    runSecondary();
                default ->
                    System.out.println("Wrong value.");

            }
        } catch (IOException | InterruptedException | ParseException e) {
            Logger.getLogger(Server.class
                    .getName()).log(Level.SEVERE, null, e);
        }
    }

}
