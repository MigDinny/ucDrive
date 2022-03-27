package server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Stack;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Miguel
 * @coauthor Edgar
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

    
    private static byte[] generateMD5Checksum(String filename) throws Exception {
        InputStream fis = new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    private static String getMD5Checksum(String filename) throws Exception {
        byte[] b = generateMD5Checksum(filename);
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
    
    private static String getMD5Checksum(byte[] checksumBytes) throws Exception {
        byte[] b = checksumBytes;
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    
    @Override
    public void run() {

        while (true) {
            System.out.println(queueToSend);

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

                // calculate file md5 checksum
                String generatedChecksum = getMD5Checksum(fileToSend);
                System.out.println("CHECKSUM: " + generatedChecksum);

                this.udpSendSocket.setSoTimeout(5000);

                // wait for ACK (in the form of CHECKSUM)
                byte[] receivedChecksum = new byte[16];
                DatagramPacket ackPacket = new DatagramPacket(receivedChecksum, receivedChecksum.length);

                // if error, retry
                this.udpSendSocket.receive(ackPacket);

                String checksumString = getMD5Checksum(ackPacket.getData());

                //means there was an error with the packet
                if (!checksumString.equals(generatedChecksum)) {
                    System.err.println("ERRO CHECKSUM: original = " + generatedChecksum + " ||| recebida = " + checksumString);
                    queueToSend.add(fileToSend);
                } else {

                }

                //Just in case!
                this.udpSendSocket.setSoTimeout(0);
                // if error, retry  
                //queueToSend.add(fileToSend);

                // if timeout on ACK is reached, ... idk
            } catch (SocketTimeoutException ex) {
                //@TO-DO. IF ACK IS NOT RECEIVED!!
                System.out.println("TO-DO");
                Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(FileUDPPrimarySend.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

}
