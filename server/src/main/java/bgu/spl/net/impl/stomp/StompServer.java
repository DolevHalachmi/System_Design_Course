package bgu.spl.net.impl.stomp;

import bgu.spl.net.impl.echo.EchoProtocol;
import bgu.spl.net.impl.echo.LineMessageEncoderDecoder;
import bgu.spl.net.impl.stomp.MessageEncoderDecoderImpl;
import bgu.spl.net.impl.stomp.Frame.mainFrame;
import bgu.spl.net.impl.data.Database;
import bgu.spl.net.srv.Server;

public class StompServer {
    //mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.stomp.StompServer" -Dexec.args="7777 reactor"
    //mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.stomp.StompServer" -Dexec.args="7777 tpc"

    public static void main(String[] args) {
        // TODO: implement this
        
        if (args.length < 2) {
            System.out.println("Error: Required arguments: <port> <server_type>");
            System.exit(1);
        }

        int port = 0;
        port = Integer.parseInt(args[0]);
        String serverType = args[1];

        Database.getInstance();
        ConnectionsImpl<mainFrame> sharedConnections = new ConnectionsImpl<>();

        if (serverType.equals("tpc")) {
            Server.threadPerClient(
                port,
                () -> new StompMessagingProtocolImpl(sharedConnections), 
                () -> new MessageEncoderDecoderImpl()
            ).serve();

        } else if (serverType.equals("reactor")) {
    
            Server.reactor(
                3,           //num of threads
                port,
                () -> new StompMessagingProtocolImpl(sharedConnections), 
                () -> new MessageEncoderDecoderImpl()  
            ).serve();
        }

        
    }
}