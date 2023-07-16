package rs.etf.sab.solution;

import rs.etf.sab.operations.ArticleOperations;

import java.math.BigDecimal;
import java.sql.*;

/**
 * Implements the ArticleOperations interface and provides methods related to articles.
 */
public class SolutionArticleOperations implements ArticleOperations {
    private static final Connection c = DB.getInstance().getConnection();

    /**
     * Creates an article with the specified details and quantity equal to zero and associates it with the given shop.
     *
     * @param idShop       the ID of the shop to associate the article with
     * @param articleName  the name of the article
     * @param articlePrice the price of the article
     * @return the ID of the created article, or -1 otherwise
     * @throws RuntimeException if a database error occurs
     */
    @Override
    public int createArticle(int idShop, String articleName, int articlePrice) {
        if (!shopExists(idShop)) return -1;

        String query = "INSERT INTO Article(Name, Price, Quantity, IdShop) VALUES (?, ?, 0, ?)";
        int generatedKey = -1;

        try (PreparedStatement ps = c.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, articleName);
            ps.setBigDecimal(2, BigDecimal.valueOf(articlePrice));
            ps.setInt(3, idShop);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedKey = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return generatedKey;
    }

    /**
     * Checks if a shop with the specified ID exists in the database.
     *
     * @param idShop the ID of the shop to check
     * @return true if the shop exists, false otherwise
     * @throws RuntimeException if a database error occurs
     */
    private boolean shopExists(int idShop) {
        String query = "SELECT 1 FROM Shop WHERE Id = ?";

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idShop);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
