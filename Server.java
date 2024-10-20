import Game.ClientMessage;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Server {
    public static DatagramSocket udpSocket;
    public static int serverPort = 8888;
    public static int bufferSize = 1024;
    private static List<Player> playerList = new ArrayList<>();
    private static BlockingQueue<ClientMessage> messageQueue = new LinkedBlockingQueue<>();

    public static List<Player> getPlayerList() {
        return playerList;
    }

    private static int playerCount = 0;

    public static int assignPlayerId() {
        return playerCount;
    }

    public static void TheServer() {
        try {
            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket clientPacket = new DatagramPacket(buffer, bufferSize);
                try {
                    udpSocket.receive(clientPacket);
                } catch (IOException e) {
                    System.out.println("cannot receive data packet!");
                    continue;
                }
                InetAddress clientAddress = clientPacket.getAddress();
                int clientPort = clientPacket.getPort();
                System.out.println("Client Address: " + clientAddress.getHostAddress() + ":" + clientPort);
                String message = new String(clientPacket.getData(), 0, clientPacket.getLength());
                System.out.println("Client Message: " + message);
                message = message.trim();
                int newMessage = Integer.parseInt(message) + 5;
                System.out.println(newMessage);
                byte responseBuffer[] = String.valueOf(newMessage).getBytes();
                DatagramPacket newPacket = new DatagramPacket(responseBuffer, responseBuffer.length, clientAddress, clientPort);
                udpSocket.send(newPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("did not get data!");
        }
    }

    public static void main(String[] args) throws SocketException {
        udpSocket = new DatagramSocket(serverPort);
        TheServer();
          // Start receive thread
        new Thread(new receive(messageQueue)).start();

          // Start calculate thread
        new Thread(new Calculate(messageQueue)).start();
  
          // Placeholder for the Send thread (if you need it)
          // new Thread(new Send()).start();
    }
    // Add player if not exists, based on their IP and port
    public static synchronized void addPlayerIfNotExists(InetAddress clientAddress, int clientPort) {
        InetSocketAddress clientSocketAddress = new InetSocketAddress(clientAddress, clientPort);
        for (Player player : playerList) {
            if (player.getAddress().equals(clientSocketAddress)) {
                return;  // Player already exists
            }
        }
        // New player, create and add
        Player newPlayer = new Player(clientSocketAddress, new Vec2(0,playerList.size()+2), playerList.size(), 0, System.currentTimeMillis());
        playerList.add(newPlayer);
        System.out.println("New player added: " + newPlayer);
    }
}