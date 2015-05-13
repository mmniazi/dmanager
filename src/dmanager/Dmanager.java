package dmanager;

import Controllers.layoutController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * @author muhammad
 */
public class Dmanager extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.getIcons().add(new Image("resources/icon.png"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainWindow.fxml"));
        Parent root = loader.load();
        Platform.setImplicitExit(false);
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.show();
        layoutController controller = loader.getController();
        controller.createTrayIcon(stage);

        Delta delta = new Delta();
        controller.getMainWindow().setOnMousePressed(event -> {
            delta.x = stage.getX() - event.getScreenX();
            delta.y = stage.getY() - event.getScreenY();
        });

        controller.getMainWindow().setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + delta.x);
            stage.setY(event.getScreenY() + delta.y);
        });
    }

    class Delta {
        double x, y;
    }
}
