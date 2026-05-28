package bgu.spl.net.impl.stomp;

import java.util.ArrayList;


import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.stomp.Frame.mainFrame;
import bgu.spl.net.impl.stomp.Frame.header;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MessageEncoderDecoderImpl implements MessageEncoderDecoder<mainFrame> {
    
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    @Override
    public mainFrame decodeNextByte(byte nextByte) {
        if (nextByte == '\u0000') {
            String clientOutput = popString();

            ArrayList<header> headers = stringToHeaders(clientOutput);       

            String command = stringToCommand(clientOutput);

            String body = stringTobody(clientOutput);

            if (headers == null){
                return null;
            }
            mainFrame outputFrame = new mainFrame(headers, command, body);
            return outputFrame;

        }

        pushByte(nextByte);
        return null; //not a line yet
    }

    private String stringToCommand(String string){
        String[] lines = string.split("\n");
        if (lines[0].equals("\u0000") || lines.length == 0){
            return null;
        }
        return lines[0].trim();
    }

    private String stringTobody(String string){
    
        String[] lines = string.split("\n");
        if (lines[0].equals("\u0000") || lines.length == 0){
            return null;
        }
        int lineIndex = 1;
        while (lineIndex < lines.length && !lines[lineIndex].isEmpty()){
            lineIndex++;
        }
        lineIndex++;
        StringBuilder body = new StringBuilder();
        while (lineIndex < lines.length){
            body.append(lines[lineIndex]);
            if(lineIndex < lines.length-1){
                body.append("\n");
            }
            lineIndex++;
        }
        return body.toString().trim();

    }
        
    private ArrayList<header> stringToHeaders(String string){

        ArrayList<header> headersList = new ArrayList<header>();

        String[] lines = string.split("\n");
        if (lines[0].equals("\u0000") || lines.length == 0){
            return null;
        }

        int lineIndex = 1;
        while (lineIndex < lines.length && !lines[lineIndex].isEmpty()){

            String line = lines[lineIndex];
            int headerParter = line.indexOf(":");
            if(headerParter != -1){
                String Hname = line.substring(0, headerParter).trim();
                String Hvalue = line.substring(headerParter+1).trim();
                headersList.add(new header(Hname, Hvalue));
            }
            
            lineIndex++;
        }

        return headersList;
    }


    @Override
    public byte[] encode(mainFrame message) {

        StringBuilder text = new StringBuilder();
        text.append(message.getCommand()+"\n");
        
        ArrayList<header> headers = message.getHeadersList();
        for (int hIndex = 0; hIndex < headers.size(); hIndex++){
            text.append(headers.get(hIndex).getHName() + ":" + headers.get(hIndex).getHValue() + "\n");
        }

        text.append("\n" + message.getBody() + "\n");


        return (text.toString() + "\n" + "\u0000").getBytes(); //uses utf8 by default
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len++] = nextByte;
    }

    private String popString() {
        //notice that we explicitly requesting that the string will be decoded from UTF-8
        //this is not actually required as it is the default encoding in java.
        String result = new String(bytes, 0, len, StandardCharsets.UTF_8);
        len = 0;
        return result;
    }
}