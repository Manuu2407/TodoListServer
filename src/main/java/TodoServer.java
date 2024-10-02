import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TodoServer {
    private static final List<Todo> todos = new ArrayList<>();
    private static int idCounter = 1;
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/todos", new TodoHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server runs at http://localhost:8080/todos");

    }


    static class TodoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            switch (exchange.getRequestMethod()) {
                case "GET":
                    handleGet(exchange);
                    break;
                case "POST":
                    handlePost(exchange);
                    break;
                case "PUT":
                    handlePut(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange);
                    break;
                default:
                    sendResponse(exchange, 405, "Method Not Allowed");
            }
        }

        // readAllBytes can be replaced by InputStream.readAllBytes() in newest java version.
        // Not available before java 9.
        private String readAllBytes(HttpExchange exchange) throws IOException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = exchange.getRequestBody();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toString();
        }

        private void handleGet(HttpExchange exchange) throws IOException {
            String response = gson.toJson(todos);
            sendResponse(exchange, 200, response);
        }

        private Todo findTodoById(int id) {
            for (Todo todo : todos) {
                if (todo.getId() == id) {
                    return todo;
                }
            }
            return null;
        }

private void handlePut(HttpExchange exchange) throws IOException {
    String path = exchange.getRequestURI().getPath();
    String[] parts = path.split("/");
    int id = Integer.parseInt(parts[parts.length - 1]);
    Todo todo = findTodoById(id);

    if (todo != null) {
        InputStream inputStream = exchange.getRequestBody();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;

        // Lese den InputStream
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        // Konvertiere die gesammelten Bytes in einen String
        String json = outputStream.toString(StandardCharsets.UTF_8);

        // Konvertiere den JSON-String in ein Todo-Objekt
        Todo updatedTodo = gson.fromJson(json, Todo.class);

        // Aktualisiere das To-Do
        if (updatedTodo.getTask() != null) {
            todo.setTask(updatedTodo.getTask());
        }
            todo.setCompleted(updatedTodo.isCompleted());

        // Sende eine Bestätigung
        sendResponse(exchange, 200, "Todo updated");
    } else {
        // Wenn das To-Do nicht gefunden wird
        sendResponse(exchange, 404, "Todo not found");
    }
}


        private void handlePost(HttpExchange exchange) throws IOException {
            Todo todo = gson.fromJson(readAllBytes(exchange), Todo.class);
            todo.setId(idCounter++);
            todos.add(todo);
            sendResponse(exchange, 201, "Todo hinzugefügt");
        }

        private void handleDelete(HttpExchange exchange) throws IOException{
            int id = Integer.parseInt(exchange.getRequestURI().getPath().split("/")[2]);
            todos.removeIf(todo -> todo.getId() == id);
            sendResponse(exchange, 200, "Todo gelöscht");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}