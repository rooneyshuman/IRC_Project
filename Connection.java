// Instance of connection to server

import org.apache.commons.lang3.*;
import java.io.*;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;

// Derived from Thread class - handles multiple connections
public class Connection extends Thread {
  private Socket clientSocket;
  private Server server;
  private String nickname;
  private OutputStream out;
  private InputStream in;
  private HashSet<String> roomList;

  // Constructor
  public Connection(Server server, Socket clientSocket) throws IOException {
    this.clientSocket = clientSocket;
    this.server = server;
    this.nickname = null;
    this.roomList = new HashSet<>();
    this.in = clientSocket.getInputStream();              //gets input from client
    this.out = clientSocket.getOutputStream();            //sends output to client
  }

  // Override of Thread.run()
  public void run() {
    try {
      connectionManager();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Getter for username
  public String getNickname() {
    return nickname;
  }


  // Allows multiple connections being handled, takes in clientSocket for that client connection
  private void connectionManager() throws IOException {
    String userInput;
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    promptNick();
    displayCommands();

    while ((userInput = br.readLine()) != null) {
      String[] parsedInput = StringUtils.split(userInput);          //splits the userInput into diff string parsedInput based on whitespace
      if (parsedInput != null && parsedInput.length > 0) {
        String command = parsedInput[0];                                // take first word as command
        if (command.equals("QUIT")) {
          quit();
          break;
        } else if (command.equals("JOIN")) {
          joinRoom(parsedInput);
        } else if (command.equals("LEAVE")) {
          leaveRoom(parsedInput);
        } else if (command.equals("ROOMS")) {
          displayRooms();
        } else if (command.equals("USERS")) {
          displayUsers(parsedInput);
        } else if (command.equals("SEND")) {
          String[] parsedMessage = StringUtils.split(userInput, null, 3);          //splits the userInput into diff string parsedInput based on whitespace
          sendMsg(parsedMessage);
        } else if (command.equals("HELP")){
          displayCommands();
        }
        else {
          String msg = "Unknown command: " + command + "\n";
          out.write(msg.getBytes());
        }
      }
    }

    clientSocket.close();   //ends connection
  }

  // Displays all users in a room
  private void displayUsers(String [] parsedInput) throws IOException {
    if(parsedInput.length > 1){
      String roomName = parsedInput[1];
      // Find message recipient
      List<Connection> connectionList = server.getConnectionList();
      boolean isEmpty = true;
      for (Connection connection : connectionList) {
        if(connection.inRoom(roomName)) {
          printOut(connection.nickname + "\n");
          isEmpty = false;
        }
      }
      if (isEmpty)
        printOut("The room is empty\n");
    }
    else
      printOut("Please enter a room name\n");
  }

  // Prompts user for nickname upon connection
  public void promptNick() throws IOException {
    String userInput;
    String[] parsedInput;
    out.write("Enter a nickname: \n".getBytes());
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    userInput = br.readLine();
    parsedInput = StringUtils.split(userInput);
    nickname = parsedInput[0];
    String output = "Welcome, " + nickname + "!\n\n";
    out.write(output.getBytes());
  }

  // Displays all available commands
  public void displayCommands() throws IOException {
    String output = "These are all available commands: \n" +
      "JOIN @room_name: creates a room by that name if it doesn't exist already, or joins it if it does.\n" +
      "LEAVE @room_name: leaves a room that has been joined.\n" +
      "ROOMS: lists all rooms that have been created\n" +
      "USERS @room_name: lists all users that have joined that room\n" +
      "SEND nickname message: sends a private message to a user with that nickname\n" +
      "SEND @room_name message: sends a public message to users in a room\n" +
      "HELP: shows this list of available commands\n" +
      "QUIT: terminates connection\n";
    printOut(output);
  }

  // Displays all rooms that have been joined/created
  private void displayRooms() throws IOException {
    printOut(server.getServerRooms() + "\n");
  }

  // Allows a user to leave a room
  private void leaveRoom(String[] parsedInput) throws IOException {
    if (parsedInput.length > 1) {
      String roomName = parsedInput[1];
      roomList.remove(roomName);
      printOut("You have now left " + roomName + "\n");
    }
  }

  // Checks whether a user is in a room
  public boolean inRoom(String roomName) {
    return roomList.contains(roomName);
  }

  // Utility function to facilitate output to client
  public void printOut(String output) throws IOException {
    out.write(output.getBytes());
  }

  // Allows a client to join a room
  private void joinRoom(String[] parsedInput) throws IOException {
    if (parsedInput.length > 1) {
      String roomName = parsedInput[1];
      roomList.add(roomName);
      server.addServerRoom(roomName);
      printOut("You have now joined " + roomName + "\n");
    } else {
      printOut("Please enter a room name to join\n");
    }
  }

  // Sends message to a specific user or to a users in a room
  private void sendMsg(String[] parsedInput) throws IOException {
    // Invalid number of arguments
    if (parsedInput.length < 3)
      printOut("Please enter a message\n");

    else {
      String recipient = parsedInput[1];
      String message = parsedInput[2];
      boolean isRoom = recipient.charAt(0) == '@';

      // Find message recipient
      List<Connection> connectionList = server.getConnectionList();
      for (Connection connection : connectionList) {

        // Message is being sent to users in a room
        if (isRoom) {
          // If the current connection's user is in a room, display message
          if (connection.inRoom(recipient)) {
            connection.printOut("<" + recipient + ">. " + nickname + " : " + message + "\n");
          }

          // Message is being sent to a single user
        } else {
          if (connection.getNickname().equals(recipient)) {
            connection.printOut("<" + nickname + "> : " + message + "\n");
          }
        }
      }
    }
  }

  // Ends connection with server
  private void quit() throws IOException {
    Server.removeConnection(this);    // removes connection from list
    String output = "Goodbye, " + nickname + "\n";
    out.write(output.getBytes());
    clientSocket.close();   // closes connection
  }
}
