package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;


/*
Heartbeat Controller class.
It opens a thread running in an infinite loop waiting for pings to answer them.
This is useful for the client and primary server so they can be aware about the primary server status.
*/
public class HeartbeatController extends Thread {

    private static DatagramSocket udpAnswerPing;
    
    /*
    Heartbeat constructor. Takes a socket to receive pings.
    */
    public HeartbeatController(DatagramSocket udpAnswerPing) throws SocketException {
        this.udpAnswerPing = udpAnswerPing;

        udpAnswerPing.setSoTimeout(7500);

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
                Logger.getLogger(HeartbeatController.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

}
