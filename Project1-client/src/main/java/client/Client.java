package client;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
//package project1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 *
 * @author Edgar Duarte
 * @author Miguel Dinis
 */




public class Client {
    
    private static int serversocket = 6000;
    private static DataInputStream in;
    private static DataOutputStream out;
    private static String username;
    private static Scanner sc;
    
    
    //Authenticates user
    public static void auth(){
        
        
        while(true){
            try{
                System.out.println("0-Login | 1-Sign up");
                String mode = sc.nextLine();

                if(Integer.parseInt(mode) != 0 && Integer.parseInt(mode) != 1){
                    System.out.println("Please select a correct mode"); 
                    continue;
                }
                
                System.out.println("Username");
                username = sc.nextLine();
                
                System.out.println("Password");
                String password = sc.nextLine();
                
                String message = "1-" + mode + "-" + username + "-" + password;
                out.writeUTF(message);
                
                boolean success = in.readBoolean();
                
                if(success){
                    System.out.println("You are now authenticated");
                    break;
                }
                
                
                
                
                
            }catch (NumberFormatException e) {
                System.out.println("Please introduce correct input values");
            }catch (IOException e){
                System.out.println("IO " + e);
            }catch (Exception e){
                System.out.println("Something went wrong, please try again");
            }
        }
    }
    
    //Lets user change password
    //TO-DO Desconetar o cliente e pedir nova autenticacao
    public static void changePassword(){
        
        try{
            System.out.println("New password: ");
            String password = sc.nextLine();

            String message = "2-" + username + "-" + password;
            out.writeUTF(message);
        
        }catch(IOException e){
            System.out.println("IO " + e);
        }
        
        
        
    }
    
    //User picks next action
    public static void menu(){
        
        while(true){
            try{
                boolean exit = false;
                System.out.println("What action do you want to perform:");
                System.out.println("1- Change password");
                System.out.println("2- Configure ports");
                System.out.println("3- List files server side");
                System.out.println("4- List files client side");
                System.out.println("5- Change server dir");
                System.out.println("6- Change local dir");
                System.out.println("7- Download server file");
                System.out.println("8- Upload local file to server");
                System.out.println("9- Exit app");
                
                int option = Integer.parseInt(sc.nextLine());
                
                switch(option){
                    case 1:
                        changePassword();
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                    case 4:
                        break;
                    case 5:
                        break;
                    case 6:
                        break;
                    case 7:
                        break;
                    case 8:
                        break;
                    case 9:
                        exit = true;
                        break;
                        
                    default:
                        System.out.println("Please input a correct value");
                }
                
                if(exit) break;
                
            }catch (NumberFormatException e) {
                System.out.println("Please introduce correct input values");
            }
        }
    }
    
    public static void main(String[] args) {

        
        
        if (args.length == 0) {
			System.out.println("java TCPClient hostname");
			System.exit(0);
        }
        
        
        try (Socket s = new Socket(args[0], serversocket)) {
            
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
            
            sc = new Scanner(System.in);
            
            auth();
            
            menu();
			
	} catch (UnknownHostException e) {
            System.out.println("Sock:" + e.getMessage());
	} catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
	} catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
	}
	
    }
}
