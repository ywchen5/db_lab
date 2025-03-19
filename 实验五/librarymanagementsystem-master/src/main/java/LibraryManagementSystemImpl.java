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
            pStmt = conn.prepareStatement(insert_sql);
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
            PreparedStatement pStmt = conn.prepareStatement(exist_check_sql);
            for (Book book : books) {
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
            }

            String insert_sql = "INSERT INTO book (category, title, press, publish_year, author, price, stock) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)"; // insert the book
            pStmt = conn.prepareStatement(insert_sql);
            for (Book book : books) {
                pStmt.setString(1, book.getCategory());
                pStmt.setString(2, book.getTitle());
                pStmt.setString(3, book.getPress());
                pStmt.setInt(4, book.getPublishYear());
                pStmt.setString(5, book.getAuthor());
                pStmt.setDouble(6, book.getPrice());
                pStmt.setInt(7, book.getStock());
                pStmt.addBatch(); // add the batch
            }
            pStmt.executeBatch(); // execute the batch
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
                    "author = ?, price = ? WHERE book_id = ? AND stock = ?"; // update the book
            PreparedStatement pStmt = conn.prepareStatement(update_sql);
            pStmt.setString(1, book.getCategory());
            pStmt.setString(2, book.getTitle());
            pStmt.setString(3, book.getPress());
            pStmt.setInt(4, book.getPublishYear());
            pStmt.setString(5, book.getAuthor());
            pStmt.setDouble(6, book.getPrice());
            pStmt.setInt(7, book.getBookId());
            pStmt.setInt(8, book.getStock());
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
            query_sql.append(" ORDER BY ").append(conditions.getSortBy().toString())
                     .append(" ").append(conditions.getSortOrder().toString())
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
            }

            commit(conn); // commit the transaction
            return new ApiResult(true, books); // return the books
        } catch (SQLException e) {
            return new ApiResult(false, e.getMessage());
        }
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult registerCard(Card card) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult removeCard(int cardId) {
        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult showCards() {
        return new ApiResult(false, "Unimplemented Function");
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
