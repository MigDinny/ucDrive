package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.InetAddress;
import java.security.MessageDigest;

/*
This class runs a thread to receive files from primary server. 

It also calculates the checksum to send to the primary server so it can be checked.
*/
public class FileUDPSecondaryReceive extends Thread {

    private DatagramSocket udpFileSocket;

    /*
    Constructor
    
    Takes a port to listen on.
    */
    public FileUDPSecondaryReceive(int serverPort) throws InterruptedException, IOException {

        this.udpFileSocket = new DatagramSocket(serverPort);

        this.start();
    }

    // generates MD5 hash (checksum) given a filename, returning a bytearray
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

    // generates MD5 hash (checksum) given a filename, returning a string
    private static String getMD5Checksum(String filename) throws Exception {
        
        byte[] b = generateMD5Checksum(filename);
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }


    // infinite loop to receive incoming files
    public void run() {

        do {
            int bytesRead = 0;
            int fileLen = 0;
            String filePath;
            byte[] lenBytes = new byte[4];
            byte[] pathBytes = new byte[1024];
            int incPort = 0;
            InetAddress incAddress = null;

            DatagramPacket lenBytesPacket = new DatagramPacket(lenBytes, 4);
            DatagramPacket pathBytesPacket = new DatagramPacket(pathBytes, 1024);

            try {
                
                this.udpFileSocket.setSoTimeout(0); // reset timeout
                this.udpFileSocket.receive(lenBytesPacket); // receive length of the file -- this receive lasts forever until one packet arrives and starts the loop
                this.udpFileSocket.setSoTimeout(1000); // set timeout to 1 second because if any .receive() timeouts, the loop MUST BE RESETTED
                
                this.udpFileSocket.receive(pathBytesPacket); // receive path
                
                
                incPort = lenBytesPacket.getPort();
                incAddress = lenBytesPacket.getAddress();
                
                fileLen = ByteBuffer.wrap(lenBytes).getInt();
                filePath = new String(pathBytes, StandardCharsets.UTF_8);
                String filePathTrimmed = filePath.trim();
                
                System.out.println("LENGTH: " + Integer.toString(fileLen));
                System.out.println("PATH: " + filePath);
                
                String[] arr = filePath.split("/");
                String new_dir = "home2";
                
                for(int i = 0; i < arr.length - 1; i++){
                    new_dir += arr[i] + "/";
                    File dir= new File(new_dir);
                    dir.mkdir();
                }
                
                System.out.println(new_dir);
                

                byte[] fileBytes = new byte[fileLen + 1024];
                byte[] buffer = new byte[1024];
                DatagramPacket filePacket = new DatagramPacket(buffer, 1024);

                
                while (bytesRead < fileLen) {
                    this.udpFileSocket.receive(filePacket);
                    


                    System.arraycopy(buffer, 0, fileBytes, bytesRead, filePacket.getLength());
                    bytesRead += filePacket.getLength();
                    System.out.println("BYTES READ: " + Integer.toString(bytesRead));
                }

                String p = "home2";
                p = p.concat(filePathTrimmed);
                
                FileOutputStream fos = new FileOutputStream(p);
                byte[] slice = Arrays.copyOfRange(fileBytes, 0, fileLen);
                fos.write(slice);
                fos.flush();
                fos.close();

                System.out.println("FILE \"" + p + "\" received and saved.");
                
                // CALCULATE MD5 Checksum
                String md5string = getMD5Checksum(p);
                System.out.println("CHECKSUM: " + md5string);
                
                // SEND ACK (acknowledge)
                byte[] ack = generateMD5Checksum(p);
                DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
                DatagramPacket reply = new DatagramPacket(ackPacket.getData(), ackPacket.getLength(), incAddress, incPort);
                this.udpFileSocket.send(reply);
                
                

            } catch (SocketTimeoutException e) {
                // a receive() timed out, the loop must be resetted and primary server must be informed about this error
                // send info saying there was an error
                
                try{
                    byte[] ack = ByteBuffer.allocate(16).putShort((short) 0).array();
                    DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
                    DatagramPacket reply = new DatagramPacket(ackPacket.getData(), ackPacket.getLength(), incAddress, incPort);
                    this.udpFileSocket.send(reply);
                }catch(IOException ex){
                    Logger.getLogger(FileUDPSecondaryReceive.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                continue;
                
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileUDPSecondaryReceive.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(FileUDPSecondaryReceive.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(FileUDPSecondaryReceive.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } while (true);

    }
}
