package com.aderversa;

import lombok.Data;

import java.net.InetAddress;

@Data
public class ClientMessage {
    String username;
    InetAddress address;
    Integer port;
}
