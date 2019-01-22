package ru.stoliarenkoas.gb.filehosting.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import ru.stoliarenkoas.gb.filehosting.common.Message;

public class MessageToByteHandler extends ChannelOutboundHandlerAdapter {
    ByteBufAllocator allocator = new PooledByteBufAllocator();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Message message = (Message)msg;
        byte[] bytes = message.getBytes();
        ByteBuf byteBuf = allocator.buffer(bytes.length);

        byteBuf.writeBytes(bytes);
        ctx.writeAndFlush(byteBuf);
        byteBuf.release();
    }
}
