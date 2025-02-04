package com.lgzClient.service;

import com.lgzClient.NettyClient;
import com.lgzClient.redis.MsgReceiveHelper;
import com.lgzClient.types.Message;
import com.lgzClient.utils.TimeUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.concurrent.LinkedBlockingDeque;

//消息重发服务
@Service
public class TractSqlReSendService {
    public static final int fixedDelay=3000;
    public static TractSqlReSendService instance;
    @PostConstruct
    public void init(){
        instance=this;
    }
    public void addToResendQueue(Message msg){
        blockingDeque.add(msg);
    }
    public int reSendInterval=5;
    @Autowired
    MsgReceiveHelper msgReceiveHelper;
    private final LinkedBlockingDeque<Message>blockingDeque=new LinkedBlockingDeque<>();
    //消息重发服务
    @Scheduled(fixedDelay = TractSqlReSendService.fixedDelay)
    public void reSend() throws ParseException {
        String now= TimeUtil.getLocalTime();
        while(true){
            Message message=blockingDeque.poll();//将第一个消息从队列中取出
            if(message==null) break;
            boolean isReceive=msgReceiveHelper.getMessageReceive(message.getMsgId());//通过message的id判断当前的服务端是否收到了该消息
            if(isReceive){//如果是收到了消息,则删除该消息确认
                msgReceiveHelper.deleteMessageReceive(message.getMsgId());
            }
            else{//如果没有收到消息 那么就判断是否满足再次发送的条件
                String lastSendTime= message.getLastSendTime();
                if(TimeUtil.getPastSeconds(now,lastSendTime)>=reSendInterval){//如果当前到达了重发的时间间隔那么进行重发
                    message.setLastSendTime(now);
                    NettyClient.sendMsg(message,true);
                }else{
                    blockingDeque.add(message);
                    break;//否则就break这个循环
                }
            }

        }
    }

}
