import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import memory.Memory;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible
    // when running tests.
    //System.out.println("Logs from your program will appear here!");
    ServerSocket serverSocket = null;
    int port = 6379;
    try {
      serverSocket = new ServerSocket(port);
      serverSocket.setReuseAddress(true);
      // Wait for connection from client.

      //Logic to clear the expired keys from memory store after every 10s
      Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
        Memory.expireKeys();
      },5, 5, TimeUnit.SECONDS);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        Thread t = new Thread(new ClientHandler(clientSocket));
        t.start();
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    } finally {
      try {
        if (serverSocket != null) {
          serverSocket.close();
        }
      } catch (IOException e) {
        System.out.println("IOException: " + e.getMessage());
      }
    }
  }

  static class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
      this.clientSocket = clientSocket;
    }

    private void handlePing(BufferedWriter writer) throws IOException {
      writer.write("+PONG\r\n");
      writer.flush();
    }

    private void handleEcho(BufferedWriter writer, String line) throws IOException {
      writer.write("+" + line + "\r\n");
      writer.flush();
    }

    private void handleSet(BufferedWriter writer, List<String> args) throws IOException {
      String key = args.get(0);
      String value = args.get(1);
      long pxMilliSeconds = 86400000000L; //default expiry for a day
      for(int i=2;i<args.size();i++){
        if(args.get(i).equalsIgnoreCase("px")){
          pxMilliSeconds = Long.parseLong(args.get(i+1));
        }
      }
      Memory.set(key, value, pxMilliSeconds);
      writer.write("+OK\r\n");
      writer.flush();
    }

    private void handleGet(BufferedWriter writer, String key) throws IOException {
      String value = Memory.get(key);
      if(value != null)
        writer.write("+" + value +"\r\n");
      else writer.write("$-1\r\n");
      writer.flush();
    }

    public void run() {
      try {
        //BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        byte[] buffer = new byte[1024];
        while (clientSocket.getInputStream().read(buffer)!=-1) {
          String msg = new String(buffer);
          List<String> args = Arrays.stream(msg.split("\r\n")).filter(arg -> !arg.trim().isEmpty() && !arg.startsWith("*") && !arg.startsWith("$")).collect(Collectors.toList());
          switch (args.get(0).toLowerCase()) {
            case "ping":
              handlePing(writer);
              break;
            case "echo":
              handleEcho(writer, args.get(1));
              break;
            case "set":
              handleSet(writer, args.subList(1, args.size()));
              break;
            case "get":
              handleGet(writer, args.get(1));
              break;
            default:
              break;
          }
        }
      } catch (IOException e) {
        System.out.println("IOException: " + e.getMessage());
      } finally {
        try {
          if (clientSocket != null) {
            clientSocket.close();
          }
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
        }
      }
    }
  }
}
