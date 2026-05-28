package bgu.spl.net.impl.stomp.Frame;

public class messageF extends mainFrame{

    public messageF(String destination, String subscription, String messageID, String body){
        super();
        setCommand("MESSAGE");
        addHeaderToList("destination", destination);
        addHeaderToList("subscription", subscription);
        addHeaderToList("message-id", messageID);
        setBody(body);
    }
}
