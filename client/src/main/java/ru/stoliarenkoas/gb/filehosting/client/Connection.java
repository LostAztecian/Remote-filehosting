package ru.stoliarenkoas.gb.filehosting.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import java.net.InetSocketAddress;

public class Connection {

    private static Connection ourInstance = new Connection();
    public static Connection getInstance() {
        return ourInstance;
    }
    private Connection() {
        messageSender = new MessageSender(this);
    }

    private static final InetSocketAddress SERVER_ADDRESS = new InetSocketAddress("127.0.0.1", 7637);
    @Getter private Channel currentChannel;
    @Getter private final MessageSender messageSender;

    public void start() {
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(mainGroup);
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.remoteAddress(SERVER_ADDRESS);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(new InboundHandler());
                    currentChannel = socketChannel;
                }
            });
            ChannelFuture cFuture = bootstrap.connect().sync();
            cFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mainGroup.shutdownGracefully();
        }
    }

    public void close() {
        currentChannel.close();
        currentChannel = null;
    }

}
