import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
    public ApiResult storeBook(Book book) {
        try {
            Connection conn = connector.getConn(); // get connection
            String exist_check_sql = "SELECT 1 FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ? LIMIT 1"; // check if the book already exists
            PreparedStatement pStmt = conn.prepareStatement(exist_check_sql);
            pStmt.setString(1, book.getCategory());
            pStmt.setString(2, book.getTitle());
            pStmt.setString(3, book.getPress());
            pStmt.setInt(4, book.getPublishYear());
            pStmt.setString(5, book.getAuthor());

            ResultSet rs = pStmt.executeQuery(); // execute the check query
            if (rs.next()) { // if the book to be stored already exists
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "Book to be stored already exists");
            }

            String insert_sql = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)"; // insert the book
            pStmt = conn.prepareStatement(insert_sql, Statement.RETURN_GENERATED_KEYS);
            pStmt.setString(1, book.getCategory());
            pStmt.setString(2, book.getTitle());
            pStmt.setString(3, book.getPress());
            pStmt.setInt(4, book.getPublishYear());
            pStmt.setString(5, book.getAuthor());
            pStmt.setDouble(6, book.getPrice());
            pStmt.setInt(7, book.getStock());
            pStmt.executeUpdate();

            rs = pStmt.getGeneratedKeys(); // get the generated keys, namely the book id
            if (rs.next()) {
                int bookId = rs.getInt(1);
                book.setBookId(bookId);
                commit(conn); // commit the transaction
                return new ApiResult(true, bookId); // return the book id
            } else { // if failed to get the book id
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "Failed to get book id after storing a book");
            }
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        try {
            Connection conn = connector.getConn();
            String stock_check_sql = "SELECT stock FROM book WHERE book_id = ?"; // check the stock of the book
            PreparedStatement pStmt = conn.prepareStatement(stock_check_sql);
            pStmt.setInt(1, bookId);
            ResultSet rs = pStmt.executeQuery();
            if (!rs.next()) { // if the book does not exist
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "Book to be updated does not exist");
            } else {
                int stock = rs.getInt(1);
                if (stock + deltaStock < 0) { // if the stock is not enough
                    rollback(conn); // rollback the transaction
                    return new ApiResult(false, "Stock is not enough");
                }
                String update_sql = "UPDATE book SET stock = stock + ? WHERE book_id = ?"; // update the stock
                pStmt = conn.prepareStatement(update_sql);
                pStmt.setInt(1, deltaStock);
                pStmt.setInt(2, bookId);
                int affectedRows = pStmt.executeUpdate();
                if (affectedRows == 0) { // if failed to update the stock
                    rollback(conn); // rollback the transaction
                    return new ApiResult(false, "No such book to update stock");
                }
                commit(conn); // commit the transaction
                return new ApiResult(true, "Stock updated successfully");
            }
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult storeBook(List<Book> books) {
        try {
            Connection conn = connector.getConn();
            String exist_check_sql = "SELECT 1 FROM book WHERE category = ? AND title = ? " +
                    "AND press = ? AND publish_year = ? AND author = ? LIMIT 1"; // check if the book already exists
            String insert_sql = "INSERT INTO book (category, title, press, publish_year, author, price, stock) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)"; // insert the book
            PreparedStatement pStmt = conn.prepareStatement(exist_check_sql);
            List<Integer> bookIds = new ArrayList<Integer>(); // store the book ids
            for (Book book : books) {
                pStmt = conn.prepareStatement(exist_check_sql);
                pStmt.setString(1, book.getCategory());
                pStmt.setString(2, book.getTitle());
                pStmt.setString(3, book.getPress());
                pStmt.setInt(4, book.getPublishYear());
                pStmt.setString(5, book.getAuthor());
                ResultSet rs = pStmt.executeQuery();
                if (rs.next()) { // if the book to be stored already exists
                    rollback(conn); // rollback the transaction
                    return new ApiResult(false, "Book to be stored already exists");
                }

                pStmt = conn.prepareStatement(insert_sql, Statement.RETURN_GENERATED_KEYS);
                pStmt.setString(1, book.getCategory());
                pStmt.setString(2, book.getTitle());
                pStmt.setString(3, book.getPress());
                pStmt.setInt(4, book.getPublishYear());
                pStmt.setString(5, book.getAuthor());
                pStmt.setDouble(6, book.getPrice());
                pStmt.setInt(7, book.getStock());
                pStmt.executeUpdate();
                rs = pStmt.getGeneratedKeys(); // get the generated keys, namely the book id
                if (rs.next()) {
                    int bookId = rs.getInt(1);
                    bookIds.add(bookId);
                } else { // if failed to get the book id
                    rollback(conn); // rollback the transaction
                    return new ApiResult(false, "Failed to get book id after storing a book");
                }
            }

            for (int i = 0; i < books.size(); i++) {
                books.get(i).setBookId(bookIds.get(i));
            }
            commit(conn); // commit the transaction
            return new ApiResult(true, "Books stored successfully");
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }



    @Override
    public ApiResult removeBook(int bookId) {
        try {
            Connection conn = connector.getConn();
            String borrow_check_sql = "SELECT 1 FROM borrow WHERE book_id = ? " +
                    "AND return_time = 0 LIMIT 1"; // check if the book is borrowed
            PreparedStatement pStmt = conn.prepareStatement(borrow_check_sql);
            pStmt.setInt(1, bookId);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) { // if the book is borrowed
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "Book to be removed has not been returned");
            }

            String delete_sql = "DELETE FROM book WHERE book_id = ?"; // delete the book
            pStmt = conn.prepareStatement(delete_sql);
            pStmt.setInt(1, bookId);
            int affectedRows = pStmt.executeUpdate();
            if (affectedRows == 0) { // if failed to delete the book
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "No such book to be removed");
            }
            commit(conn); // commit the transaction
            return new ApiResult(true, "Book removed successfully");
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult modifyBookInfo(Book book) {
        try {
            Connection conn = connector.getConn();
            String update_sql = "UPDATE book SET category = ?, title = ?, press = ?, publish_year = ?, " +
                    "author = ?, price = ? WHERE book_id = ?"; // update the book
            PreparedStatement pStmt = conn.prepareStatement(update_sql);
            pStmt.setString(1, book.getCategory());
            pStmt.setString(2, book.getTitle());
            pStmt.setString(3, book.getPress());
            pStmt.setInt(4, book.getPublishYear());
            pStmt.setString(5, book.getAuthor());
            pStmt.setDouble(6, book.getPrice());
            pStmt.setInt(7, book.getBookId());
            int affectedRows = pStmt.executeUpdate();
            if (affectedRows == 0) { // if failed to update the book
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "No such book to be updated");
            }
            commit(conn); // commit the transaction
            return new ApiResult(true, "Book modified successfully");
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        try {
            Connection conn = connector.getConn();
            StringBuilder query_sql = new StringBuilder("SELECT * FROM book WHERE 1 = 1"); // query the book
            if (conditions.getCategory() != null) {
                query_sql.append(" AND category = ?"); // add the exact category condition
            }
            if (conditions.getTitle() != null) {
                query_sql.append(" AND title LIKE ?"); // add the fuzzy title condition
            }
            if (conditions.getPress() != null) {
                query_sql.append(" AND press LIKE ?"); // add the fuzzy press condition
            }
            if (conditions.getMinPublishYear() != null) {
                query_sql.append(" AND publish_year >= ?"); // add the min publish year condition
            }
            if (conditions.getMaxPublishYear() != null) {
                query_sql.append(" AND publish_year <= ?"); // add the max publish year condition
            }
            if (conditions.getAuthor() != null) {
                query_sql.append(" AND author LIKE ?"); // add the fuzzy author condition
            }
            if (conditions.getMinPrice() != null) {
                query_sql.append(" AND price >= ?"); // add the min price condition
            }
            if (conditions.getMaxPrice() != null) {
                query_sql.append(" AND price <= ?"); // add the max price condition
            }
            query_sql.append(" ORDER BY ").append(conditions.getSortBy().getValue())
                     .append(" ").append(conditions.getSortOrder().getValue())
                    .append(", book_id ASC"); // sort the result
            PreparedStatement pStmt = conn.prepareStatement(query_sql.toString());

            int parameterIndex = 1;
            if (conditions.getCategory() != null) {
                pStmt.setString(parameterIndex++, conditions.getCategory());
            }
            if (conditions.getTitle() != null) {
                pStmt.setString(parameterIndex++, "%" + conditions.getTitle() + "%");
            }
            if (conditions.getPress() != null) {
                pStmt.setString(parameterIndex++, "%" + conditions.getPress() + "%");
            }
            if (conditions.getMinPublishYear() != null) {
                pStmt.setInt(parameterIndex++, conditions.getMinPublishYear());
            }
            if (conditions.getMaxPublishYear() != null) {
                pStmt.setInt(parameterIndex++, conditions.getMaxPublishYear());
            }
            if (conditions.getAuthor() != null) {
                pStmt.setString(parameterIndex++, "%" + conditions.getAuthor() + "%");
            }
            if (conditions.getMinPrice() != null) {
                pStmt.setDouble(parameterIndex++, conditions.getMinPrice());
            }
            if (conditions.getMaxPrice() != null) {
                pStmt.setDouble(parameterIndex, conditions.getMaxPrice());
            }

            List<Book> books = new ArrayList<Book>(); // store the books
            int count = 0; // store the number of books
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                Book book = new Book();
                book.setBookId(rs.getInt("book_id"));
                book.setCategory(rs.getString("category"));
                book.setTitle(rs.getString("title"));
                book.setPress(rs.getString("press"));
                book.setPublishYear(rs.getInt("publish_year"));
                book.setAuthor(rs.getString("author"));
                book.setPrice(rs.getDouble("price"));
                book.setStock(rs.getInt("stock"));
                books.add(book);
                count++;
            }

            BookQueryResults bookQueryResults = new BookQueryResults(books); // store the book query results
            bookQueryResults.setCount(count);
            bookQueryResults.setResults(books);

            commit(conn); // commit the transaction
            return new ApiResult(true, bookQueryResults);
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        try {
            Connection conn = connector.getConn();
            String borrow_check_sql = "SELECT 1 FROM borrow WHERE card_id = ? AND " +
                    "book_id = ? AND return_time = 0 LIMIT 1"; // check if the book is borrowed
            PreparedStatement pStmt = conn.prepareStatement(borrow_check_sql);
            pStmt.setInt(1, borrow.getCardId());
            pStmt.setInt(2, borrow.getBookId());
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) { // if the book is borrowed
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "Book to be borrowed has not been returned");
            }

            String stock_check_sql = "SELECT stock FROM book WHERE book_id = ? FOR UPDATE"; // check the stock of the book
            pStmt = conn.prepareStatement(stock_check_sql);
            pStmt.setInt(1, borrow.getBookId());
            rs = pStmt.executeQuery();
            if (!rs.next()) { // if the book does not exist
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "Book to be borrowed does not exist");
            } else {
                int stock = rs.getInt(1);
                if (stock <= 0) { // if the stock is not enough
                    rollback(conn); // rollback the transaction
                    return new ApiResult(false, "Stock is not enough");
                }
            }

            String insert_sql = "INSERT INTO borrow (card_id, book_id, borrow_time, return_time) VALUES (?, ?, ?, 0)"; // insert the borrow record
            pStmt = conn.prepareStatement(insert_sql);
            pStmt.setInt(1, borrow.getCardId());
            pStmt.setInt(2, borrow.getBookId());
            pStmt.setLong(3, borrow.getBorrowTime());
            pStmt.executeUpdate();

            String update_sql = "UPDATE book SET stock = stock - 1 WHERE book_id = ? AND stock > 0"; // update the stock of the book
            pStmt = conn.prepareStatement(update_sql);
            pStmt.setInt(1, borrow.getBookId());
            pStmt.executeUpdate();

            commit(conn); // commit the transaction
            return new ApiResult(true, "Book borrowed successfully");
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        try {
            Connection conn = connector.getConn();
            String return_check_sql = "SELECT borrow_time FROM borrow WHERE card_id = ? " +
                    "AND book_id = ? AND return_time = 0"; // check if the book is borrowed
            PreparedStatement pStmt = conn.prepareStatement(return_check_sql);
            pStmt.setInt(1, borrow.getCardId());
            pStmt.setInt(2, borrow.getBookId());
            ResultSet rs = pStmt.executeQuery();
            if (!rs.next()) { // if the book is not borrowed
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "Book to be returned has not been borrowed");
            } else {
                long borrowTime = rs.getLong("borrow_time");
                if (borrowTime >= borrow.getReturnTime()) { // if the return time is earlier than the borrow time
                    rollback(conn); // rollback the transaction
                    return new ApiResult(false, "Return time is earlier than borrow time");
                }
            }

            String update_sql = "UPDATE borrow SET return_time = ? WHERE card_id = ? " +
                    "AND book_id = ? AND return_time = 0"; // update the return time
            pStmt = conn.prepareStatement(update_sql);
            pStmt.setLong(1, borrow.getReturnTime());
            pStmt.setInt(2, borrow.getCardId());
            pStmt.setInt(3, borrow.getBookId());
            pStmt.executeUpdate();

            String update_stock_sql = "UPDATE book SET stock = stock + 1 WHERE book_id = ?"; // update the stock of the book
            pStmt = conn.prepareStatement(update_stock_sql);
            pStmt.setInt(1, borrow.getBookId());
            pStmt.executeUpdate();

            commit(conn);
            return new ApiResult(true, "Book returned successfully");
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        try {
            Connection conn = connector.getConn();
            String query_sql = "SELECT * FROM borrow NATURAL JOIN book WHERE card_id = ? ORDER BY borrow_time DESC, book_id ASC"; // query the borrow history
            PreparedStatement pStmt = conn.prepareStatement(query_sql);
            pStmt.setInt(1, cardId);
            ResultSet rs = pStmt.executeQuery();

            List<BorrowHistories.Item> items = new ArrayList<BorrowHistories.Item>(); // store the borrow history
            int count = 0; // store the number of borrow history
            while (rs.next()) {
                BorrowHistories.Item item = new BorrowHistories.Item();
                item.setCardId(rs.getInt("card_id"));
                item.setBookId(rs.getInt("book_id"));
                item.setCategory(rs.getString("category"));
                item.setTitle(rs.getString("title"));
                item.setPress(rs.getString("press"));
                item.setPublishYear(rs.getInt("publish_year"));
                item.setAuthor(rs.getString("author"));
                item.setPrice(rs.getDouble("price"));
                item.setBorrowTime(rs.getLong("borrow_time"));
                item.setReturnTime(rs.getLong("return_time"));
                items.add(item);
                count++;
            }

            BorrowHistories borrowHistories = new BorrowHistories(items); // store the borrow histories
            borrowHistories.setCount(count);
            borrowHistories.setItems(items);

            commit(conn); // commit the transaction
            return new ApiResult(true, borrowHistories); // return the borrow histories
    } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult registerCard(Card card) {
        try {
            Connection conn = connector.getConn();
            String exist_check_sql = "SELECT 1 FROM card WHERE name = ? AND department = ? AND type = ? LIMIT 1"; // check if the card already exists
            PreparedStatement pStmt = conn.prepareStatement(exist_check_sql);
            pStmt.setString(1, card.getName());
            pStmt.setString(2, card.getDepartment());
            pStmt.setString(3, card.getType().getStr());
            ResultSet rs = pStmt.executeQuery();

            if (rs.next()) { // if the card to be registered already exists
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "Card to be registered already exists");
            }

            String insert_sql = "INSERT INTO card (name, department, type) VALUES (?, ?, ?)"; // insert the card
            pStmt = conn.prepareStatement(insert_sql, Statement.RETURN_GENERATED_KEYS);
            pStmt.setString(1, card.getName());
            pStmt.setString(2, card.getDepartment());
            pStmt.setString(3, card.getType().getStr());
            pStmt.executeUpdate();
            rs = pStmt.getGeneratedKeys(); // get the generated keys, namely the card id
            if (rs.next()) {
                int cardId = rs.getInt(1);
                card.setCardId(cardId);
                commit(conn); // commit the transaction
                return new ApiResult(true, "Card registered successfully");
            } else { // if failed to get the card id
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "Failed to get card id after registering a card");
            }
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult modifyCard(Card card) {
        try {
            Connection conn = connector.getConn();
            String update_sql = "UPDATE card SET name = ?, department = ?, type = ? WHERE card_id = ?"; // update the card
            PreparedStatement pStmt = conn.prepareStatement(update_sql);
            pStmt.setString(1, card.getName());
            pStmt.setString(2, card.getDepartment());
            pStmt.setString(3, card.getType().getStr());
            pStmt.setInt(4, card.getCardId());
            int affectedRows = pStmt.executeUpdate();
            if (affectedRows == 0) { // if failed to update the card
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "No such card to be updated");
            }
            commit(conn); // commit the transaction
            return new ApiResult(true, "Card modified successfully");
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult removeCard(int cardId) {
        try {
            Connection conn = connector.getConn();
            String borrow_check_sql = "SELECT 1 FROM borrow WHERE card_id = ? AND return_time = 0 LIMIT 1"; // check if the card has borrowed books
            PreparedStatement pStmt = conn.prepareStatement(borrow_check_sql);
            pStmt.setInt(1, cardId);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) { // if the card has borrowed books
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "Card to be removed has not returned books");
            }

            String delete_sql = "DELETE FROM card WHERE card_id = ?"; // delete the card
            pStmt = conn.prepareStatement(delete_sql);
            pStmt.setInt(1, cardId);
            int affectedRows = pStmt.executeUpdate();
            if (affectedRows == 0) { // if failed to delete the card
                rollback(conn); // rollback the transaction
                return new ApiResult(false, "No such card to be removed");
            }

            commit(conn); // commit the transaction
            return new ApiResult(true, "Card removed successfully");
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult showCards() {
        try {
            Connection conn = connector.getConn();
            String query_sql = "SELECT * FROM card ORDER BY card_id ASC"; // query the cards
            PreparedStatement pStmt = conn.prepareStatement(query_sql);
            ResultSet rs = pStmt.executeQuery();

            List<Card> cards = new ArrayList<Card>(); // store the cards
            int count = 0; // store the number of cards
            while (rs.next()) {
                Card card = new Card();
                card.setCardId(rs.getInt("card_id"));
                card.setName(rs.getString("name"));
                card.setDepartment(rs.getString("department"));
                card.setType(Card.CardType.values(rs.getString("type")));
                cards.add(card);
                count++;
            }

            CardList cardList = new CardList(cards); // store the card list
            cardList.setCount(count);
            cardList.setCards(cards);

            commit(conn); // commit the transaction
            return new ApiResult(true, cardList); // return the card list
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
