package client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Edgar Duarte
 * @author Miguel Dinis
 */
public class Client {

    private static int serversocket;
    private static int serversocket2;
    private static String host1;
    private static String host2;

    private static DataInputStream in;
    private static DataOutputStream out;
    private static String username;
    private static Scanner sc;
    private static String current_dir;

    //Authenticates user
    public static void auth() {

        while (true) {
            try {
                System.out.println("0-Login | 1-Sign up");
                String mode = sc.nextLine();

                if (Integer.parseInt(mode) != 0 && Integer.parseInt(mode) != 1) {
                    System.out.println("Please select a correct mode");
                    continue;
                }

                System.out.println("Username");
                username = sc.nextLine();

                System.out.println("Password");
                String password = sc.nextLine();

                current_dir = System.getProperty("user.dir");
                String message = "1#" + mode + "#" + username + "#" + password + "#" + System.getProperty("user.dir");
                out.writeUTF(message);

                boolean success = in.readBoolean();

                if (success) {
                    System.out.println("You are now authenticated");
                    //Get last directory
                    if (Integer.parseInt(mode) == 0) {

                        current_dir = in.readUTF();

                    }   
                    break;
                } else {
                    System.out.println("Username is already taken or wrong password");
                }

            } catch (NumberFormatException e) {
                System.out.println("Please introduce correct input values");
            } catch (IOException e) {
                System.out.println("IO " + e);
            } catch (Exception e) {
                System.out.println("Something went wrong, please try again");
            }
        }
    }

    //Lets user change password
    //TO-DO Desconetar o cliente e pedir nova autenticacao
    public static void changePassword() {

        try {
            System.out.println("New password: ");
            String password = sc.nextLine();

            String message = "2#" + username + "-" + password;
            out.writeUTF(message);

        } catch (IOException e) {
            System.out.println("IO " + e);
        }

    }

    public static void listServerFiles() {
        try {
            String message = "4";
            out.writeUTF(message);

            String response1 = in.readUTF();
            System.out.println("Dir: " + response1);

            String response2 = in.readUTF();
            System.out.println(response2);

        } catch (IOException e) {
            System.out.println("IO " + e);
        }
    }

    public static void changeDirServer() {
        try {
            System.out.println("Select a dir to go to: ");
            String new_dir = sc.nextLine();

            String message = "5#" + new_dir;
            out.writeUTF(message);

        } catch (IOException e) {
            System.out.println("IO " + e);
        }
    }

    //From https://stackoverflow.com/questions/5694385/getting-the-filenames-of-all-files-in-a-folder
    public static void listLocalFiles() {

        System.out.println("CURRENT DIR     " + current_dir);
        File folder = new File(current_dir);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }

    }

    public static void changeDirLocal() {

        System.out.println("Select a dir to go to: ");
        String path = sc.nextLine();

        if (path.equals("..")) {

            File past_dir = new File(current_dir);
            current_dir = past_dir.getParent();
            System.out.println("In folder " + current_dir);

            try {
                String message = "9#" + username + "#" + current_dir;
                out.writeUTF(message);
            } catch (IOException e) {
                System.out.println("IO " + e);
            }

        } else {

            String temp = current_dir;
            current_dir = current_dir + "\\" + path;

            File file = new File(current_dir);
            if (!file.exists()) {
                current_dir = temp;
                System.out.println("Invalid folder");
            } else {

                try {
                    String message = "9#" + username + "#" + current_dir;
                    out.writeUTF(message);
                } catch (IOException e) {
                    System.out.println("IO " + e);
                }

                System.out.println("In folder " + current_dir);
            }
        }

    }

    public static void downloadFileServer() {
        try {
            System.out.println("File to download: ");
            String filename = sc.nextLine();

            String message = "6#" + filename;
            out.writeUTF(message);

            boolean response = in.readBoolean();

            if (response) {
                try ( Socket s2 = new Socket(host1, 7000)) {

                    // expect a file here, read and save
                    byte[] buffer = new byte[8192];

                    InputStream is = s2.getInputStream();

                    FileOutputStream fos = new FileOutputStream(current_dir + "/" + filename);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);

                    int bytesRead = is.read(buffer, 0, buffer.length);

                    while (bytesRead != -1) {
                        //System.out.println("Bytes read: " + bytesRead);
                        bos.write(buffer, 0, bytesRead);
                        bos.flush();
                        bytesRead = is.read(buffer, 0, buffer.length);
                    }

                    bos.close();
                    s2.close();

                } catch (UnknownHostException e) {
                    System.out.println("Sock:" + e.getMessage());
                } catch (EOFException e) {
                    System.out.println("EOF:" + e.getMessage());
                } catch (IOException e) {
                    System.out.println("IO:" + e.getMessage());
                }
            } else {
                System.out.println("Connection refused or invalid file given");
            }

        } catch (IOException e) {
            System.out.println("IO " + e);
        }
    }

    public static void uploadFileServer() {
        try {
            System.out.println("File to upload: ");
            String filename = sc.nextLine();

            File f = new File(current_dir + "/" + filename);

            if (!f.exists()) {
                System.out.println("Given file does not exist");
                return;
            }

            String message = "7#" + filename;
            out.writeUTF(message);

            try ( Socket s2 = new Socket(host1, 7000)) {

                // send file over above socket
                byte[] buffer = new byte[(int) f.length()];

                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
                bis.read(buffer, 0, buffer.length);

                OutputStream os = s2.getOutputStream();
                os.write(buffer, 0, buffer.length);

                os.flush();

                s2.close();

            } catch (UnknownHostException e) {
                System.out.println("Sock:" + e.getMessage());
            } catch (EOFException e) {
                System.out.println("EOF:" + e.getMessage());
            } catch (IOException e) {
                System.out.println("IO:" + e.getMessage());
            }

        } catch (IOException e) {
            System.out.println("IO " + e);
        }
    }

    public static void help() {
        System.out.println("-----Help------");
        System.out.println("chg pass - Change password");
        System.out.println("conf ports - Configure ports");
        System.out.println("ls server - List files server side");
        System.out.println("ls - List files client side");
        System.out.println("cd server- Change server dir");
        System.out.println("cd - Change local dir");
        System.out.println("download - Download server file");
        System.out.println("upload - Upload local file to server");
        System.out.println("exit - Exit app");
    }

//User picks next action
    public static void menu() {

        while (true) {
            try {
                boolean exit = false;

                System.out.print(current_dir + ">");

                String option = sc.nextLine();

                switch (option) {
                    case "help":
                        help();
                        break;

                    case "chg pass":
                        changePassword();
                        break;

                    case "conf ports":
                        break;

                    case "ls server":
                        listServerFiles();
                        break;

                    case "ls":
                        listLocalFiles();
                        break;

                    case "cd server":
                        changeDirServer();
                        break;

                    case "cd":
                        changeDirLocal();
                        break;

                    case "download":
                        downloadFileServer();
                        break;

                    case "upload":
                        uploadFileServer();
                        break;

                    case "exit":
                        exit = true;

                        try {
                            String message = "8";
                            out.writeUTF(message);

                        } catch (IOException e) {
                            System.out.println("IO " + e);
                        }

                        break;

                    default:
                        System.out.println("Please input a correct value");
                }

                if (exit) {
                    break;
                }

            } catch (NumberFormatException e) {
                System.out.println("Please introduce correct input values");
            }
        }
    }

    public static void main(String[] args) {

        if (args.length != 4) {
            System.out.println("java client hostname port1 hostname2 port2");
            System.exit(0);

        }

        try {
            host1 = args[0];
            serversocket = Integer.parseInt(args[1]);
            host2 = args[2];
            serversocket2 = Integer.parseInt(args[3]);

        } catch (Exception e) {
            System.out.println("java client hostname port1 hostname2 port2");
        }

        try ( Socket s = new Socket(args[0], serversocket)) {

            current_dir = System.getProperty("user.dir");

            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());

            sc = new Scanner(System.in);

            auth();

            menu();

            s.close();

        } catch (UnknownHostException e) {
            System.out.println("Sock:" + e.getMessage());
        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }

    }
}
