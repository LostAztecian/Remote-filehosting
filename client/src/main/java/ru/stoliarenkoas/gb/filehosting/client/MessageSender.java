package ru.stoliarenkoas.gb.filehosting.client;

import com.sun.istack.internal.NotNull;
import lombok.RequiredArgsConstructor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import ru.stoliarenkoas.gb.filehosting.common.MessageType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
public final class MessageSender {

    private final static int BUFFER_SIZE = 256;
    private final Connection connection;
    private ByteBuf byteBuf = new PooledByteBufAllocator().buffer(BUFFER_SIZE);

    /**
     * Sending type(1b) + length(1b) + message(up to 127b)
     */
    public void sendHandshake() {
        final String greetings = "Client ver:meow asking pur-r-r-mission to land!";
        byteBuf.clear();

        byteBuf.writeByte((byte)MessageType.HANDSHAKE.ordinal());
        byteBuf.writeByte((byte)greetings.getBytes().length);
        byteBuf.writeBytes(greetings.getBytes());

        byteBuf.retain();
        connection.getCurrentChannel().writeAndFlush(byteBuf);
    }

    public void sendLoginRequest(final @NotNull String login, final @NotNull String password) {
        byteBuf.clear();

        byteBuf.writeByte((byte)MessageType.LOGIN.ordinal());
        byteBuf.writeByte((byte)login.getBytes().length);
        byteBuf.writeBytes(login.getBytes());
        byteBuf.writeByte((byte)password.getBytes().length);
        byteBuf.writeBytes(password.getBytes());

        byteBuf.retain();
        connection.getCurrentChannel().writeAndFlush(byteBuf);
    }

    public void sendLogoutRequest() {
        byteBuf.clear();

        byteBuf.writeByte((byte)MessageType.LOGOUT.ordinal());

        byteBuf.retain();
        connection.getCurrentChannel().writeAndFlush(byteBuf);
    }

    public void sendRegisterRequest(final @NotNull String login, final @NotNull String password) {
        byteBuf.clear();

        byteBuf.writeByte((byte)MessageType.REGISTER.ordinal());
        byteBuf.writeByte((byte)login.getBytes().length);
        byteBuf.writeBytes(login.getBytes());
        byteBuf.writeByte((byte)password.getBytes().length);
        byteBuf.writeBytes(password.getBytes());

        byteBuf.retain();
        connection.getCurrentChannel().writeAndFlush(byteBuf);
    }

    public void sendFileListRequest() {
        byteBuf.clear();

        byteBuf.writeByte((byte)MessageType.GET_FILE_LIST.ordinal());

        byteBuf.retain();
        connection.getCurrentChannel().writeAndFlush(byteBuf);
    }

    public void sendLocationChangeRequest(final @NotNull String deltaLocation) {
        byteBuf.clear();

        byteBuf.writeByte((byte)MessageType.LOCATION_CHANGE.ordinal());
        byteBuf.writeByte((byte)deltaLocation.getBytes().length);
        byteBuf.writeBytes(deltaLocation.getBytes());

        byteBuf.retain();
        connection.getCurrentChannel().writeAndFlush(byteBuf);
    }

    public void sendFileUploadRequest(final @NotNull String pathToFile) throws IOException {
        //Check if file is uploadable
        final Path filePath = Paths.get(pathToFile);
        if (!Files.isRegularFile(filePath)) {
            System.out.println("Inappropriate path to file");
            return;
        }
        byteBuf.clear();
        /**
         * Эта часть доставляется без проблем
         */
        //Send message type, file name and length
        byteBuf.writeByte((byte)MessageType.FILE_UPLOAD.ordinal());
        byteBuf.writeByte((byte)filePath.getFileName().toString().getBytes().length);
        byteBuf.writeBytes(filePath.getFileName().toString().getBytes());
        final long size = Files.size(filePath);
        byteBuf.writeLong(size);
        byteBuf.retain();
        connection.getCurrentChannel().writeAndFlush(byteBuf);
        /**
         * Эта часть теряет некоторые сообщения, а некоторые отправляются по 10-20 раз.
         * при этом проверка содержимого буффера говорит что всё в порядке.
         */
        //Send file chunks
        byte[] buf = new byte[BUFFER_SIZE];
        final long chunksCount = size / BUFFER_SIZE;
        InputStream is = Files.newInputStream(filePath);
        try {
            for (long i = 0; i < chunksCount; i++) {
                byteBuf.clear();
                is.read(buf);
                byteBuf.writeBytes(buf);
                byteBuf.retain();
                connection.getCurrentChannel().writeAndFlush(byteBuf);
            }
            if (size % BUFFER_SIZE == 0) return;
            buf = new byte[(int)size % BUFFER_SIZE];
            is.read(buf);
            byteBuf.writeBytes(buf);
            byteBuf.retain();
            connection.getCurrentChannel().writeAndFlush(byteBuf);
        } finally {
            is.close();
        }
    }

    public void sendFileDownloadRequest(final @NotNull String fileName) {
        byteBuf.clear();

        byteBuf.writeByte((byte)MessageType.FILE_DOWNLOAD.ordinal());
        byteBuf.writeByte((byte)fileName.getBytes().length);
        byteBuf.writeBytes(fileName.getBytes());

        byteBuf.retain();
        connection.getCurrentChannel().writeAndFlush(byteBuf);
    }

    public void sendFileRemoveRequest(final @NotNull String fileName) {
        byteBuf.clear();

        byteBuf.writeByte((byte)MessageType.FILE_DELETE.ordinal());
        byteBuf.writeByte((byte)fileName.getBytes().length);
        byteBuf.writeBytes(fileName.getBytes());

        byteBuf.retain();
        connection.getCurrentChannel().writeAndFlush(byteBuf);
    }

    public void sendFileRenameRequest(final @NotNull String from, final @NotNull String to) {
        byteBuf.clear();

        byteBuf.writeByte((byte)MessageType.FILE_RENAME.ordinal());
        byteBuf.writeByte((byte)from.getBytes().length);
        byteBuf.writeBytes(from.getBytes());
        byteBuf.writeByte((byte)to.getBytes().length);
        byteBuf.writeBytes(to.getBytes());

        byteBuf.retain();
        connection.getCurrentChannel().writeAndFlush(byteBuf);
    }

}
