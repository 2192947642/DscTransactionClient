package com.lgzClient.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

//@Component
public class MsgReceiveHelper {
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    private final String prefix="receive:";//客户端收到消息的key localId//事务的id
    public void setMessageReceive(boolean receive,String msgId){
        stringRedisTemplate.opsForValue().set(prefix+msgId, String.valueOf(receive));
    }
    public boolean getMessageReceive(String msgId){
        return Boolean.valueOf(stringRedisTemplate.opsForValue().get(prefix+msgId));
    }
    public void deleteMessageReceive(String msgId){
        stringRedisTemplate.delete(prefix+msgId);
    }

}
