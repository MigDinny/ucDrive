package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
Class Config
    
Loads, handles and manages server's configurations.
 */
class Config {

    private volatile JSONArray users = new JSONArray();
    private final String filename = "conf.json";

    /*    
    Reads and parses config file.
     */
    public Config() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        Object obj = new JSONArray();

        // try parse
        try {
            FileReader reader = new FileReader(filename);
            obj = jsonParser.parse(reader);
        } catch (FileNotFoundException fe) {
            File conf = new File(filename);
            conf.createNewFile();

            FileReader reader = new FileReader(filename);
            obj = jsonParser.parse(reader);
        } catch (ParseException e) {
            System.out.println("Warning: conf.json empty");
        }

        users = (JSONArray) obj;
    }

    /*
    Returns JSONArray object containing all users and their info.
     */
    public JSONArray getUsers() {
        return users;
    }

    /*
    Sets JSONArray object containing all users and their info.
     */
    public void setUsers(JSONArray users) {
        this.users = users;
    }

    /*
    Authenticates a client.
    TO-DO: encrypt password.
    Returns true if succeeded and false otherwise.
     */
    public boolean auth(String username, String password) {

        for (int i = 0; i < users.size(); i++) {
            JSONObject user = (JSONObject) users.get(i);

            if (user.get("username").equals(username) && user.get("password").equals(password)) {
                return true;
            }
        }
        return false;
    }

    /*
    Returns a user ID given the username.
    Returns -1 if not found any user with given username.
     */
    public int getId(String username) {

        for (int i = 0; i < users.size(); i++) {
            JSONObject user = (JSONObject) users.get(i);
            if (user.get("username").equals(username)) {
                return Integer.parseInt(user.get("id").toString());
            }
        }

        return -1;
    }

    /*
    Adds a user to the configuration file.
    Returns true if succeeded, false if there is already a user with given username.
     */
    synchronized boolean addUser(String username, String password, String dir) throws IOException {
        JSONObject json_user = new JSONObject();

        for (int i = 0; i < users.size(); i++) {
            JSONObject user = (JSONObject) users.get(i);
            if (user.get("username").equals(username)) {
                return false;
            }
        }

        json_user.put("username", username);
        json_user.put("password", password);
        json_user.put("dir", dir);
        json_user.put("id", getNewID());

        users.add(json_user);

        writeToFile();

        return true;

    }

    // gets the next assignable ID
    private int getNewID() {
        int j = 1;
        for (int i = 0; i < users.size(); i++) {
            JSONObject user = (JSONObject) users.get(i);
            int aux = Integer.parseInt(user.get("id").toString());

            if (aux >= j) {
                j = aux + 1;
            }
        }
        return j;
    }

    /*
    Updates the password of a given user.
     */
    public synchronized void updatePassword(String username, String new_password) throws IOException {
        for (int i = 0; i < users.size(); i++) {
            JSONObject user = (JSONObject) users.get(i);

            if (user.get("username").equals(username)) {
                user.put("password", new_password);
            }
        }
        writeToFile();

    }

    // Flushes the JSON array to the disk file
    private synchronized void writeToFile() throws IOException {
        FileWriter file = new FileWriter(filename);
        //We can write any JSONArray or JSONObject instance to the file
        file.write(users.toJSONString());
        file.flush();
        file.close();
    }

    /*
    Changes the user's remote directory on the config file, so it is later remembered. 
     */
    public synchronized void changeDir(String username, String new_directory) throws IOException {
        for (int i = 0; i < users.size(); i++) {
            JSONObject user = (JSONObject) users.get(i);

            if (user.get("username").equals(username)) {
                user.put("dir", new_directory);
            }
        }
        writeToFile();
    }

    /*
    Returns the path of the user's remote directory retrieved from the config file.
    Returns "-1" if user doesn't exist.
     */
    synchronized String getDir(String username) {
        for (int i = 0; i < users.size(); i++) {
            JSONObject user = (JSONObject) users.get(i);

            if (user.get("username").equals(username)) {
                return user.get("dir").toString();
            }
        }

        return "-1";
    }
}
