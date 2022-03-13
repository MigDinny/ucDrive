package server;

/**
 *
 * @author Edgar Duarte
 * @author Miguel Dinis
 */



import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
 
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author edgar
 */
class ConfFile {
    public volatile JSONArray user_list = new JSONArray();

    
    public ConfFile(){
        JSONParser jsonParser = new JSONParser();
        
        try (FileReader reader = new FileReader("conf.json"))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
            
            user_list = (JSONArray) obj;
            
            
 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public JSONArray getUser_list() {
        return user_list;
    }

    public void setUser_list(JSONArray user_list) {
        this.user_list = user_list;
    }
    
    //Returns true if there is a user and a password that fit.
    //TODO MAKE SURE THAT PASSWORD IS ENCRYPTED
    public boolean auth(String username, String password){
        
        for(int i = 0; i < user_list.size(); i++ ){
            JSONObject user = (JSONObject) user_list.get(i);
            
            
            if(user.get("username").equals(username) && user.get("password").equals(password)){
                return true;
            }
        }
        return false;
    }
    
    //Adds user to configuration file
    //Returns false if a user with the same user name exists. Returns true if user was sucessfully added
    synchronized boolean addUser(String username, String password){
        JSONObject json_user = new JSONObject();
        
        for(int i = 0; i < user_list.size(); i++){
            JSONObject user = (JSONObject) user_list.get(i);
            if(user.get("username").equals(username)){
                return false;
            }
        }
        
        
        
        json_user.put("username", username);
        json_user.put("password", password);
        
        user_list.add(json_user);
        
        writeToFile();
        
        return true;
        
    }
    
    //updates the password of a user
    synchronized void updatePassword(String username, String new_password){
        for(int i = 0; i < user_list.size(); i++ ){
            JSONObject user = (JSONObject) user_list.get(i);

            if(user.get("username").equals(username)){
                user.put("password", new_password); 
            }
        }
        writeToFile();
        
    }
    
    synchronized void writeToFile(){
        try (FileWriter file = new FileWriter("conf.json")) {
            //We can write any JSONArray or JSONObject instance to the file
            file.write(user_list.toJSONString()); 
            file.flush();
 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


//Will receive client's messages and behave accordingly to its requests.
class Connection extends Thread {
    
    Socket client_socket;
    DataInputStream in;
    DataOutputStream out;
    
    
    int thread_number;
    String[] data_arr;
    
    ConfFile confF;
    
    //Constructor
    Connection(Socket client_socket, int n_thread, ConfFile confF) {
        try{
            
            this.client_socket = client_socket;
            in = new DataInputStream(client_socket.getInputStream());
            out = new DataOutputStream(client_socket.getOutputStream());
            
            this.thread_number = n_thread;
            
            this.confF = confF;
            
            this.start();
        }catch(IOException e){
            System.out.println("Connection:" + e.getMessage());
        }
    }
    
    //Authenticates the new client connection.
    /*

        data_arr[1] == option ( 0 = wants to login, 1 = wants to signup)
        data_arr[2] == username
        data_arr[3] == password
    */
    public boolean authenticate(){
        

        try{
            if(Integer.parseInt(data_arr[1]) == 0){
               if(confF.auth(data_arr[2], data_arr[3])){
                   out.writeBoolean(true);
               }
               else{
                   out.writeBoolean(false);
               }
            }

            else{
                if(confF.addUser(data_arr[2], data_arr[3])){
                    out.writeBoolean(true);
                }
                else{
                    out.writeBoolean(false);
                }
            }
            
        }catch (NumberFormatException e) {
            System.out.println("Something is wrong with incoming string. Thread ending");
            return false;   
        }catch( IOException e){
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
    public void changePassword(){
        System.out.println("Here");
        confF.updatePassword(data_arr[1], data_arr[2]);
        return;
    }
    
    //Lets client change the addresses and ports of the servers
    public void configPorts(){
        //TO-DO
        return;
    }
    
    //Lists all of the files in the current directory of the server
    public void listFilesServer(){
        return;
    }
    
    //Changes server directory
    public void changeDir(){
        return;
    }
    
    //Sends to the client's directory a file that is currently in the server's directory
    public void getServerFile(){
        return;
    }
    
    /*
      data_arr[0] == mode
    
    
    */
    public boolean switcher(String data){
        
        try{
            data_arr = data.split("-");
            
            switch(Integer.parseInt(data_arr[0])){
                
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
            }
        }catch (NumberFormatException e) {
            System.out.println("Something is wrong with incoming string. Thread ending");
            return false;   
        }
        
        return true;
    }
        
    public void run() {
        String response;
        try{
            while(true){
                //First we need to authenticate the user
                String data = in.readUTF();
                
                if(!switcher(data)) break;
            }
   
        } catch(EOFException e) {
            System.out.println("EOF:" + e);
        } catch(IOException e) {
            System.out.println("IO:" + e);
            
        }

      
    }
}

//Creates a connection for each client that tries to connect to the server
public class Server {
    
    private static int serverPort = 6000;
    
    
    public static void main(String[] args) {
        
        int n_thread = 0;
        
        ConfFile confF = new ConfFile();
                
        try (ServerSocket listenSocket = new ServerSocket(serverPort)) {
            
            while(true) {
                Socket clientSocket = listenSocket.accept();
                n_thread++;
                new Connection(clientSocket, n_thread, confF);
            }
        
        }catch(IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }
        
    }
    
}
