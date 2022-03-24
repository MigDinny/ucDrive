package server;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Miguel
 */
public class FileUDPPrimarySend extends Thread {
    
    private final int port;
    private DatagramSocket udpSendSocket;
    public Queue<String> queueToSend;
    
    
    public FileUDPPrimarySend(int port) {
        
        this.port = port;
        this.queueToSend = new LinkedList<>();
        
        try {
            this.udpSendSocket = new DatagramSocket(port);
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
