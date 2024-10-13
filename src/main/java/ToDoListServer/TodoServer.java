package ToDoListServer;

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
    private static final String FILE_PATH = ".\\src\\main\\java\\ToDoListServer\\todos.json";

    public static void main(String[] args) throws IOException {
        loadTodosFromFile();

        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/todos", new TodoHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server runs at http://localhost:8080/todos");

        Timer saveTodosTimer = new Timer();
        saveTodosTimer.schedule(new SafeFiles(), 10000, 300000);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                saveTodosToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ));
    }

    // class for persistent saving and loading data from server after server was down
    static class SafeFiles extends TimerTask {
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
            Type todoListType = new TypeToken<List<Todo>>() {
            }.getType();
            List<Todo> loadedTodos = gson.fromJson(reader, todoListType);

            if (loadedTodos != null) {
                todos.clear();
                todos.addAll(loadedTodos);

                // sets idCounter to the value for next task to add list
                // necessary when restart server, to keep adding the correct ID
                idCounter = todos.stream().mapToInt(Todo::getId).max().orElse(0);
                idCounter++;

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
            // sets CORS headers
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

        // handles every GET request and answers with the List that contains todos as JSON
        private void handleGet(HttpExchange exchange) throws IOException {
            String response = gson.toJson(todos);
            sendResponse(exchange, 200, response);
        }

        // handles every PUT request, finds the task to change by finding it by its ID
        // and updates that task with given parameters
        // can handle multiple updates at once
        private void handlePut(HttpExchange exchange) throws IOException {
            String requestBody = readAllBytes(exchange);
            Type listType = new TypeToken<List<Todo>>() {
            }.getType();
            List<Todo> updatedTodos = gson.fromJson(requestBody, listType);

            for (Todo updatedTodo : updatedTodos) {
                Todo existingTodo = findTodoById(updatedTodo.getId());
                if (existingTodo != null) {
                    existingTodo.setCompleted(updatedTodo.isCompleted());
                }
            }
            // answers with updated todolist as JSON
            String response = gson.toJson(todos);
            sendResponse(exchange, 200, response);
        }

        // handles every POST request and adds the given task to the todolist
        // answers with the added task as JSON
        private void handlePost(HttpExchange exchange) throws IOException {
            Todo todo = gson.fromJson(readAllBytes(exchange), Todo.class);
            // handlePost is also responsible to allocate unique IDs
            todo.setId(idCounter++);
            todos.add(todo);
            sendResponse(exchange, 201, gson.toJson(todo));
        }

        // handles every DELETE request by its unique URL ending with the id that wants to be deleted
        // can handle only one DELETE request at a time
        // answers with a String containing information that delete is done
        private void handleDelete(HttpExchange exchange) throws IOException {
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

        private Todo findTodoById(int id) {
            for (Todo todo : todos) {
                if (todo.getId() == id) {
                    return todo;
                }
            }
            return null;
        }
    }
}