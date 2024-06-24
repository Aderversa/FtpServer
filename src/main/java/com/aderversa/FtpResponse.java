package com.aderversa;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class FtpResponse extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer(256);
        Integer responseCode = null;
        String message = null;
        if (msg instanceof Integer) {
            responseCode = (Integer) msg;
        }
        else if (msg instanceof Response) {
            responseCode = ((Response) msg).getCode();
            message = ((Response) msg).getMessage();
        }
        String NEWLINE = "\r\n";
        switch (responseCode) {
            case 150:
                buf.writeBytes((responseCode  + " Prepare to establish a data connection for data transfer." + NEWLINE).getBytes());
                break;
            case 200:
                buf.writeBytes((responseCode  + " The command has been received and understood correctly." + NEWLINE).getBytes());
                break;
            case 220:
                buf.writeBytes((responseCode  + " Server is ready." + NEWLINE).getBytes());
                break;
            case 226:
                buf.writeBytes((responseCode  + " Close data connection, request success." + NEWLINE).getBytes());
                break;
            case 230:
                buf.writeBytes((responseCode  + " Login successfully." + NEWLINE).getBytes());
                break;
            case 250:
                buf.writeBytes((responseCode  + " file tansfer request is successful." + NEWLINE).getBytes());
                break;
            case 257:
                String tmp = responseCode + " \"" + message + "\"" + " is the current directory." + NEWLINE;
                buf.writeBytes(tmp.getBytes());
                break;
            case 331:
                buf.writeBytes((responseCode  + " Please specify the password." + NEWLINE).getBytes());
                break;
            case 530:
                buf.writeBytes((responseCode  + " Login incorrect." + NEWLINE).getBytes());
                break;
            case 502:
                buf.writeBytes((responseCode  + " Command is not exist." + NEWLINE).getBytes());
                break;
            default:
                buf.writeBytes((responseCode  + " Error Command" + NEWLINE).getBytes());
                break;
        }
        super.write(ctx, buf, promise);
    }
}
