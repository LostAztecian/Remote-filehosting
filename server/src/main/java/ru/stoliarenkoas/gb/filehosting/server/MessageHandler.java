package ru.stoliarenkoas.gb.filehosting.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.stoliarenkoas.gb.filehosting.common.Message;

import java.util.Arrays;

public class MessageHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final Message message = (Message)msg;
        System.out.printf("Received message: type %s, part %d of %d%nPayload: %s", message.getType(), message.getPart(), message.getParts(), Arrays.toString(message.getBytes()));
    }
}
