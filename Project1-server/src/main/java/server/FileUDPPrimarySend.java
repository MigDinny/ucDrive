package server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public byte[] calculateMD5Checksum(String path) {

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");

            try ( InputStream is = Files.newInputStream(Paths.get("home/" + path));  DigestInputStream dis = new DigestInputStream(is, md)) {
                /* Read decorated stream (dis) to EOF as normal... */
            } catch (IOException ex) {
                Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
            }

            return md.digest();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        return new byte[16];
    }

    @Override
    public void run() {

        while (true) {
            // check if empty queue
            if (queueToSend.isEmpty()) {
                try {
                    Thread.sleep(5000);
                    continue;
                } catch (InterruptedException ex) {
                    Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            System.out.println("Sending: " + queueToSend.peek());

            String fileToSend = queueToSend.remove();

            // fetch file from system
            File f = new File("home/" + fileToSend);

            if (!f.exists()) {
                System.err.println("Error sending file from primary to secondary: file does not exist.");
                continue;
            }
            
            // calculate file md5 checksum
            byte[] checksum = calculateMD5Checksum(fileToSend);
            System.out.println(checksum.toString());
            

            // send file over above socket
            byte[] buffer = new byte[(int) f.length()];

            BufferedInputStream bis;
            try {

                // send number of bytes to read in a 4-byte buffer
                byte[] len = ByteBuffer.allocate(4).putInt((int) f.length()).array();
                DatagramPacket lenPacket = new DatagramPacket(len, len.length, this.secondaryLocationAddress, this.secondaryPort);
                this.udpSendSocket.send(lenPacket);

                // send path
                byte[] pathBytes = new byte[1024];
                pathBytes = fileToSend.getBytes();
                DatagramPacket pathPacket = new DatagramPacket(pathBytes, pathBytes.length, this.secondaryLocationAddress, this.secondaryPort);
                this.udpSendSocket.send(pathPacket);

                // send file
                bis = new BufferedInputStream(new FileInputStream(f));
                bis.read(buffer, 0, buffer.length);

                int bytesSent = 0;
                DatagramPacket filePacket;
                int lenToRead = 1024;
                while (bytesSent < buffer.length) {
                    lenToRead = 1024;
                    if (buffer.length - bytesSent < 1024) {
                        lenToRead = buffer.length - bytesSent;
                    }

                    filePacket = new DatagramPacket(buffer, bytesSent, lenToRead, this.secondaryLocationAddress, this.secondaryPort);

                    bytesSent += 1024;
                    System.out.println("BYTES SENT: " + Integer.toString(bytesSent));

                    this.udpSendSocket.send(filePacket);
                }

                // wait for ACK
                // if error, retry
                //queueToSend.add(fileToSend);
                // if timeout on ACK is reached, ... idk
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

}
