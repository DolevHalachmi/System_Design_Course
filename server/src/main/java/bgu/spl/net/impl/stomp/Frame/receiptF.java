package bgu.spl.net.impl.stomp.Frame;

public class receiptF extends mainFrame{

    public receiptF(String receipt_id){
    super();
    setCommand("RECEIPT");
    addHeaderToList("receipt-id", receipt_id);
    setBody("");
    }
    
}