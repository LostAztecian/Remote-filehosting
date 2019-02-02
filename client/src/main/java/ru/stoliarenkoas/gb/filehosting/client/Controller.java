package ru.stoliarenkoas.gb.filehosting.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.stoliarenkoas.gb.filehosting.common.MessageType;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

public class Controller implements Initializable {
    private final MessageType[] messageTypes = MessageType.values();
    private Connection connection = Connection.getInstance();

    final Path clientFolder = Paths.get("client_folder");
    final Path downloadsFolder = clientFolder.resolve("downloads");
    final Path clientFile = Paths.get("Clients-file.txt"); //Stub

    final Set<String> dirs = new TreeSet<>();
    final Set<String> files = new TreeSet<>();

    Path requestedFile;

    public void initialize(URL location, ResourceBundle resources) {
        initializeWindowDragAndDropLabel();
    }

    @FXML
    Label labelDragWindow;
    @FXML
    VBox mainVBox;
    @FXML
    TextArea textarea;

    @FXML
    private void login() {
        System.out.println("login");
    }

    @FXML
    private void auth(ActionEvent actionEvent) {
        System.out.println("auth!");
    }

    public void btnShowModal(ActionEvent actionEvent) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            LoginController lc = (LoginController) loader.getController();
            lc.id = 100;
            lc.backController = this;

            stage.setTitle("JavaFX Autorization");
            stage.setScene(new Scene(root, 400, 200));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    double dragDeltaX, dragDeltaY;
    public void initializeWindowDragAndDropLabel() {
        Platform.runLater(() -> {
            Stage stage = (Stage) mainVBox.getScene().getWindow();

            labelDragWindow.setOnMousePressed(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    // record a delta distance for the drag and drop operation.
                    dragDeltaX = stage.getX() - mouseEvent.getScreenX();
                    dragDeltaY = stage.getY() - mouseEvent.getScreenY();
                }
            });
            labelDragWindow.setOnMouseDragged(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    stage.setX(mouseEvent.getScreenX() + dragDeltaX);
                    stage.setY(mouseEvent.getScreenY() + dragDeltaY);
                }
            });
        });
    }

    public void btnExit(ActionEvent actionEvent) {
        System.exit(0);
    }


    public void btnConnect(ActionEvent actionEvent) {
        System.out.println("Button 'Connect/Disconnect' is pressed");

        //Close connection if it is opened
        if (connection.getCurrentChannel() != null) {
            connection.close();
            ((Button)actionEvent.getSource()).setText("Connect");
            System.out.println("Disconnected!");
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                connection.start();
            }
        });
        t.setDaemon(true);
        t.start();
//        connection.getMessageSender().sendHandshake();
        ((Button)actionEvent.getSource()).setText("Disconnect");
        System.out.println("Connected!");
    }

    /**
     * Part of the stub, should not be here.
     */
    private void receiveFile() throws Exception {
//        System.out.println("Receiving file...");
//        InputStream is = socket.getInputStream();
//
//        //get file size
//        long filesize = 0;
//        for (int i = 7; i >= 0; i--) {
//            int b = is.read();
//            filesize += b << (8 * i);
//
//        }
//        System.out.println("File size is " + filesize);
//
//        //create dirs, remove old files and open file-writing stream
//        Files.createDirectories(downloadsFolder);
//        Path targetFile = downloadsFolder.resolve(requestedFile);
//        if (Files.exists(targetFile)) Files.delete(targetFile);
//        OutputStream os = Files.newOutputStream(targetFile);
//
//        //divide file onto chunks
//        long chunksCount = filesize / 256;
//        byte[] buf = new byte[256];
//        while (chunksCount-- > 0) {
//            is.read(buf);
//            System.out.println("Reading filechunk: " + Arrays.toString(buf));
//            os.write(buf);
//        }
//        //send remainder
//        for (int i = 0; i < filesize % 256; i++) {
//            os.write(is.read());
//        }
//
//        //free resources, set flags
//        os.close();
//        requestedFile = null;
    }

    private boolean checkConnection() {
        if (connection.getCurrentChannel() == null || !connection.getCurrentChannel().isActive()) {
            System.out.println("You are not connected!");
            return false;
        }
        return true;
    }

    public void btnLogin(ActionEvent actionEvent) {
        System.out.println("Button 'Login' is pressed");
        if (!checkConnection()) return;

        connection.getMessageSender().sendLoginRequest("merciful goddess", "mother of the Forlorn");
//        //get login and password
//        final byte[] loginStub = "merciful goddess".getBytes();
//        final byte[] pwdStub = "mother of the Forlorn".getBytes();
//        final byte[] msg = new byte[1 + 1 + loginStub.length + 1 + pwdStub.length];
//
//        //assemble message
//        msg[0] = (byte) MessageType.LOGIN.ordinal();
//        msg[1] = (byte) loginStub.length;
//        msg[2 + msg[1]] = (byte) pwdStub.length;
//        System.arraycopy(loginStub, 0, msg, 2, msg[1]);
//        System.arraycopy(pwdStub, 0, msg, 3 + msg[1], msg[2 + msg[1]]);
//        System.out.println(Arrays.toString(msg));
//
//        //send message
//        try {
//            socket.getOutputStream().write(msg);
//            System.out.println("Login message sent!");
//        } catch (IOException e) {
//            System.out.println("Connection crashed and connection button is not updated!!!");
//            socket = null;
//        }
    }

    public void btnUploadFile(ActionEvent actionEvent) throws IOException{
        System.out.println("Button 'UploadFile' is pressed");
        if (!checkConnection()) return;

        Path filepath = clientFolder.resolve(clientFile);
        connection.getMessageSender().sendFileUploadRequest(filepath.toString());
//        try {
//            //assemble message
//            final long filesize = Files.size(filepath);
//            final byte[] filepathBytes = filepath.getFileName().toString().getBytes();
//            final byte[] msg = new byte[1 + 8 + 1 + filepathBytes.length];
//            msg[0] = (byte)MessageType.FILE_UPLOAD.ordinal();
//            for (int i = 0; i < 8; i++) {
//                msg[i + 1] = (byte) (filesize >>> (8 * (7 - i)));
//            }
//            msg[9] = (byte)filepathBytes.length;
//            System.arraycopy(filepathBytes, 0, msg, 10, filepathBytes.length);
//            System.out.println("File upload message assembled: " + Arrays.toString(msg));
//
//            //write message
//            OutputStream os = socket.getOutputStream();
//            os.write(msg);
//            os.flush();
//
//            //count file-chunks
//            long partsCount = filesize / 256;
//            final byte[] reminder = new byte[(int)filesize%256];
//
//            //write file-chunks
//            boolean exceptionSourceIn = true;
//            try (InputStream is = Files.newInputStream(filepath)) {
//                final byte[] buf = new byte[256];
//                while (0 < partsCount--) {
//                    is.read(buf);
//                    exceptionSourceIn = false;
//                    os.write(buf);
//                    exceptionSourceIn = true;
//                    System.out.println("Buffer part is written: " + Arrays.toString(buf));
//                }
//                is.read(reminder);
//                exceptionSourceIn = false;
//                os.write(reminder);
//                System.out.println("Reminder is written: " + Arrays.toString(reminder));
//            } catch (IOException e) {
//                if (exceptionSourceIn) System.out.println("File read problem occurred!");
//                else {
//                    try {
//                        socket.close();
//                    } catch (IOException ex) {}
//                    socket = null;
//                    System.out.println("Connection closed!");
//                }
//            }
//        } catch (IOException e) {
//            System.out.println("Can't get size.");
//        }
    }


    public void btnDownloadFile(ActionEvent actionEvent) {
        System.out.println("Button 'DownloadFile' is pressed");

        if (!checkConnection()) return;
        connection.getMessageSender().sendFileDownloadRequest(clientFile.toString());
//        //assemble message
//        final byte[] filepathBytes = clientFile.toString().getBytes();
//        final byte[] msg = new byte[1 + 1 + filepathBytes.length];
//        if  (socket == null) return;
//        msg[0] = (byte)MessageType.FILE_DOWNLOAD.ordinal();
//        msg[1] = (byte)filepathBytes.length;
//        System.arraycopy(filepathBytes, 0, msg, 2, filepathBytes.length);
//
//        //write message
//        try {
//            socket.getOutputStream().write(msg);
//        } catch (IOException e) {
//            try {
//                socket.close();
//            } catch (Exception ex) {
//            } finally {
//                socket = null;
//                System.out.println("Connection closed!");
//            }
//        }
//
//        //set flags
//        requestedFile = clientFile;
//        System.out.println("Download request sent: " + Arrays.toString(msg));
    }

    public void btnSomethingElse(ActionEvent actionEvent) throws Exception{
        System.out.println("Button 'SomethingElse' is pressed");
        textarea.clear();
        dirs.clear();
        files.clear();

        Files.walkFileTree(clientFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.equals(clientFolder)) return FileVisitResult.CONTINUE;
                dirs.add(String.format("%s\\%n", dir.getFileName()));
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                files.add(String.format("%s%n", file.getFileName()));
                return FileVisitResult.CONTINUE;
            }
        });

        dirs.forEach(textarea::appendText);
        files.forEach(textarea::appendText);
    }
}
