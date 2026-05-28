package bgu.spl.net.impl.stomp;


import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import bgu.spl.net.impl.stomp.Frame.mainFrame;
import bgu.spl.net.impl.stomp.Frame.messageF;
import bgu.spl.net.impl.stomp.Frame.errorF;
import bgu.spl.net.impl.stomp.Frame.header;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;

public class ConnectionsImpl<T> implements Connections<T> {

    private AtomicInteger connectionId;
    private AtomicInteger messageId;
    private ConcurrentHashMap<Integer, ConnectionHandler<T>> connectionsHandlers;
    private ConcurrentHashMap<String, ClientUser> users;
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<ClientUser>> channels;
    private final ConcurrentHashMap<Integer, String> activeUsers;

    public ConnectionsImpl() {
        this.connectionId = new AtomicInteger(0);
        this.messageId = new AtomicInteger(0);
        this.connectionsHandlers = new ConcurrentHashMap<>();
        this.users = new ConcurrentHashMap<>();
        this.channels = new ConcurrentHashMap<>();
        this.activeUsers = new ConcurrentHashMap<>();
    }


    @Override
    public void send(String channel, T msg) {

        if(!(msg instanceof mainFrame)){
            throw new IllegalArgumentException(msg.toString() + " \n is not a frame");
        }
        mainFrame frame = (mainFrame) msg;
        String body = frame.getBody();

        ConcurrentLinkedQueue<ClientUser> subscribers = channels.get(channel);
        if (subscribers == null) return;

        for (ClientUser user : subscribers) {
            if (user.isConnected()) {
                String subId = user.getSubscriptionId(channel);

                if (subId != null){
                    int connectionID = user.getConnectionId();
                    int msgID = createMessageId();
                    
                    messageF sendFrame = new messageF(channel, subId, String.valueOf(msgID), body);
                    send(connectionID, (T)sendFrame);
                }
            }
        }
    }

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = connectionsHandlers.get(connectionId);
        if (handler == null){
            return false;
        }
        
        handler.send(msg);
        return true;
    }

    @Override
    public void disconnect(int connectionId) {
        this.getConnectionsHandlers().remove(connectionId);

        String username = activeUsers.remove(connectionId);
        if (username != null) {
            ClientUser user = users.get(username);
            if (user != null) {
                user.disconnect();
            }
        }
    }

    public void addConnection(int connectionId, ConnectionHandler<T> handler) {
        connectionsHandlers.put(connectionId, handler);
    }
    
    public void addActiveUser(int connectionId, String username) {
        activeUsers.put(connectionId, username);
    }

    public int createConnectionId() {
        return connectionId.incrementAndGet();
    }

    public int createMessageId() {
        return messageId.incrementAndGet();
    }

    public ConcurrentHashMap<String, ClientUser> getUsers() {
        return users;
    }

    public ConcurrentHashMap<Integer, ConnectionHandler<T>> getConnectionsHandlers() {
        return connectionsHandlers;
    }

    public ConcurrentHashMap<String, ConcurrentLinkedQueue<ClientUser>> getChannels() {
        return channels;
    }
}