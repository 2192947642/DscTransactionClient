package com.lgzClient.handlers;

import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.GlobalNotice;
import com.lgzClient.types.Message;
import com.lgzClient.types.status.MessageTypeEnum;
import com.lgzClient.utils.JsonUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.sql.SQLException;

public class MessageHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String string) throws Exception {
        Message msg= JsonUtil.jsonToObject(string,Message.class);
        if(msg.type== MessageTypeEnum.GlobalNotice){
            handleGlobalNotice(msg);
        }
    }
    //收到全局的事务状态通知
    public void handleGlobalNotice(Message message) throws SQLException {
        GlobalNotice globalNotice= JsonUtil.jsonToObject(message.content,GlobalNotice.class);
        if(globalNotice.isSuccess)//如果全局事务成功 那么就提交事务
            LocalTransactionManager.instance.commit(globalNotice.localType);
        else LocalTransactionManager.instance.rollBack(globalNotice.localType);//回滚事务
    }
}
