package com.lgzClient.types;

import com.lgzClient.utils.AddressUtil;
import lombok.Data;

@Data
public class ServerAddress {

    public static ServerAddress buildServerAddress(String ip,String port)
    {
        String a= AddressUtil.buildAddress(ip,port);
        ServerAddress address=new ServerAddress();
        address.serverAddress=a;
        return address;
    };
    public ServerAddress(){

    }
    private String serverAddress;
}
