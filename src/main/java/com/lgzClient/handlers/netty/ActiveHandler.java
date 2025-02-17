package com.lgzClient.handlers.netty;

import com.lgzClient.types.Message;
import com.lgzClient.types.ServerAddress;
import com.lgzClient.types.status.MessageTypeEnum;
import com.lgzClient.utils.AddressUtil;
import com.lgzClient.utils.JsonUtil;
import com.lgzClient.utils.TimeUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ActiveHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String ip=AddressUtil.getIp();
        String address=AddressUtil.buildAddress(ip);
        ServerAddress serverAddress=new ServerAddress();
        serverAddress.setServerAddress(address);
        Message message=new Message(MessageTypeEnum.ServerAddress, JsonUtil.objToJson(serverAddress),TimeUtil.getLocalTime());
        ctx.channel().writeAndFlush(JsonUtil.objToJson(message));
    }
}
