package bgu.spl.net.impl.stomp.Frame;

public class header {
    private String headerName;
    private String headerValue;


    public header(String name, String value){
        this.headerName = name;
        this.headerValue = value;
    }

    public void setHName(String string){
        this.headerName = string;
    }



    public String getHName(){
        return this.headerName;
    }

    

    public void setHValue(String string){
        this.headerValue = string;
    }
    


    public String getHValue() {
        return this.headerValue;
    }


}
