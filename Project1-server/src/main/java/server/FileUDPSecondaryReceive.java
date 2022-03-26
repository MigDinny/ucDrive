/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 *
 * @author edgar
 */
public class FileUDPSecondaryReceive extends Thread {

    private static DatagramSocket udpFileSocket;

    public FileUDPSecondaryReceive(int serverPort)  throws InterruptedException, IOException {

        this.udpFileSocket = new DatagramSocket(serverPort);
        this.start();
       
    }

    public void run() {

        try {
            byte[] buffer = new byte[8192];
            byte[] pathFile = new byte[8192];
            
            while (true) {
                
                DatagramPacket pathReply = new DatagramPacket(pathFile, pathFile.length);
                DatagramPacket file = new DatagramPacket(buffer, buffer.length);
                
                udpFileSocket.receive(pathReply);
                
                String path_received = new String(pathReply.getData(), 0, pathReply.getLength());
                
                FileOutputStream fos = new FileOutputStream(path_received);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                
                udpFileSocket.receive(pathReply);
                int reply = 
                while (pathReply.getData() != -1) {
                    //System.out.println("Bytes read: " + bytesRead);
                    bos.write(buffer, 0, pathReply.getData().intValue());
                    bos.flush();
                    pathReply = is.read(buffer, 0, buffer.length);
                }

                bos.close();
                
                
            }
        } catch (Exception e) {

        }
    }
}
