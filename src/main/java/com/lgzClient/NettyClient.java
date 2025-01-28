package com.lgzClient;

import com.lgzClient.service.TractSqlReSendService;
import com.lgzClient.types.Message;
import com.lgzClient.utils.Emitter;
import com.lgzClient.utils.JsonUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
public class NettyClient {
    public static NettyClient buildClient(String ip,Integer port){
        if(clientMaps.get(new InetSocketAddress(ip,port).toString())!=null) return null;
        NettyClient nettyClient=new NettyClient(ip,port);
        nettyClient.connect();
        return nettyClient;
    }
    private static final ConcurrentHashMap<String,NettyClient> clientMaps=new ConcurrentHashMap<>();
    private static int number=0;//轮询发送消息
    //发送消息
    public static void sendMsg(Message message,boolean addToResendQueue){
       NettyClient client= getActiveNettyClientByPoll();
       client.sendMessage(message,addToResendQueue);//发送消息
    };
    public static NettyClient getActiveNettyClientByPoll(){
        if (clientMaps.isEmpty()) {
            throw new RuntimeException("当前没有连接的事务管理服务端");
        }
        Object[]keys= clientMaps.keySet().toArray();
        int originAlVal=(number)%keys.length;//初始index
        int index=originAlVal;
        number++;
        while(true){
            NettyClient nettyClient= clientMaps.get(keys[index]);
            if(nettyClient.connection!=null&&nettyClient.connection.isActive()){
                return nettyClient;
            }
            index=(index+1)%keys.length;
            if(number==originAlVal){
                throw new RuntimeException("当前所有的事务管理服务端都未成功连接");
            }
       }
    }
    Executor executor=Executors.newSingleThreadExecutor();
    private EventLoopGroup group;
    private  Bootstrap bootstrap;
    private Channel connection;
    private final String ip;//要连接的服务端的ip
    private final int port;//要连接的服务段的port
    public void sendMessage(Message msg,boolean addToResendQueue){
        if(msg==null||connection==null){
            return;
        }
        String str=JsonUtil.objToJson(msg);
        connection.writeAndFlush(str);
        if (addToResendQueue)
        TractSqlReSendService.instance.addToResendQueue(msg);//添加到重新上传
    }

    private NettyClient(String ip,int port){
        this.ip=ip;
        this.port=port;
        this.group=new NioEventLoopGroup();
        this.bootstrap=new Bootstrap();
        this.bootstrap.group(group);
        this.bootstrap.channel(NioSocketChannel.class);
        this.bootstrap.handler(new ClientChannelInit());
        clientMaps.put(new InetSocketAddress(ip,port).toString(),this);
    }
    public Channel connect()   {
        try {
            if(this.connection!=null&&connection.isActive()){
                return connection;
            }
            ChannelFuture channelFuture =bootstrap.connect(new InetSocketAddress(ip,port)).sync();
            if(channelFuture.isSuccess()){
                connection=channelFuture.channel();
                System.out.println(connection.localAddress());
                NettyClient.clientMaps.put(new InetSocketAddress(ip,port).toString(),this);//连接成功后 添加到连接池中
                Emitter.emit(Emitter.Event.Success);
            }
            connection.closeFuture().addListener((ChannelFuture future)->{//关闭后
                 clientMaps.remove(new InetSocketAddress(ip,port).toString());
                 reconnect();//失败后重连
            });
            return connection;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    private void reconnect(){
        executor.execute(()->{
            int count=0;
            while(count++< ClientConfig.maxReconnectAttempts){
                try{
                    Thread.sleep(ClientConfig.reconnectInterval);
                    Channel channel=connect();
                    if(channel!=null&&channel.isActive()){
                        break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
