/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author edgar
 */
public class FileUDPSecondaryReceive extends Thread {

    private static DatagramSocket udpFileSocket;

    public FileUDPSecondaryReceive(int serverPort) throws InterruptedException, IOException {

        this.udpFileSocket = new DatagramSocket(serverPort);
        this.udpFileSocket.setSoTimeout(1000); // timeout de 1 sec
        this.start();

    }




    public void run() {

        do {
            int bytesRead = 0;
            int fileLen = 0;
            String filePath;
            byte[] lenBytes = new byte[4];
            byte[] pathBytes = new byte[1024];

            DatagramPacket lenBytesPacket = new DatagramPacket(lenBytes, 4);
            DatagramPacket pathBytesPacket = new DatagramPacket(pathBytes, 1024);

            try {

                this.udpFileSocket.receive(lenBytesPacket);
                this.udpFileSocket.receive(pathBytesPacket);
                fileLen = ByteBuffer.wrap(lenBytes).getInt();
                filePath = new String(pathBytes, StandardCharsets.UTF_8);
                String filePathTrimmed = filePath.trim();
                
                System.out.println("LENGTH: " + Integer.toString(fileLen));
                System.out.println("PATH: " + filePath);

                byte[] fileBytes = new byte[fileLen + 1024];
                byte[] buffer = new byte[1024];
                DatagramPacket filePacket = new DatagramPacket(buffer, 1024);

                while (bytesRead < fileLen) {
                    this.udpFileSocket.receive(filePacket);

                    System.arraycopy(buffer, 0, fileBytes, bytesRead, filePacket.getLength());
                    bytesRead += filePacket.getLength();
                    System.out.println("BYTES READ: " + Integer.toString(bytesRead));
                }

                String p = "home2/";
                p = p.concat(filePathTrimmed);

                FileOutputStream fos = new FileOutputStream(p);
                byte[] slice = Arrays.copyOfRange(fileBytes, 0, fileLen);
                fos.write(slice);
                fos.flush();
                fos.close();

                System.out.println("FILE \"" + filePath + "\" received and saved.");
                
                // SEND ACK (acknowledge)

            } catch (SocketTimeoutException e) {
                
                // we know that there was something wrong receiving the file, probably
                
                // send info saying there was an error
                
                continue;
                
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileUDPSecondaryReceive.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(FileUDPSecondaryReceive.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } while (true);

    }
}
