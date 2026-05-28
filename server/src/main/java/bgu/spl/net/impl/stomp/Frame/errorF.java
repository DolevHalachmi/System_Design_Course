package bgu.spl.net.impl.stomp.Frame;

import bgu.spl.net.impl.stomp.ConnectionsImpl;
import bgu.spl.net.srv.Connections;

public class errorF extends mainFrame {

    public errorF(String receiptID, String errorMessage, String body, Connections<mainFrame> connections, int connectionId){
    super();
    
    setCommand("ERROR");
    addHeaderToList("receipt-id", receiptID);
    addHeaderToList("message", errorMessage);
    setBody(body);
    }

}
