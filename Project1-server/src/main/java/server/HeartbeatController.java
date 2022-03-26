package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import static server.Server.udpAnswerPing;

/**
 *
 * @author Miguel Dinis
 */
public class HeartbeatController extends Thread {

    private static DatagramSocket udpAnswerPing;
    private static int serverPortPing;

    public HeartbeatController(DatagramSocket udpAnswerPing, int serverPortPing) {
        this.udpAnswerPing = udpAnswerPing;
        this.serverPortPing = serverPortPing;
        System.out.println("WERWQEFWQEFWEFQ");

        try {
            udpAnswerPing.setSoTimeout(7500);
        } catch (SocketException ex) {
            
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.start();
    }

    @Override
    public void run() {

        while (true) {

            try {
                byte[] ping = {(byte) 0x1}; // 0x1 expected value on pings

                DatagramPacket incomingPing = new DatagramPacket(ping, ping.length);
                udpAnswerPing.receive(incomingPing);

                //System.out.println("Primary received ping: " + Arrays.toString(incomingPing.getData()));

                DatagramPacket reply = new DatagramPacket(incomingPing.getData(), incomingPing.getLength(), incomingPing.getAddress(), incomingPing.getPort());
                udpAnswerPing.send(reply);
                //System.out.println("Primary: resend ping");
            } catch (IOException ex) {
                System.out.println("WEWEWEFWEFQWEFQWEFQwqef");
                Logger.getLogger(HeartbeatController.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

}
