package bgu.spl.net.impl.stomp.Frame;

import java.util.ArrayList;

public class mainFrame {
    
    protected ArrayList<header> headersList;
    protected String receipt_id;
    protected String command;
    protected String body;

    
    public mainFrame(ArrayList<header> headersList, String command, String body) {
        this.headersList = headersList;
        this.receipt_id = this.getHeaderValueInList("receipt_id");
        this.command = command;
        this.body = body;
    }

    public mainFrame(String command, String body) {
        this.headersList = new ArrayList<header>();
        this.receipt_id = this.getHeaderValueInList("receipt_id");
        this.command = command;
        this.body = body;
    }

    public mainFrame(){
        this.headersList = new ArrayList<header>();
        this.receipt_id = null;
        command = null;
        body = "";
    }

    public void frameToString(){
        System.out.println(command);
        for (header header: headersList){
            System.out.println(header.getHName() + " : " + header.getHValue());
        }
        System.out.println(body);
        System.out.println("\n" + "\u0000");
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public String getReceipt_id() {
        return receipt_id;
    }

    public void setReceipt_id(String receipt_id) {
        this.receipt_id = receipt_id;
    }

    public ArrayList<header> getHeadersList(){
        return this.headersList;
    }

    public void addHeaderToList(header header){
        headersList.add(header);
    }

    public void addHeaderToList(String Hname, String Hvalue){
        headersList.add(new header(Hname, Hvalue));
    }

     public void removeHeaderFromList(String Hname){
        headersList.remove(getHeaderFromList(Hname));
    }


    public String getHeaderValueInList(String headerName){
    for(header header: headersList){
        if(header.getHName().equals(headerName)){
            return header.getHValue();
       }
    }
    return null;
    }

    public header getHeaderFromList(String headerName){
        for(header header: headersList){
            if(header.getHName().equals(headerName)){
                return header;
            }
        }
        return null;
    }


    public void clearHeaders(){
        for(header header: headersList){
            if(!header.equals(null)){
                removeHeaderFromList(header.getHName());
            }
        }
    }

    


}