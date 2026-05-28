package bgu.spl.net.impl.stomp.Frame;

public class connectedF extends mainFrame {



    
    public connectedF(String version) {
    super();
    setCommand("CONNECTED");
    addHeaderToList("version", version);
    setBody("");
    }
}
