package com.aderversa;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ParseFtpCommand extends ChannelInboundHandlerAdapter {
    static EventLoopGroup tasks;
    FtpServerConfig ftpServerConfig;
    static Map<ChannelHandlerContext, ClientMessage> attachment;
    static Bootstrap bootstrap;

    ParseFtpCommand() {
        ftpServerConfig = new FtpServerConfig();
        ftpServerConfig.addUserMap("aderversa", "123456");
        ftpServerConfig.addUserMap("teacher", "123456");
        ftpServerConfig.addUserMap("student", "123456");
        bootstrap = new Bootstrap()
                .channel(NioSocketChannel.class)
                .group(new NioEventLoopGroup())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                    }
                });

        attachment = new HashMap<>();
        tasks = new NioEventLoopGroup();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(220);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        String commandLine = buf.toString(ftpServerConfig.getFtpCharset());
        // 使用空格作为分隔符
        String[] parts = commandLine.split("\\s+"); // \\s+ 匹配一个或多个空白字符
        // 第一个元素是命令，其余的是参数
        String command = parts[0];
        if (command.equals("OPTS")) {
            OPTS(ctx, parts[1], parts[2]);
        }
        else if (command.equals("USER")) {
            USER(ctx, parts[1]);
        }
        else if (command.equals("PASS")) {
            PASS(ctx, parts[1]);
        }
        else if (command.equals("PORT")) {
            if (parts[1] == null) {
                ctx.writeAndFlush(502);
            }
            else {
                String[] nums = parts[1].split(",");
                Integer port = Integer.parseInt(nums[4]) * 256 + Integer.parseInt(nums[5]);
                String address = nums[0] + "." + nums[1] + "." + nums[2] + "." + nums[3];
                PORT(ctx, port, address);
            }
        }
        else if (command.equals("NLST")) {
            NLST(ctx);
        }
        else if (command.equals("CWD")) {
            CWD(ctx, parts[1]);
        }
        else if (command.equals("RETR")) {
            RETR(ctx, parts[1]);
        }
        else if (command.equals("XPWD")) {
            XPWD(ctx);
        }
        else if (command.equals("QUIT")) {
            ctx.close();
        }
        else {
            ctx.writeAndFlush(502);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn(cause.getMessage());
        ctx.close();
    }

    private void OPTS(ChannelHandlerContext ctx, String charset, String option) {
        if (charset != null) {
            if (charset.equals("UTF8")) {
                ftpServerConfig.setFtpCharset(StandardCharsets.UTF_8);
                ctx.writeAndFlush(200);
            }
        }
    }
    private void USER(ChannelHandlerContext ctx, String username) {
        ClientMessage clientMessage = new ClientMessage();
        clientMessage.setUsername(username);
        attachment.put(ctx, clientMessage);
        ctx.writeAndFlush(331); // 331：输入密码
    }
    private void PASS(ChannelHandlerContext ctx, String password) {
        String username = attachment.get(ctx).getUsername();
        String recordPassword = ftpServerConfig.getPassword(username);
        if (recordPassword == null) { // 对应用户不存在
            ctx.writeAndFlush(530);
        }
        else if (recordPassword.equals(password)) { // 用户存在且密码正确
            ctx.writeAndFlush(230);
        }
        else { // 用户存在但密码不正确
           ctx.writeAndFlush(530);
        }
    }
    private void PORT(ChannelHandlerContext ctx, Integer port, String address) {
        try {
            ClientMessage clientMessage = attachment.get(ctx);
            clientMessage.setPort(port);
            clientMessage.setAddress(InetAddress.getByName(address));
            ctx.writeAndFlush(200);
        } catch (UnknownHostException e) {
            ctx.writeAndFlush(502);
            log.warn(e.getMessage());
        }
    }
    private void NLST(ChannelHandlerContext ctx) {
        StringBuilder result = new StringBuilder();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(ftpServerConfig.getPath())) {
            for (Path file : directoryStream) {
                File tmp = new File(file.toFile().getAbsolutePath());
                if (tmp.isDirectory()) {
                    result.append("\033[1;34m");
                    result.append(file.getFileName().toString()).append("\r\n");
                    result.append("\033[0m");
                }
                else {
                    result.append(file.getFileName().toString()).append("\r\n");
                }
            }
            ByteBuf data = ByteBufAllocator.DEFAULT.buffer();
            data.writeBytes(result.toString().getBytes(ftpServerConfig.getFtpCharset()));
            InetAddress address = attachment.get(ctx).getAddress();
            Integer port = attachment.get(ctx).getPort();
            ChannelFuture connect = bootstrap.connect(address, port);
            connect.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    ctx.writeAndFlush(150);
                    future.channel().writeAndFlush(data).addListener((ChannelFutureListener) future1 -> {
                        if(future1.isSuccess()) {
                            ctx.writeAndFlush(226);
                            future1.channel().close();
                        }
                    });
                }
            });
        }
        catch (IOException e) {
            log.warn(e.getMessage());
        }
    }
    private void CWD (ChannelHandlerContext ctx, String filename) {
        if (filename == null) {
            log.warn("CWD filename is null.");
            return;
        }
        if (filename.equals("..")) {
            ftpServerConfig.setPath(ftpServerConfig.getPath().getParent());
            ctx.writeAndFlush(250);
        }
        else {
            ftpServerConfig.setPath(ftpServerConfig.getFileByName(filename));
            ctx.writeAndFlush(250);
        }
    }
    private void RETR(ChannelHandlerContext ctx, String filename) {
        Path file = ftpServerConfig.getFileByName(filename);
        if (file == null) {
            log.warn("RETR {} isn't exist.", filename);
            ctx.writeAndFlush(550);
            return;
        }
        File target = new File(file.toAbsolutePath().toString());
        FileRegion region = new DefaultFileRegion(target, 0, target.length());
        ChannelFuture connectFuture = bootstrap.connect(attachment.get(ctx).getAddress(), attachment.get(ctx).getPort());
        connectFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                ctx.writeAndFlush(150);
                future.channel().writeAndFlush(region).addListener((ChannelFutureListener) future1 -> {
                    if(future1.isSuccess()) {
                        ctx.writeAndFlush(226);
                        future1.channel().close();
                    }
                });
            }
        });
    }
    private void XPWD(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new Response(257, ftpServerConfig.getPath().toAbsolutePath().toString()));
    }
}
