import com.sun.net.httpserver.Headers;
import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.ApiResult;
import queries.BookQueryConditions;
import queries.SortOrder;
import utils.ConnectConfig;
import utils.DatabaseConnector;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.net.InetSocketAddress;
import java.util.stream.Collectors;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;



public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());
    private static DatabaseConnector connector;

    public static void main(String[] args) {
        try {
            // parse connection config from "resources/application.yaml"
            ConnectConfig conf = new ConnectConfig();
            log.info("Success to parse connect config. " + conf.toString());
            // connect to database
            connector = new DatabaseConnector(conf);
            boolean connStatus = connector.connect();
            if (!connStatus) {
                log.severe("Failed to connect database.");
                System.exit(1);
            }
            /* do somethings */
            // 创建HTTP服务器，监听指定端口
            // 这里是8000，建议不要80端口，容易和其他的撞
            HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

            // 添加handler，这里就绑定到/card路由
            // 所以localhost:8000/card是会有handler来处理
            server.createContext("/card", new CardHandler());
            server.createContext("/borrow", new BorrowHandler());
            server.createContext("/book", new BookHandler());

            // 启动服务器
            server.start();

            // 标识一下，这样才知道我的后端启动了（确信
            System.out.println("Server is listening on port 8000");
//            // release database connection handler
//            if (connector.release()) {
//                log.info("Success to release connection.");
//            } else {
//                log.warning("Failed to release connection.");
//            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (connector != null && connector.release()) {
                    log.info("Database connection released.");
                }
                server.stop(0); // 停止服务器
                log.info("Server shutdown complete.");
            }));

            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class CardHandler implements HttpHandler {
        // 关键重写handle方法
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            } else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
            String response = "";

            LibraryManagementSystemImpl library = new LibraryManagementSystemImpl(connector);
            ApiResult result = library.showCards();
            if (result.ok) {
                // [{"cards":[{"cardId":1,"department":"Architecture","name":"User00000","type":"Teacher"}]}]
                JSONArray jsonArray = new JSONArray(result.payload);
                JSONArray cards = jsonArray.getJSONObject(0).getJSONArray("cards");
                response = cards.toString();
            } else {
                System.out.println(result.message);
            }

            // 写
            outputStream.write(response.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            // 读取POST请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            // 拼字符串的
            StringBuilder requestBodyBuilder = new StringBuilder();
            // 用来读的
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }

            // 看看读到了啥
            // 实际处理可能会更复杂点
            System.out.println("Received POST request to create card with data: " + requestBodyBuilder.toString());

            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            String response = "";

            LibraryManagementSystemImpl library = new LibraryManagementSystemImpl(connector);

            JSONObject jsonObject = JSON.parseObject(requestBodyBuilder.toString());
            String action = jsonObject.getString("action");
            if (action.equals("CreateCard")) {
                Card card = new Card();
                card.setName((String) jsonObject.get("name"));
                card.setDepartment((String) jsonObject.get("department"));
                Card.CardType type = null;
                if (jsonObject.get("type").equals("Student")) {
                    type = Card.CardType.values("S");
                } else if (jsonObject.get("type").equals("Teacher")) {
                    type = Card.CardType.values("T");
                }
                card.setType(type);

                ApiResult result = library.registerCard(card);
                if (result.ok) {
                    System.out.println("Card created successfully");
                    response = "Card created successfully";
                    exchange.sendResponseHeaders(200, 0);
                } else {
                    System.out.println(result.message);
                    response = result.message;
                    exchange.sendResponseHeaders(405, -1);
                }
            } else if (action.equals("DeleteCard")) {
                int cardId = jsonObject.getIntValue("cardId");
                ApiResult result = library.removeCard(cardId);
                if (result.ok) {
                    System.out.println("Card deleted successfully");
                    response = "Card deleted successfully";
                    exchange.sendResponseHeaders(200, 0);
                } else {
                    System.out.println(result.message);
                    response = result.message;
                    exchange.sendResponseHeaders(405, -1);
                }
            } else if (action.equals("ModifyCard")) {
                Card card = new Card();
                card.setCardId(jsonObject.getIntValue("cardId"));
                card.setName((String) jsonObject.get("name"));
                card.setDepartment((String) jsonObject.get("department"));
                Card.CardType type = null;
                if (jsonObject.get("type").equals("Student")) {
                    type = Card.CardType.values("S");
                } else if (jsonObject.get("type").equals("Teacher")) {
                    type = Card.CardType.values("T");
                }
                card.setType(type);

                ApiResult result = library.modifyCard(card);
                if (result.ok) {
                    System.out.println("Card modified successfully");
                    response = "Card modified successfully";
                    exchange.sendResponseHeaders(200, 0);
                } else {
                    System.out.println(result.message);
                    response = result.message;
                    exchange.sendResponseHeaders(405, -1);
                }
            }

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // OPTIONS请求直接返回204 No Content
            exchange.sendResponseHeaders(204, -1);
        }
    }

    static class BorrowHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            }  else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private static Map<String, String> parseQueryParams(String query) {
            Map<String, String> queryParams = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];
                        queryParams.put(key, value);
                    }
                }
            }
            return queryParams;
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            System.out.println("Received GET request for borrowHistory with query: " + query);

            Map<String, String> params = parseQueryParams(query);

            int cardId = Integer.parseInt(params.get("cardId"));

            LibraryManagementSystemImpl library = new LibraryManagementSystemImpl(connector);
            ApiResult result = library.showBorrowHistory(cardId);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            String response = "";

            if (result.ok) {
                // [{"count":1,"items":[{"author":"Yuuku","bookId":1,"borrowTime":1743091450180,"cardId":1,"category":"Nature",
                // "press":"Press-C","price":198.46,"publishYear":2000,"returnTime":0,"title":"Le Petit Prince"}]}]
                JSONArray jsonArray = new JSONArray(result.payload);
                JSONArray borrowHistory = jsonArray.getJSONObject(0).getJSONArray("items");
                response = borrowHistory.toString();
            } else {
                System.out.println(result.message);
            }

            // 写
            outputStream.write(response.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // OPTIONS请求直接返回204 No Content
            exchange.sendResponseHeaders(204, -1);
        }
    }

    static class BookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 允许所有域的请求，cors处理
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            // 解析请求的方法，看GET还是POST
            String requestMethod = exchange.getRequestMethod();
            // 注意判断要用equals方法而不是==啊，java的小坑（
            if (requestMethod.equals("GET")) {
                // 处理GET
                handleGetRequest(exchange);
            }  else if (requestMethod.equals("POST")) {
                // 处理POST
                handlePostRequest(exchange);
            } else if (requestMethod.equals("OPTIONS")) {
                // 处理OPTIONS
                handleOptionsRequest(exchange);
            } else {
                // 其他请求返回405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private static Map<String, String> parseQueryParams(String query) {
            Map<String, String> queryParams = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];
                        queryParams.put(key, value);
                    }
                }
            }
            return queryParams;
        }

        private void handleGetRequest(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            System.out.println("Received GET request for borrowHistory with query: " + query);

            BookQueryConditions conditions = new BookQueryConditions();

            Map<String, String> params = parseQueryParams(query);
            String Category = params.get("category");
            String Title = params.get("title");
            String Author = params.get("author");
            String Press = params.get("press");
            if (params.get("minPublishYear") != null) {
                int MinPublishYear = Integer.parseInt(params.get("minPublishYear"));
                conditions.setMinPublishYear(MinPublishYear);
            }
            if (params.get("maxPublishYear") != null) {
                int MaxPublishYear = Integer.parseInt(params.get("maxPublishYear"));
                conditions.setMaxPublishYear(MaxPublishYear);
            }
            if (params.get("minPrice") != null) {
                double MinPrice = Double.parseDouble(params.get("minPrice"));
                conditions.setMinPrice(MinPrice);
            }
            if (params.get("maxPrice") != null) {
                double MaxPrice = Double.parseDouble(params.get("maxPrice"));
                conditions.setMaxPrice(MaxPrice);
            }
            if (params.get("sortBy") != null) {
                String sortBy = params.get("sortBy");
                if (sortBy.equals("category")) {
                    conditions.setSortBy(Book.SortColumn.CATEGORY);
                } else if (sortBy.equals("title")) {
                    conditions.setSortBy(Book.SortColumn.TITLE);
                } else if (sortBy.equals("author")) {
                    conditions.setSortBy(Book.SortColumn.AUTHOR);
                } else if (sortBy.equals("press")) {
                    conditions.setSortBy(Book.SortColumn.PRESS);
                } else if (sortBy.equals("publishyear")) {
                    conditions.setSortBy(Book.SortColumn.PUBLISH_YEAR);
                } else if (sortBy.equals("price")) {
                    conditions.setSortBy(Book.SortColumn.PRICE);
                } else if (sortBy.equals("stock")) {
                    conditions.setSortBy(Book.SortColumn.STOCK);
                } else if (sortBy.equals("bookid")) {
                    conditions.setSortBy(Book.SortColumn.BOOK_ID);
                }

                String sortOrder = params.get("sortOrder");
                if (sortOrder.equals("ASC")) {
                    conditions.setSortOrder(SortOrder.ASC);
                } else if (sortOrder.equals("DESC")) {
                    conditions.setSortOrder(SortOrder.DESC);
                }
            }

            conditions.setCategory(Category);
            conditions.setTitle(Title);
            conditions.setPress(Press);
            conditions.setAuthor(Author);

            // 响应头，因为是JSON通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为200，也就是status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();
            // 构建JSON响应数据，这里简化为字符串
            // 这里写的一个固定的JSON，实际可以查表获取数据，然后再拼出想要的JSON
            String response = "";

            LibraryManagementSystemImpl library = new LibraryManagementSystemImpl(connector);
            ApiResult result = library.queryBook(conditions);
            if (result.ok) {
                // [{"count":1,"results":[{"author":"Yuuku","bookId":1,"category":"Nature","press":"Press-C","price":198.46,"publishYear":2000,"stock":0,"title":"Le Petit Prince"}]}]
                JSONArray jsonArray = new JSONArray(result.payload);
                JSONArray books = jsonArray.getJSONObject(0).getJSONArray("results");
                response = books.toString();
            } else {
                System.out.println(result.message);
            }

            // 写
            outputStream.write(response.getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        }

        private void handlePostRequest(HttpExchange exchange) throws IOException {
            InputStream requestBody = exchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            StringBuilder requestBodyBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            System.out.println("Received POST request to create book with data: " + requestBodyBuilder.toString());

            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            String response = "";

            LibraryManagementSystemImpl library = new LibraryManagementSystemImpl(connector);
            JSONObject jsonObject = JSON.parseObject(requestBodyBuilder.toString());
            String action = jsonObject.getString("action");
            if (action.equals("AddBook")) {
                Book book = new Book();
                book.setTitle((String) jsonObject.get("title"));
                book.setCategory((String) jsonObject.get("category"));
                book.setPress((String) jsonObject.get("press"));
                book.setAuthor((String) jsonObject.get("author"));
                book.setPublishYear(jsonObject.getIntValue("publishYear"));
                book.setPrice(jsonObject.getDoubleValue("price"));
                book.setStock(jsonObject.getIntValue("stock"));

                ApiResult result = library.storeBook(book);
                if (result.ok) {
                    System.out.println("Book created successfully");
                    response = "Book created successfully";
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                } else {
                    System.out.println(result.message);
                    response = result.message;
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                }
            } else if (action.equals("AddBatch")) {
                // D:\study\year2sem2\dbs\db_lab\lab5\librarymanagementsystem-master\src\test\BookBatch.json
                String filePath = (String) jsonObject.get("path");
                File file = new File(filePath);
                if (!file.exists()) {
                    System.out.println("File not found: " + filePath);
                    response = "File not found";
                    exchange.sendResponseHeaders(404, response.getBytes().length);
                } else {
                    String jsonString = new String(java.nio.file.Files.readAllBytes(file.toPath()));
                    JSONArray jsonArray = JSON.parseArray(jsonString);
                    JSONArray books = jsonArray.getJSONObject(0).getJSONArray("books");

                    ArrayList<Book> bookList = new ArrayList<>();
                    for (int i = 0; i < books.size(); i++) {
                        Book book = new Book();
                        JSONObject bookJson = books.getJSONObject(i);
                        book.setTitle(bookJson.getString("title"));
                        book.setCategory(bookJson.getString("category"));
                        book.setPress(bookJson.getString("press"));
                        book.setAuthor(bookJson.getString("author"));
                        book.setPublishYear(bookJson.getIntValue("publishYear"));
                        book.setPrice(bookJson.getDoubleValue("price"));
                        book.setStock(bookJson.getIntValue("stock"));
                        bookList.add(book);
                    }
                    ApiResult result = library.storeBook(bookList);

                    if (result.ok) {
                        System.out.println("Books created successfully");
                        response = "Books created successfully";
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                    } else {
                        System.out.println(result.message);
                        response = result.message;
                        exchange.sendResponseHeaders(405, response.getBytes().length);
                    }
                }
            } else if (action.equals("ModifyStock")) {
                int bookId = jsonObject.getIntValue("bookId");
                int deltaStock = jsonObject.getIntValue("deltaStock");
                ApiResult result = library.incBookStock(bookId, deltaStock);
                if (result.ok) {
                    System.out.println("Book stock modified successfully, the current stock is " + result.payload);
                    response = "Book stock modified successfully, the current stock is " + result.payload;
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                } else {
                    System.out.println(result.message);
                    response = result.message;
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                }
            } else if (action.equals("ModifyBook")) {
                Book book = new Book();
                book.setBookId(jsonObject.getIntValue("bookId"));
                book.setTitle((String) jsonObject.get("title"));
                book.setCategory((String) jsonObject.get("category"));
                book.setPress((String) jsonObject.get("press"));
                book.setAuthor((String) jsonObject.get("author"));
                book.setPublishYear(jsonObject.getIntValue("publishYear"));
                book.setPrice(jsonObject.getDoubleValue("price"));
                book.setStock(jsonObject.getIntValue("stock"));

                ApiResult result = library.modifyBookInfo(book);
                if (result.ok) {
                    System.out.println("Book modified successfully");
                    response = "Book modified successfully";
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                } else {
                    System.out.println(result.message);
                    response = result.message;
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                }
            } else if (action.equals("DeleteBook")) {
                int bookId = jsonObject.getIntValue("bookId");
                ApiResult result = library.removeBook(bookId);
                if (result.ok) {
                    System.out.println("Book deleted successfully");
                    response = "Book deleted successfully";
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                } else {
                    System.out.println(result.message);
                    response = result.message;
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                }
            } else if (action.equals("BorrowBook")) {
                Borrow borrow = new Borrow();
                int bookId = jsonObject.getIntValue("bookId");
                int cardId = jsonObject.getIntValue("cardId");
                long borrowTime = jsonObject.getLong("borrowTime");

                borrow.setBookId(bookId);
                borrow.setCardId(cardId);
                borrow.setBorrowTime(borrowTime);
                borrow.setReturnTime(0);

                ApiResult result = library.borrowBook(borrow);
                if (result.ok) {
                    System.out.println("Book borrowed successfully");
                    response = "Book borrowed successfully";
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                } else {
                    System.out.println(result.message);
                    response = result.message;
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                }
            } else if (action.equals("ReturnBook")) {
                Borrow borrow = new Borrow();
                int bookId = jsonObject.getIntValue("bookId");
                int cardId = jsonObject.getIntValue("cardId");
                long returnTime = jsonObject.getLong("returnTime");
                borrow.setBookId(bookId);
                borrow.setCardId(cardId);
                borrow.setReturnTime(returnTime);

                ApiResult result = library.returnBook(borrow);
                if (result.ok) {
                    System.out.println("Book returned successfully");
                    response = "Book returned successfully";
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                } else {
                    System.out.println(result.message);
                    response = result.message;
                    exchange.sendResponseHeaders(405, response.getBytes().length);
                }
            }

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
        private void handleOptionsRequest(HttpExchange exchange) throws IOException {
            // OPTIONS请求直接返回204 No Content
            exchange.sendResponseHeaders(204, -1);
        }
    }
}

