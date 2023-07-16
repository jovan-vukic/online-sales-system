package rs.etf.sab.solution;

import rs.etf.sab.operations.CityOperations;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the CityOperations interface and provides methods related to cities.
 */
public class SolutionCityOperations implements CityOperations {
    private static final Connection c = DB.getInstance().getConnection();

    /**
     * Creates a new city with the specified unique name.
     *
     * @param cityName the name of the city to create
     * @return the ID of the newly created city, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int createCity(String cityName) {
        if (cityExists(cityName)) return -1;

        String query = "INSERT INTO City (Name) VALUES (?)";
        int generatedKey = -1;

        try (PreparedStatement ps = c.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cityName);

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
     * Retrieves a list of all city IDs.
     *
     * @return a list of all city IDs, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public List<Integer> getCities() {
        String query = "SELECT Id FROM City";
        List<Integer> allCities = new ArrayList<>();

        try (
                PreparedStatement ps = c.prepareStatement(query);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                allCities.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return allCities.isEmpty() ? null : allCities;
    }

    /**
     * Connects two cities with the given IDs and sets the distance between them.
     * There can be max one line between cities.
     *
     * @param idCity1  the ID of the first city
     * @param idCity2  the ID of the second city
     * @param distance the distance between the cities (measured in days)
     * @return the ID of the newly created connection, or -1 otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public int connectCities(int idCity1, int idCity2, int distance) {
        if (lineExists(idCity1, idCity2)) return -1;

        String query = "INSERT INTO Line (Id1, Id2, Distance) VALUES (?, ?, ?)";
        int generatedKey = -1;

        try (PreparedStatement ps = c.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idCity1);
            ps.setInt(2, idCity2);
            ps.setInt(3, distance);

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
     * Retrieves a list of IDs of cities connected to the specified city.
     *
     * @param idCity the ID of the specified city
     * @return a list of IDs of cities connected to the specified city, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public List<Integer> getConnectedCities(int idCity) {
        String query = "SELECT Id1, Id2 FROM Line WHERE Id1 = ? OR Id2 = ?";
        List<Integer> allConnectedCities = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idCity);
            ps.setInt(2, idCity);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id1 = rs.getInt("Id1");
                    int id2 = rs.getInt("Id2");

                    int connectedCity = (id1 == idCity) ? id2 : id1;
                    allConnectedCities.add(connectedCity);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return allConnectedCities.isEmpty() ? null : allConnectedCities;
    }

    /**
     * Retrieves a list of shop IDs in the specified city.
     *
     * @param idCity the ID of the specified city
     * @return a list of shop IDs in the specified city, or null otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    @Override
    public List<Integer> getShops(int idCity) {
        String query = "SELECT Id FROM Shop WHERE IdCity = ?";
        List<Integer> shopsInCity = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idCity);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    shopsInCity.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return shopsInCity.isEmpty() ? null : shopsInCity;
    }

    /**
     * Checks if a city with the given name exists.
     *
     * @param cityName the name of the city to check
     * @return true if the city exists, false otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    private boolean cityExists(String cityName) {
        String query = "SELECT 1 FROM City WHERE Name = ?";

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setString(1, cityName);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if a line/connection between two cities with the given IDs exists.
     *
     * @param idCity1 the ID of the first city
     * @param idCity2 the ID of the second city
     * @return true if the line/connection exists, false otherwise
     * @throws RuntimeException if an SQL exception occurs during the operation
     */
    private boolean lineExists(int idCity1, int idCity2) {
        String query = "SELECT 1 FROM Line WHERE (Id1 = ? AND Id2 = ?) OR (Id1 = ? AND Id2 = ?)";

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idCity1);
            ps.setInt(2, idCity2);
            ps.setInt(3, idCity2);
            ps.setInt(4, idCity1);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
