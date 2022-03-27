package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Edgar
 * @author Miguel
 */
class Connection extends Thread {
    
    private String homeDir; // home directory with all the contents, cannot be changed
    private Socket client_socket;
    private DataInputStream in; // stream to receive from client
    private DataOutputStream out; // stream to reply to client
    private int thread_number; // thread number for debug purposes
    private String[] data_arr;
    private Config confF; // config.json
    private Stack<String> path; // each folder is an element of this stack, so we can push and pop
    private File currentDir;
    private Queue<String> fileQueue;
    private boolean secondaryActingAsPrimary = false;
    
    
    //Constructor
    public Connection(Socket client_socket, int n_thread, Queue<String> fileQueue, Config confF, String homeDir) {
        try {

            this.client_socket = client_socket;
            
            this.in = new DataInputStream(client_socket.getInputStream());
            this.out = new DataOutputStream(client_socket.getOutputStream());
            this.thread_number = n_thread;
            this.confF = confF;
            this.homeDir = homeDir;
            this.path = new Stack<>();
            this.fileQueue = fileQueue;
            
            
            this.currentDir = new File(homeDir);
            
            this.start();
        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
        }
    }
    
    public Connection(Socket client_socket, int n_thread, Config confF, String homeDir) {
        try {

            this.client_socket = client_socket;
            
            this.in = new DataInputStream(client_socket.getInputStream());
            this.out = new DataOutputStream(client_socket.getOutputStream());
            this.thread_number = n_thread;
            this.confF = confF;
            this.homeDir = homeDir;
            this.path = new Stack<>();
            this.secondaryActingAsPrimary = true;
            
            this.currentDir = new File(homeDir);
            
            this.start();
        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
        }
    }

    private void reply(String s) {
        try {
            out.writeUTF(s);
            out.flush();
        } catch (IOException ex) {
            System.out.println("!asdasd");
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void reply(boolean b) {
        try {
            out.writeBoolean(b);
            out.flush();
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private String joinPath(Stack<String> s) {
        String path = homeDir + "/";
        
        for (String f : s) {
            path = path.concat(f);
            path = path.concat("/");
        }
        
        return path;
    }
    
    private String joinPathWithoutHomeDir(Stack<String> s) {
        String path = "";
        
        for (String f : s) {
            path = path.concat(f);
            path = path.concat("/");
        }
        
        return path;
    }
   
    
    //Authenticates the new client connection.
    /*

        data_arr[1] == option ( 0 = wants to login, 1 = wants to signup)
        data_arr[2] == username
        data_arr[3] == password
     */
    public boolean authenticate() {
        try {
            if (Integer.parseInt(data_arr[1]) == 0) {
                if (confF.auth(data_arr[2], data_arr[3])) {
                    out.writeBoolean(true);
                    
                    String dir = confF.getDir(data_arr[2]);
                    String dir_path_arr[] = dir.split("/");
                    
                    //ignore home/
                    boolean first = true;

                    for(String s : dir_path_arr){
                        if (!first) this.path.push(s);
                        first = false;
                        
                    }
                    this.homeDir = this.homeDir + "/" + confF.getId(data_arr[2]);
                    this.currentDir = new File(joinPath(this.path));
                    
                    
                    
                } else {
                    out.writeBoolean(false);
                }
            } else {
                if (confF.addUser(data_arr[2], data_arr[3], "")) {
                    this.homeDir = this.homeDir + "/" + confF.getId(data_arr[2]);
                    this.currentDir = new File(this.homeDir);
                    this.currentDir.mkdir();
                    out.writeBoolean(true);
                } else {
                    out.writeBoolean(false);
                }
            }
            

        } catch (NumberFormatException e) {
            System.out.println("Something is wrong with incoming string. Thread ending");
            return false;
        } catch (IOException e) {
            System.out.println("IO: " + e);
            return false;
        }

        return true;
    }

    //Changes user's password
    /*
        data_arr[1] == username
        data_arr[2] == password
     */
    public void changePassword() {
        confF.updatePassword(data_arr[1], data_arr[2]);
    }

    //Lets client change the addresses and ports of the servers
    public void configPorts() {
        //TO-DO
        return;
    }

    //Lists all of the files in the current directory of the server
    public void listFilesServer() {
        String[] dirsAndFiles;
        
        dirsAndFiles = this.currentDir.list();
        
        String response = "";
        
        for (String s : dirsAndFiles)
            response += s + "\n";
        
        System.out.println("OUTPUT LIST: " + response);       
        reply("Currently on " + this.joinPath(path));
        reply(response);

    }

    //Changes server directory
    public void changeDir() {
        String newFolder = data_arr[1];
        
        System.out.println("change dir: " + newFolder);
        
        if (newFolder.equals("..")) {
            if (!this.path.empty()) this.path.pop();
        } else {
            this.path.push(newFolder);
        }
       
        System.out.println(joinPath(this.path));
        
        confF.changeDir(data_arr[2], joinPath(this.path));
        
        this.currentDir = new File(joinPath(this.path));
        
        System.out.println("Current path: " + this.currentDir.getAbsolutePath());
        
        // if new directory doesn't exist, create it anyways
        this.currentDir.mkdir();
    }

    //Sends to the client's directory a file that is currently in the server's directory
    public void getServerFile() {
        String filename = data_arr[1];
        
        File f = new File(joinPath(this.path) + filename);
        
        if (!f.exists()) {
            reply(false);
            return;
        }
        
        
        try (ServerSocket listenSocket = new ServerSocket(0)) {
            
            System.out.println(listenSocket.getLocalPort());
            reply(true); // green light s
            out.writeInt(listenSocket.getLocalPort());
            
            
            
            Socket fileTransferSocket = listenSocket.accept();
                        
            // send file over above socket
            byte[] buffer = new byte[(int) f.length()];   
            
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
            bis.read(buffer, 0, buffer.length);
            
            OutputStream os = fileTransferSocket.getOutputStream();
            os.write(buffer, 0, buffer.length);
            
            os.flush();
            
            fileTransferSocket.close();
            bis.close();
        

        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
            reply(false);
        }
    }
    
    
    public void downloadClientFile(){
        String filename = data_arr[1];
        
        try (ServerSocket listenSocket = new ServerSocket(0)) {
            System.out.println(listenSocket.getLocalPort());
            out.writeInt(listenSocket.getLocalPort());
            Socket fileDownloadSocket = listenSocket.accept();
            
            // expect a file here, read and save
                byte[] buffer = new byte[8192];

                InputStream is = fileDownloadSocket.getInputStream();

                FileOutputStream fos = new FileOutputStream(joinPath(this.path) + filename);
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                int bytesRead = is.read(buffer, 0, buffer.length);

                while (bytesRead != -1) {
                    //System.out.println("Bytes read: " + bytesRead);
                    bos.write(buffer, 0, bytesRead);
                    bos.flush();
                    bytesRead = is.read(buffer, 0, buffer.length);
                }

                bos.close();
                listenSocket.close();
        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }
        
        
        // if this thread was launched by Primary Server and not by Secondary Server (acting as Primary)
        // add file to Queue
        if (!secondaryActingAsPrimary) 
            fileQueue.add(joinPathWithoutHomeDir(this.path) + filename);
        
    }

    /*
      data_arr[0] == mode
    
    
     */
    public boolean switcher(String data) {

        try {
            data_arr = data.split("#");

            switch (Integer.parseInt(data_arr[0])) {

                case 1:
                    authenticate();
                    break;
                case 2:
                    changePassword();
                    break;
                case 3:
                    configPorts();
                    break;
                case 4:
                    listFilesServer();
                    break;
                case 5:
                    changeDir();
                    break;
                case 6:
                    getServerFile();
                    break;
                case 7:
                    downloadClientFile();
                    break;
                    
                case 8:
                    return false;
                    
                    
            }
        } catch (NumberFormatException e) {
            System.out.println("Something is wrong with incoming string. Thread ending");
            return false;
        }

        return true;
    }

    @Override
    public void run() {
        String response;
        try {
            while (true) {
                //First we need to authenticate the user
                String data = in.readUTF();

                if (!switcher(data)) {
                    System.out.println("Client exited");
                    break;
                }
            }

        } catch (EOFException e) {
            System.out.println("EOF:" + e);
        } catch (IOException e) {
            System.out.println("IO:" + e);

        }

    }
}
