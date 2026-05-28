package bgu.spl.net.impl.stomp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;


//i didnt see the user.java file in data so i made my own one



public class ClientUser {
    
    private final String username;
    private final String password;
    private boolean connected;
    private int connectionId;
    private final Map<String, String> topicOfId;
    private final Map<String, String> IdOfTopic;

    public ClientUser(String username, String password) {
        this.username = username;
        this.password = password;
        this.connected = false;
        this.connectionId = -1;
        this.topicOfId = new ConcurrentHashMap<>();
        this.IdOfTopic = new ConcurrentHashMap<>();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public void connect(int connectionId){
        this.connectionId = connectionId;
        this.connected = true;
    }
    
    public void disconnect(){
        this.connectionId = -1;
        this.connected = false;
        topicOfId.clear();
        IdOfTopic.clear();
    }

    
    public void subscribe(String topic, String subId) {
        topicOfId.put(subId, topic);
        IdOfTopic.put(topic, subId);
    }

    public void unSubscribe(String subId) {
        String topic = topicOfId.remove(subId);
        if (topic != null) {
            IdOfTopic.remove(topic);
        }
    }

    public boolean isSubscribedTo(String topic) {
        return IdOfTopic.containsKey(topic);
    }

    public String getSubscriptionId(String topic) {
        return IdOfTopic.get(topic);
    }

    public String getTopicOfId(String id){
        return topicOfId.get(id);
    }
}