package server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Miguel
 */
public class FileUDPPrimarySend extends Thread {

    private final int secondaryPort; // secondary server port to send files
    private final String secondaryLocation; // secondary server location
    private InetAddress secondaryLocationAddress; // secondary server location in InetAddress form
    private DatagramSocket udpSendSocket;
    public Queue<String> queueToSend;
    

    public FileUDPPrimarySend(String secondaryLocation, int port) {

        this.secondaryPort = port;
        this.secondaryLocation = secondaryLocation;
        this.queueToSend = new LinkedList<>();
        
        try {
            this.secondaryLocationAddress = InetAddress.getByName(this.secondaryLocation);            
        } catch (UnknownHostException ex) {
            Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
        }
        
               
        try {
            this.udpSendSocket = new DatagramSocket();
        } catch (SocketException ex) {
            Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.start();
    }

    @Override
    public void run() {

        while (true) {
            System.out.println("Sending: " + queueToSend.peek());

            String fileToSend = queueToSend.remove();

            // fetch file from system
            File f = new File(fileToSend);

            if (!f.exists()) {
                System.err.println("Error sending file from primary to secondary: file does not exist.");
                continue;
            }
            
            

            // send file over above socket
            byte[] buffer = new byte[(int) f.length()];

            BufferedInputStream bis;
            try {
                bis = new BufferedInputStream(new FileInputStream(f));
                bis.read(buffer, 0, buffer.length);

                DatagramPacket filePacket = new DatagramPacket(buffer, buffer.length, this.secondaryLocationAddress, this.secondaryPort);
                
                this.udpSendSocket.send(filePacket);
                
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
            }
            

            // use socket to send buffered file
            if (queueToSend.isEmpty()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }

}
