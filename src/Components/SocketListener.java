package Components;

import Controllers.layoutController;
import javafx.application.Platform;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.impl.DefaultBHttpServerConnection;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketListener {
    private static SocketListener ourInstance = new SocketListener();
    private static ExecutorService executorService;
    private static layoutController controller;
    private boolean listening;

    public static SocketListener getInstance() {
        return ourInstance;
    }

    public static void setController(layoutController controller) {
        SocketListener.controller = controller;
    }

    public void startListening() {
        executorService = Executors.newWorkStealingPool();
        listening = true;
        listen();
    }

    public void stopListening() {
        listening = false;
        executorService.shutdownNow();
    }

    private void listen() {
        executorService.execute(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(20570);

                while (listening) {
                    Socket clientSocket = serverSocket.accept();
                    executorService.execute(new Client(clientSocket));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private class Client implements Runnable {
        private Socket socket;

        public Client(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                DefaultBHttpServerConnection connection = new DefaultBHttpServerConnection(8 * 1024);
                connection.bind(socket);
                HttpRequest request = connection.receiveRequestHeader();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                if (request instanceof HttpEntityEnclosingRequest) {
                    connection.receiveRequestEntity((HttpEntityEnclosingRequest) request);
                    HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                    if (entity != null) {
                        entity.writeTo(outputStream);
                        String url = outputStream.toString();
                        Platform.runLater(() -> new AddPopUp(controller, url));
                        EntityUtils.consume(entity);
                    }
                }
            } catch (IOException | HttpException e) {
                e.printStackTrace();
            }
        }
    }
}
