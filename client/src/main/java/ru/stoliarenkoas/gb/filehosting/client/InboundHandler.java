package ru.stoliarenkoas.gb.filehosting.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.stoliarenkoas.gb.filehosting.common.MessageType;

public class InboundHandler extends ChannelInboundHandlerAdapter {
    private static final MessageType[] MESSAGE_TYPES = MessageType.values();
    private MessageType stage = null;
    private boolean stageFinished = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) return;
        ByteBuf byteBuf = (ByteBuf)msg;

        if (stage == null) {
            byte typeByte = byteBuf.readByte();
            System.out.printf("Type byte received: %d%n", typeByte);
            stage = MESSAGE_TYPES[typeByte];
        }

        switch (stage) {
            case HANDSHAKE_RESPONSE: {
                break;
            }
            case LOGIN_RESPONSE: {
                break;
            }
            case LOGOUT_RESPONSE: {
                break;
            }
            case REGISTER_RESPONSE: {
                break;
            }
            case GET_FILE_LIST_RESPONSE: {
                break;
            }
            case LOCATION_CHANGE_RESPONSE: {
                break;
            }
            case FILE_UPLOAD_RESPONSE: {
                break;
            }
            case FILE_DOWNLOAD_RESPONSE: {
                break;
            }
            case FILE_DELETE_RESPONSE: {
                break;
            }
            case FILE_RENAME_RESPONSE: {
                break;
            }

        }

        if (stageFinished) {
            stage = null;
            stageFinished = false;
        }

    }

}
