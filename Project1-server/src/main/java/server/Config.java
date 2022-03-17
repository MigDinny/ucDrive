package server;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author edgar
 */
class Config {

    public volatile JSONArray user_list = new JSONArray();

    public Config() {
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader("conf.json")) {
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
    public boolean auth(String username, String password) {

        for (int i = 0; i < user_list.size(); i++) {
            JSONObject user = (JSONObject) user_list.get(i);

            if (user.get("username").equals(username) && user.get("password").equals(password)) {
                return true;
            }
        }
        return false;
    }

    //Adds user to configuration file
    //Returns false if a user with the same user name exists. Returns true if user was sucessfully added
    synchronized boolean addUser(String username, String password, String dir) {
        JSONObject json_user = new JSONObject();

        for (int i = 0; i < user_list.size(); i++) {
            JSONObject user = (JSONObject) user_list.get(i);
            if (user.get("username").equals(username)) {
                return false;
            }
        }

        json_user.put("username", username);
        json_user.put("password", password);
        json_user.put("dir", dir);

        user_list.add(json_user);

        writeToFile();

        return true;

    }

    //updates the password of a user
    synchronized void updatePassword(String username, String new_password) {
        for (int i = 0; i < user_list.size(); i++) {
            JSONObject user = (JSONObject) user_list.get(i);

            if (user.get("username").equals(username)) {
                user.put("password", new_password);
            }
        }
        writeToFile();

    }

    synchronized void writeToFile() {
        try (FileWriter file = new FileWriter("conf.json")) {
            //We can write any JSONArray or JSONObject instance to the file
            file.write(user_list.toJSONString());
            file.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    synchronized void changeDir(String username, String new_directory){
        for (int i = 0; i < user_list.size(); i++){
            JSONObject user = (JSONObject) user_list.get(i);
            
            if(user.get("username").equals(username)){
                user.put("dir", new_directory);
            }
        }
        writeToFile();
    }
    
    
    synchronized String getDir(String username){
        for(int i = 0; i < user_list.size(); i++){
            JSONObject user = (JSONObject) user_list.get(i);
            
            if(user.get("username").equals(username)){
                return user.get("dir").toString();
            }
        }
        
        return "-1";
        
    }
}
