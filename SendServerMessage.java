import Game.*;
import Game.GameData;
import Game.Vec2;
import com.google.flatbuffers.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;

public class SendServerMessage implements Runnable {
    private final int serverRefreshRate;

    public DatagramPacket sendMessage(int messageCode, int playerID) {
        FlatBufferBuilder fbb = new FlatBufferBuilder(0);
        List<Player> playerList = Server.getPlayerList();
        byte[] data;
        DatagramPacket message = null;
        if(messageCode == 2){
            int playerDataList[] = new int[playerList.size()];
            for(int i = 0; i < playerList.size(); i++) {
                Player player = playerList.get(i);
                PlayerData.startPlayerData(fbb);
                PlayerData.addPlayerId(fbb, player.getPlayerId());
                int pos = Vec2.createVec2(fbb, player.getCoordinates().getX(), player.getCoordinates().getY());
                PlayerData.addPos(fbb, pos);
                PlayerData.addTimestamp(fbb, player.getTimestampMilli());
                PlayerData.addLastProcessedSeqNumber(fbb, player.getLastProcessedSeqNum());
                playerDataList[i] = PlayerData.endPlayerData(fbb);
            }
            int playerDataVector = ServerMessage.createPlayerDataVector(fbb, playerDataList);
            ServerMessage.startServerMessage(fbb);
            ServerMessage.addMessageCode(fbb, messageCode);
            ServerMessage.addPlayerData(fbb, playerDataVector);
            ServerMessage.addPlayerId(fbb, 0);
            int serverMessage = ServerMessage.endServerMessage(fbb);
            data = makeGameMessage(fbb, serverMessage);
            // broadcast to all clients
            message = new DatagramPacket(data, data.length);
        }
        else if(messageCode == 0 || messageCode == 1){
            ServerMessage.startServerMessage(fbb);
            ServerMessage.addMessageCode(fbb, messageCode);
            ServerMessage.addPlayerId(fbb, playerID+1);
            int serverMessage = ServerMessage.endServerMessage(fbb);
            data = makeGameMessage(fbb, serverMessage);
            message = new DatagramPacket(data, data.length, playerList.get(playerID).getAddress());
        }
        return message;
    }

    private byte[] makeGameMessage(FlatBufferBuilder fbb, int serverMessage) {
        byte[] data;
        GameMessage.startGameMessage(fbb);
        GameMessage.addDataTypeType(fbb, GameData.ServerMessage);
        GameMessage.addDataType(fbb, serverMessage);
        int gameMessage = GameMessage.endGameMessage(fbb);
        fbb.finish(gameMessage);
        data = fbb.sizedByteArray();
        return data;
    }

    public SendServerMessage(int serverRefreshRate) {
        this.serverRefreshRate = serverRefreshRate;

    }

    public void run() {
        try {
            Server.udpSocket.setBroadcast(true);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        while(true){
            try{
                DatagramPacket packet = sendMessage(2, -1);
                packet.setPort(Server.serverPort);
                packet.setAddress(InetAddress.getByName("255.255.255.255"));
                Server.udpSocket.send(packet);
                Thread.sleep(serverRefreshRate);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}
