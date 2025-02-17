package com.lgzClient;

import com.lgzClient.handlers.netty.ActiveHandler;
import com.lgzClient.handlers.netty.ExceptionHandler;
import com.lgzClient.handlers.netty.MessageHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class ClientChannelInit  extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline=socketChannel.pipeline();
        pipeline.addLast(new LengthFieldPrepender(2,0,false));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(65535,0,2,0,2));
        pipeline.addLast("decoder", new StringDecoder());
        pipeline.addLast("encoder",new StringEncoder());
        pipeline.addLast(new MessageHandler());
        pipeline.addLast(new ActiveHandler());
        pipeline.addLast(new ExceptionHandler());
    }
}
