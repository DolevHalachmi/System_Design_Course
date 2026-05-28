package bgu.spl.net.impl.stomp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.impl.stomp.Frame.connectedF;
import bgu.spl.net.impl.stomp.Frame.errorF;
import bgu.spl.net.impl.stomp.Frame.messageF;
import bgu.spl.net.impl.stomp.Frame.receiptF;
import bgu.spl.net.impl.stomp.Frame.mainFrame;
import bgu.spl.net.impl.data.LoginStatus;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<mainFrame> {

    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<mainFrame> connections;
    private ClientUser user;


    public StompMessagingProtocolImpl(ConnectionsImpl<mainFrame> connections) {
        this.connections = connections;
        this.connectionId = connections.createConnectionId();
    }


     @Override
    public void start (int connectionId, Connections<mainFrame> connections) {
        this.connectionId = connectionId;
        this.connections = (Connections<mainFrame>) connections;
        user = null;
    }


    @Override
    public mainFrame process(mainFrame message) {
        
        if (message == null || message.getCommand() == null){
            return null;
        }


        switch (message.getCommand()) {
            case "CONNECT":
                if(user != null && user.isConnected()){
                    return error(message.getReceipt_id(), "user already connected", message.toString());
                }
                return handleConnect(message);

            case "SEND":
                if(user != null && user.isConnected())
                return handleSend(message);

            case "SUBSCRIBE":
                if(user != null && user.isConnected())
                return handleSubscribe(message);
            
            case "UNSUBSCRIBE":
                if(user != null && user.isConnected())
                return handleUnsubscribe(message);
            
            case "DISCONNECT":
                if(user != null && user.isConnected())
                return handleDisconnect(message);
            
            default: 
                return error(message.getReceipt_id(), "malformed frame received", message.toString() + " \n destination header missing");
        }
        
    }


    @Override
    public boolean shouldTerminate() {  
        return shouldTerminate;
    }


    private mainFrame handleConnect (mainFrame frame){
        
        frame.frameToString();

        String login = frame.getHeaderValueInList("login");
        String passcode = frame.getHeaderValueInList("passcode");


        if (frame.getHeaderValueInList("accept-version") == null){
            return error(frame.getReceipt_id(), "malformed frame received", frame.toString() + "no data on accept-version");
        }

        if (frame.getHeaderValueInList("host") == null){
            return error(frame.getReceipt_id(), "malformed frame received", frame.toString() + "no data on host");
        }

        if (login == null || passcode == null){
            return error(frame.getReceipt_id(), "malformed frame received", frame.toString() + "no data on login or passcode");
        }

        LoginStatus status = bgu.spl.net.impl.data.Database.getInstance().login(connectionId, login, passcode);

        if (status == LoginStatus.LOGGED_IN_SUCCESSFULLY || status == LoginStatus.ADDED_NEW_USER){     

            ConnectionsImpl<mainFrame> con = (ConnectionsImpl<mainFrame>) connections;
            this.user = con.getUsers().computeIfAbsent(login, k -> new ClientUser(login, passcode));
            this.user.connect(connectionId);
            con.addActiveUser(connectionId, login);
            return new connectedF("1.2");
        } else if (status == LoginStatus.WRONG_PASSWORD) {
            return error(frame.getReceipt_id(), "Wrong password", "Password does not match");
        } else 
            return error(frame.getReceipt_id(), "login failed","");
    }


    private mainFrame handleSend (mainFrame frame){

       frame.frameToString();
    
        String destination = frame.getHeaderValueInList("destination");

        if (destination == null){
            
            return error(frame.getReceipt_id(), "malformed frame received", frame.toString() + " \n destination header missing");
        }
        else{
            if(!user.isSubscribedTo(destination)){
                return error(frame.getReceipt_id(), "user isnt subscribed to destination", frame.toString());
            }

            ConnectionsImpl<mainFrame> con = (ConnectionsImpl<mainFrame>) connections;
            String body = frame.getBody();
            int messageId = con.createMessageId();
            String subId = frame.getHeaderValueInList("id");

            messageF toSend = new messageF(destination, subId, String.valueOf(messageId), body);

            connections.send(destination, toSend);
            
            receiptF receipt = new receiptF(frame.getReceipt_id());

            bgu.spl.net.impl.data.Database.getInstance().trackFileUpload(user.getUsername(), "events.json", destination);

            if (frame.getReceipt_id() != null)
            return receipt;
        }
        return null;
    }



    private mainFrame handleSubscribe (mainFrame frame){

        frame.frameToString();
        
        String destination = frame.getHeaderValueInList("destination");
        if (destination == null){
            return error(frame.getReceipt_id(), "malformed frame received", frame.toString() + " \n destination header missing");
        }
        String subId = frame.getHeaderValueInList("id");
        if (subId == null){
            return error(frame.getReceipt_id(), "malformed frame received", frame.toString() + " \n id header missing");
        }
        if(user.isSubscribedTo(destination)){
            return error(frame.getReceipt_id(), "already subscribed to " + user.getTopicOfId(subId) , frame.toString());
        }
        user.subscribe(destination, subId);

        ConcurrentHashMap<String, ConcurrentLinkedQueue<ClientUser>> channels = ((ConnectionsImpl<mainFrame>)connections).getChannels();

        channels.computeIfAbsent(destination, k -> new ConcurrentLinkedQueue<>()).add(user);


        if(frame.getReceipt_id() != null){
            return new receiptF(frame.getReceipt_id());
        }
        return null;
    }

    private mainFrame handleUnsubscribe (mainFrame frame){

        frame.frameToString();


    String subId = frame.getHeaderValueInList("id");
    if (subId == null){
        return error(frame.getReceipt_id(), "malformed frame received", frame.toString() + " \n id header missing");
    }
    
    String topic = user.getTopicOfId(subId);
    if(topic == null){
        return error(frame.getReceipt_id(), "already unSubscribed from " + user.getTopicOfId(subId) , frame.toString());
    }

    user.unSubscribe(subId);

    ConnectionsImpl<mainFrame> con = (ConnectionsImpl<mainFrame>) connections;  
    ConcurrentLinkedQueue<ClientUser> channelSubs = con.getChannels().get(topic);
    if (!channelSubs.isEmpty())
        channelSubs.remove(user);

    if(frame.getReceipt_id() != null)
        return new receiptF(frame.getReceipt_id());

    return null;
}


    private mainFrame handleDisconnect (mainFrame frame){

        frame.frameToString();

        String receiptID = frame.getReceipt_id();

        bgu.spl.net.impl.data.Database.getInstance().logout(connectionId);

        shouldTerminate = true;

        
        if (receiptID != null){

            receiptF receipt = new receiptF(frame.getReceipt_id());
            return receipt;
        }
        
        return null;

    }


    private mainFrame error (String receiptID, String errorMessage, String body){

        shouldTerminate = true;
        return new errorF(receiptID, errorMessage, body, connections, connectionId);
    }

  
}