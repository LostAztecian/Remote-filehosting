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
import javafx.scene.control.TextField;
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

    private String currentUser = null;

    final Path clientFolder = Paths.get("client_folder");
    Path currentRemoteFolder = clientFolder.resolve("");
    Path currentFolder = Paths.get("downloads");
    final Path currentFile = Paths.get("Clients-file.txt"); //Stub

    final Set<String> dirs = new TreeSet<>();
    final Set<String> files = new TreeSet<>();

    Path requestedFile;

    public void initialize(URL location, ResourceBundle resources) {
        initializeWindowDragAndDropLabel();
    }

    @FXML Label labelDragWindow;
    @FXML VBox mainVBox;
    @FXML TextArea textarea;
    @FXML TextField username;

    //Buttons
    @FXML Button btnConnect;
    @FXML Button btnLogin;
    @FXML Button btnUploadFile;
    @FXML Button btnDownloadFile;
    @FXML Button btnSomethingElse;

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
            btnConnect.setText("Connect");
            username.setText("Unauthorized!");
            System.out.println("Disconnected!");
            return;
        }

        connection.registerController(this);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                connection.start();
            }
        });
        t.setDaemon(true);
        t.start();
        //Wait for it to start
        try {
            System.out.println("Waiting for connection to establish...");
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        connection.getMessageSender().sendHandshake();
        ((Button)actionEvent.getSource()).setText("Disconnect");
        System.out.println("Connected!");
    }

    private boolean checkConnection() {
        if (connection.getCurrentChannel() == null || !connection.getCurrentChannel().isActive()) {
            System.out.println("You are not connected!");
            return false;
        }
        return true;
    }

    public void btnLogin(ActionEvent actionEvent) {
        System.out.println("Button 'Login/Logout' is pressed");
        if (!checkConnection()) return;

        if ("Unauthorized!".equals(username.getText())) {
            connection.getMessageSender().sendLoginRequest("merciful goddess", "mother of the Forlorn");
            return;
        }

        connection.getMessageSender().sendLogoutRequest();

    }

    public void btnUploadFile(ActionEvent actionEvent) throws IOException{

        System.out.println("Button 'UploadFile' is pressed");
        if (!checkConnection()) return;

        Path filepath = clientFolder.resolve(currentFile);
        connection.getMessageSender().sendFileUploadRequest(filepath);

    }


    public void btnDownloadFile(ActionEvent actionEvent) {

        System.out.println("Button 'DownloadFile' is pressed");

        if (!checkConnection()) return;
        connection.getMessageSender().sendFileDownloadRequest(currentFile.toString());

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

    public Path resolveFilepath() {
        return clientFolder.resolve(currentFolder).resolve(currentFile);
    }

}
