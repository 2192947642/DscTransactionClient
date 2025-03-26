package com.lgzClient.handlers.netty;

import com.lgzClient.service.LocalTransactionManager;
import com.lgzClient.types.GlobalNotice;
import com.lgzClient.types.Message;
import com.lgzClient.types.TransactContainer;
import com.lgzClient.types.sql.service.BranchTransaction;
import com.lgzClient.types.status.MessageTypeEnum;
import com.lgzClient.utils.JsonUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.sql.SQLException;

public class MessageHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String string) throws Exception {
        Message msg = JsonUtil.jsonToObject(string, Message.class);
        if (msg.getType() == MessageTypeEnum.GlobalNotice) {
            handleGlobalNotice(msg);
        }
    }

    //收到全局的事务状态通知
    public void handleGlobalNotice(Message message) throws SQLException {
        GlobalNotice globalNotice = JsonUtil.jsonToObject(message.getContent(), GlobalNotice.class);
        LocalTransactionManager instance = LocalTransactionManager.instance;
        TransactContainer transactContainer = instance.getTransactionContainerById(globalNotice.getBranchId());
        if (transactContainer == null) return;//如果没有事务容器,那么就直接返回
        if (globalNotice.getIsSuccess())//如果全局事务成功 那么就提交事务
            instance.commitByThreadPoolAndWebFlux(transactContainer.getBranchTransaction());
        else instance.rollBackByThreadPoolAndWebFlux(transactContainer.getBranchTransaction());//回滚事务


    }
}
