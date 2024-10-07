import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TodoServer {
    private static final List<Todo> todos = new ArrayList<>();
    private static int idCounter = 1;
    private static final Gson gson = new Gson();
    private static final String FILE_PATH = "C:\\Users\\Manu\\dev\\TodoListServer\\src\\main\\java\\todos.json";

    public static void main(String[] args) throws IOException {
        loadTodosFromFile();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/todos", new TodoHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server runs at http://localhost:8080/todos");

        Timer saveTodosTimer = new Timer();
        saveTodosTimer.schedule(new SafeFiles(), 10000, 300000);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
           try{
               saveTodosToFile();
           } catch (IOException e) {
               e.printStackTrace();
           }
        }
        ));
    }

    static class SafeFiles extends TimerTask{
        public void run() {
            try {
                saveTodosToFile();
                System.out.println(" (automatisches speichern)");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void saveTodosToFile() throws IOException {
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            gson.toJson(todos, writer);
            System.out.println("To-Dos wurden in " + FILE_PATH + " gespeichert.");
        }
    }

    private static void loadTodosFromFile() throws IOException {
        try (FileReader reader = new FileReader(FILE_PATH)) {
            Type todoListType = new TypeToken<List<Todo>>(){}.getType();
            List<Todo> loadedTodos = gson.fromJson(reader, todoListType);

            if (loadedTodos != null) {
                todos.clear();
                todos.addAll(loadedTodos);

                idCounter = todos.stream().mapToInt(Todo::getId).max().orElse(0);

                System.out.println("To-Dos wurden aus " + FILE_PATH + " geladen.");

            }
        }
    }

    @CrossOrigin(origins = "http://127.0.0.1:5500")
    @RestController
    @RequestMapping("/todos")
    static class TodoHandler implements HttpHandler {
        @CrossOrigin(origins = "http://127.0.0.1:5500")
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Set CORS headers
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
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
            String requestBody = readAllBytes(exchange);
            Type listType = new TypeToken<List<Todo>>(){}.getType();
            List<Todo> updatedTodos = gson.fromJson(requestBody, listType);

            for (Todo updatedTodo : updatedTodos) {
                Todo existingTodo = findTodoById(updatedTodo.getId());
                if (existingTodo != null) {
                    existingTodo.setCompleted(updatedTodo.isCompleted());
                }
            }

            String response = gson.toJson(todos);
            sendResponse(exchange, 200, response);
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
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }
    }
}