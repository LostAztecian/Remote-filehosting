package ru.stoliarenkoas.gb.filehosting.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.stoliarenkoas.gb.filehosting.common.MessageType;

import java.util.HashMap;
import java.util.Map;

public class InboundHandler extends ChannelInboundHandlerAdapter {
    private static final MessageType[] MESSAGE_TYPES = MessageType.values();
    private Map<Channel, UserHandler> handlers = new HashMap<>();
    private MessageType stage = null;
    private boolean stageFinished = false;

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        handlers.put(ctx.channel(), new UserHandler());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) return;
        ByteBuf byteBuf = (ByteBuf)msg;

        if (stage == null) {
            byte typeByte = byteBuf.readByte();
            System.out.print("Type byte received: " + typeByte);
            stage = MESSAGE_TYPES[typeByte];
            System.out.println(" --> type is " + stage);
        }

        switch (stage) {
            case HANDSHAKE: {
                stageFinished = handlers.get(ctx.channel()).handshake(ctx, byteBuf);
                break;
            }
            case LOGIN: {
                stageFinished = handlers.get(ctx.channel()).login(ctx, byteBuf);
                break;
            }
            case LOGOUT: {
                stageFinished = handlers.get(ctx.channel()).logout(ctx, byteBuf);
                break;
            }
            case REGISTER: {
                stageFinished = handlers.get(ctx.channel()).register(ctx, byteBuf);
                break;
            }
            case GET_FILE_LIST: {
                stageFinished = handlers.get(ctx.channel()).sendFileList(ctx, byteBuf);
                break;
            }
            case LOCATION_CHANGE: {
                stageFinished = handlers.get(ctx.channel()).changeLocation(ctx, byteBuf);
                break;
            }
            case FILE_UPLOAD: {
                stageFinished = handlers.get(ctx.channel()).uploadFile(ctx, byteBuf);
                break;
            }
            case FILE_DOWNLOAD: {
                stageFinished = handlers.get(ctx.channel()).downloadFile(ctx, byteBuf);
                break;
            }
            case FILE_DELETE: {
                stageFinished = handlers.get(ctx.channel()).removeFile(ctx, byteBuf);
                break;
            }
            case FILE_RENAME: {
                stageFinished = handlers.get(ctx.channel()).renameFile(ctx, byteBuf);
                break;
            }
        }

        if (stageFinished) {
            stage = null;
            stageFinished = false;
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Exception caught: ");
        cause.printStackTrace();
    }

}
