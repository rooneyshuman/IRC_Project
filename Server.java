import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


// Server class
public class Server extends Thread{

  // Data members
  private int port;                                      // Server port
  private static ArrayList<Connection> connectionList;   // List with all connections
  private HashSet<String> roomList;

  // Constructor
  public Server(int port){
    this.port = port;
    this.connectionList = new ArrayList<>();
    roomList = new HashSet<>();
  }

  public String getServerRooms(){
    if(roomList.isEmpty()){
      String output = "There are no rooms.";
      return output;
    }
    else
      return roomList.toString();
  }

  public void addServerRoom(String roomName){
    roomList.add(roomName);
  }

  // Removes connection from list
  public static void removeConnection(Connection connection) {
    connectionList.remove(connection);
  }

  // Allows for all connections to have access to list of all connections
  public List<Connection> getConnectionList(){
    return connectionList;
  }

  // Override Thread.run()
  public void run() {
    try{
      ServerSocket serverSocket = new ServerSocket(port);   //creates server socket on port
      System.out.write("server is up!\n".getBytes());

      // Infinite loop to continuously handle incoming connections
      while(true){
        Socket clientSocket = serverSocket.accept();              // Accepts connection to the client - returns socket
        Connection connection = new Connection(this, clientSocket);   // Handles communication with client socket
        connectionList.add(connection);                                      // Add client connection to list
        connection.start();
      }
    }
    catch (IOException e){
      e.printStackTrace();
    }
  }

  public static void main(String [] args) {
    int port = 6667;
    Server server = new Server(port);
    server.start();   //kicks off server thread
  }
}
