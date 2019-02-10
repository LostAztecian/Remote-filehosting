package ru.stoliarenkoas.gb.filehosting.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import ru.stoliarenkoas.gb.filehosting.common.MessageType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UserHandler {
    private static final int BUFFER_SIZE = 256;
    private static final Path SERVER_ROOT_FOLDER = Paths.get("Server_files");
    private final ByteBuf buffer = new PooledByteBufAllocator().buffer(BUFFER_SIZE);
    private String username = null;

    private InputStream is = null;
    private OutputStream os = null;

    private final List<String> dirsList = new ArrayList<>();
    private final List<String> fileList = new ArrayList<>();

    private Path currentFolder = Paths.get("");
    private Path currentFile = null;
    private long remainingFileSize = 0;

    public boolean handshake(ChannelHandlerContext ctx, ByteBuf msg) {

        final byte[] msgBytes = new byte[msg.readByte()];
        msg.readBytes(msgBytes);
        msg.release();
        final String message = new String(msgBytes);
        System.out.println("User greets you: " + message);
        final String responce = "Greetings from server";
        buffer.clear();
        buffer.retain();
        buffer.writeByte((byte)MessageType.HANDSHAKE_RESPONSE.ordinal());
        buffer.writeByte(responce.getBytes().length);
        buffer.writeBytes(responce.getBytes());
        ctx.writeAndFlush(buffer);
        return true;

    }

    public boolean login(ChannelHandlerContext ctx, ByteBuf msg) {

        //get login
        final byte[] loginBytes = new byte[msg.readByte()];
        for (int i = 0; i < loginBytes.length; i++) loginBytes[i] = msg.readByte();
        final String login = new String(loginBytes);
        //get password
        final byte[] passwordBytes = new byte[msg.readByte()];
        for (int i = 0; i < passwordBytes.length; i++) passwordBytes[i] = msg.readByte();
        final String password = new String(passwordBytes);

        //put authorization here

        System.out.printf("User logged in:%n-login: %s%n-password: %s%n", login, password);
        username = login;

        //respond with login
        buffer.clear();
        buffer.retain();
        buffer.writeByte((byte)MessageType.LOGIN_RESPONSE.ordinal());
        buffer.writeByte(loginBytes.length);
        buffer.writeBytes(loginBytes);
        ctx.writeAndFlush(buffer);
        return sendFileList(ctx, msg);

    }

    public boolean logout(ChannelHandlerContext ctx, ByteBuf msg) {

        username = null;
        msg.release();

        buffer.clear();
        buffer.retain();
        buffer.writeByte((byte)MessageType.LOGOUT_RESPONSE.ordinal());
        ctx.writeAndFlush(buffer);
        return true;

    }

    public boolean register(ChannelHandlerContext ctx, ByteBuf msg) {

        //put register logic here
        msg.release();

        buffer.clear();
        buffer.retain();
        buffer.writeByte((byte)MessageType.REGISTER_RESPONSE.ordinal());
        ctx.writeAndFlush(buffer);
        return true;

    }

    /**
     * Send type(1b) + list length(1b) + data[length(1b) + payload]
     */
    public boolean sendFileList(ChannelHandlerContext ctx, ByteBuf msg) {

        msg.release();
        if (username == null) return true;
        dirsList.clear();
        fileList.clear();
        try {
            Files.walkFileTree(SERVER_ROOT_FOLDER.resolve(username).resolve(currentFolder), new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.equals(currentFolder) || dir.equals(SERVER_ROOT_FOLDER.resolve(username))) return FileVisitResult.CONTINUE;
                    dirsList.add(dir.getFileName().toString());
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    fileList.add(file.getFileName().toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        buffer.clear();
        buffer.writeByte((byte)MessageType.GET_FILE_LIST_RESPONSE.ordinal());
        buffer.writeByte((byte)(dirsList.size() + fileList.size()));
        buffer.writeByte((byte) dirsList.size());
//        System.out.printf("Files: %d, including %d dirs.%n", (byte) dirsList.size() + fileList.size(), (byte) dirsList.size());
        for (String name : dirsList) {
            writeString(ctx, name);
        }
        for (String name : fileList) {
            writeString(ctx, name);
        }
        return true;

    }

    private void writeString(final ChannelHandlerContext ctx, final String text) {
        final byte[] bytes = text.getBytes();
        buffer.retain();
        buffer.writeByte(bytes.length);
        buffer.writeBytes(bytes);
        ctx.writeAndFlush(buffer);
        buffer.clear();
    }

    public boolean changeLocation(ChannelHandlerContext ctx, ByteBuf msg) {

        final byte[] bytes = new byte[msg.readByte()];
        for (int i = 0; i < bytes.length; i++) bytes[i] = msg.readByte();
        final String folder = new String(bytes);

        //resolve target folder
        Path targetFolder;
        if ("..".equals(folder)) {
            targetFolder = currentFolder.getParent();
        } else targetFolder = currentFolder.resolve(folder);

        //prepare response
        buffer.clear();
        buffer.retain();
        buffer.writeByte((byte)MessageType.LOCATION_CHANGE_RESPONSE.ordinal());

        //if unable to change location
        if (targetFolder == null || Files.notExists(targetFolder) || !Files.isDirectory(targetFolder)) {
            buffer.writeBoolean(false);
            ctx.writeAndFlush(buffer);
            msg.release();
            return true;
        }
        //if location changed
        currentFolder = targetFolder;
        buffer.writeBoolean(true);
        buffer.writeByte(currentFolder.toString().getBytes().length);
        buffer.writeBytes(currentFolder.toString().getBytes());
        ctx.writeAndFlush(buffer);

        return sendFileList(ctx, msg);

    }

    public boolean uploadFile(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

        //If this is a first chunk - set flags and open file-writing stream.
        if (os == null) {
            System.out.println("Creating a new upload session...");
            //get name
            final byte[] filenameBytes = new byte[msg.readByte()];
            for (int i = 0; i < filenameBytes.length; i++) {
                filenameBytes[i] = msg.readByte();
            }
            currentFile = SERVER_ROOT_FOLDER.resolve(Paths.get(username).resolve(currentFolder).resolve(Paths.get(new String(filenameBytes))));
            System.out.println("Incoming file name: " + currentFile);
            //get size
            remainingFileSize = msg.readLong();
            System.out.println("Incoming file size: " + remainingFileSize);
            //Create dirs, clear old files and open stream.
            Files.createDirectories(currentFile.getParent());
            if (Files.exists(currentFile)) Files.delete(currentFile);
            os = Files.newOutputStream(currentFile, StandardOpenOption.CREATE_NEW);
        }
        //Write remaining data
        System.out.printf("Readable bytes before read %d%n", msg.readableBytes());
        final int toWrite = remainingFileSize > msg.readableBytes() ? msg.readableBytes() : (int)remainingFileSize;
        final byte[] buf = new byte[toWrite];
        msg.readBytes(buf);
        System.out.println(new String(buf));
        assert msg.readableBytes() == 0;
        os.write(buf);

        msg.release();

        //Check completion and return.
        remainingFileSize -= toWrite;
        System.out.println("Remaining file size is " + remainingFileSize);
        System.out.println("Output file size is " + Files.size(currentFile));
        if (remainingFileSize == 0) {
            os.close();
            os = null;
            currentFile = null;
            System.out.println("Uploading file is finished!");
            return true;
        }
        return false;

    }

    public boolean downloadFile(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

        //Read download request
        final byte[] filenameBytes = new byte[msg.readByte()];
        for (int i = 0; i < filenameBytes.length; i++) {
            filenameBytes[i] = msg.readByte();
        }

        //If user is not logged in - ignore request.
        if (username == null) return true;

        //Ensure file existence.
        currentFile = SERVER_ROOT_FOLDER.resolve(Paths.get(username)).resolve(currentFolder).resolve(Paths.get(new String(filenameBytes)));
        if (Files.notExists(currentFile)) {
            return true;
        }

        //Write file meta-data
        remainingFileSize = Files.size(currentFile);
        buffer.clear();
        buffer.retain();
        buffer.writeByte((byte)MessageType.FILE_DOWNLOAD_RESPONSE.ordinal());
        buffer.writeLong(remainingFileSize);
        ctx.writeAndFlush(buffer);

        //Split file and send chunks
        is = Files.newInputStream(currentFile);
        final long fullChunksCount = remainingFileSize / BUFFER_SIZE;
        buffer.clear();
        if (fullChunksCount > 0) {
            byte[] buf = new byte[BUFFER_SIZE];
            for (int i = 0; i < fullChunksCount; i++) {
                is.read(buf);
                buffer.writeBytes(buf);
                buffer.retain();
                ctx.writeAndFlush(buffer);
                buffer.clear();
            }
        }
        remainingFileSize -= fullChunksCount * BUFFER_SIZE;

        //Write remainder
        if (remainingFileSize > 0) {
            final byte[] buf = new byte[(int)remainingFileSize];
            is.read(buf);
            buffer.writeBytes(buf);
            buffer.retain();
            ctx.writeAndFlush(buffer);
        }

        //Close file ans reset flags.
        currentFile = null;
        is.close();
        is = null;
        return true;

    }

    public boolean removeFile(ChannelHandlerContext ctx, ByteBuf msg) {

        //get target file
        final byte[] bytes = new byte[msg.readByte()];
        for (int i = 0; i < bytes.length; i++) bytes[i] = msg.readByte();
        final String file = new String(bytes);
        msg.release();
        currentFile = SERVER_ROOT_FOLDER.resolve(Paths.get(username)).resolve(currentFolder).resolve(file);

        buffer.clear();
        buffer.retain();
        buffer.writeByte((byte)MessageType.FILE_DELETE_RESPONSE.ordinal());
        final boolean exists = Files.exists(currentFile);
        //double boolean: one if exists and one if deleted
        buffer.writeBoolean(exists);
        if (exists) {
            try {
                Files.delete(currentFile);
                buffer.writeBoolean(true);
            } catch (IOException e) {
                buffer.writeBoolean(false);
                e.printStackTrace();
            }
        }
        ctx.writeAndFlush(buffer);
        currentFile = null;
        return true;

    }

    public boolean renameFile(ChannelHandlerContext ctx, ByteBuf msg) {

        //get source file
        final byte[] fromBytes = new byte[msg.readByte()];
        for (int i = 0; i < fromBytes.length; i++) fromBytes[i] = msg.readByte();
        final String from = new String(fromBytes);

        //get target file
        final byte[] toBytes = new byte[msg.readByte()];
        for (int i = 0; i < toBytes.length; i++) toBytes[i] = msg.readByte();
        final String to = new String(toBytes);
        msg.release();

        //check if possible to execute
        currentFile = SERVER_ROOT_FOLDER.resolve(Paths.get(username)).resolve(currentFolder).resolve(Paths.get(from));
        final Path targetFile = currentFile.getParent().resolve(Paths.get(to));
        final boolean executable = Files.exists(currentFile) && Files.notExists(targetFile);

        buffer.clear();
        buffer.retain();
        buffer.writeByte((byte)MessageType.FILE_RENAME_RESPONSE.ordinal());
        //double boolean: one if executable and one if moved
        buffer.writeBoolean(executable);
        if (executable) {
            try {
                Files.move(currentFile, targetFile);
                buffer.writeBoolean(true);
            } catch (IOException e) {
                buffer.writeBoolean(false);
                e.printStackTrace();
            }
        }

        ctx.writeAndFlush(buffer);
        currentFile = null;
        return true;

    }

}
