package com.lgzClient.types;

import com.lgzClient.types.status.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class Message {
    public Message(){
        this.msgId= UUID.randomUUID().toString();
    }
    public Message(MessageTypeEnum type, String content,String sendTime){
        this();
        this.type=type;
        this.content=content;
        this.sendTime=sendTime;
        this.lastSendTime=sendTime;
    }
    public String msgId;
    public MessageTypeEnum type;
    public String content;
    public String sendTime;
    public String lastSendTime;
}
