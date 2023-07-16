package rs.etf.sab.solution;

import rs.etf.sab.operations.CityOperations;

import java.sql.*;
import java.util.*;

/**
 * Represents a Dijkstra algorithm implementation for calculating shortest paths in a graph.
 */
public class DijkstraAlgorithm {
    private static final Connection c = DB.getInstance().getConnection();
    private static final CityOperations co = new SolutionCityOperations();

    private int idNearestCity = -1;
    private int maxDistanceToNearestCity = 0;
    private int minDistanceFromBuyerToNearestCity = -1;

    /**
     * Represents a container class for distance and previous city information.
     */
    public static class DistancePreviousContainer {
        int distance;
        int previous;

        public DistancePreviousContainer() {

        }

        public DistancePreviousContainer(int distance, int previous) {
            this.distance = distance;
            this.previous = previous;
        }

        public int getDistance() {
            return distance;
        }

        public int getPrevious() {
            return previous;
        }

        public void setDistance(int distance) {
            this.distance = distance;
        }

        public void setPrevious(int previous) {
            this.previous = previous;
        }
    }

    /**
     * Calculates the shortest path using Dijkstra's algorithm.
     *
     * @param idBuyerCity the ID of the buyer city
     * @param idOrder     the ID of the order
     * @return the list of city IDs representing the shortest path from the nearest city to the buyer city
     */
    public List<Integer> dijkstraAlgorithm(int idBuyerCity, int idOrder) {
        /* Step 1:
         * Create map to store distances between the cities.
         * Look: (idCity -> (idNeighborCity -> distance, ...), ...)
         */
        HashMap<Integer, HashMap<Integer, Integer>> distances = getAllDistances();

        /* Step 2:
         * Create map to store distances and paths from the start city to each city.
         * Look: (idCity -> [distanceFromStartCity, previousCityInShortestPath], ...)
         */
        HashMap<Integer, DistancePreviousContainer> fromBuyerToEachCity = new HashMap<>();

        /* Step 3:
         * Create map to store the minimum distances from the nearest city to each city with ordered article(s).
         * Look: (idCity -> minDistanceFromNearestCity, ...)
         */
        HashMap<Integer, Integer> minDistanceFromNearestToEach = new HashMap<>();

        /* Step 4:
         * Create two lists with all the city ids of the cities yet to be visited.
         * Create a list of the cities to be processed in a breadth-first search (BFS) manner.
         */
        List<Integer> citiesToBeVisited = co.getCities();
        List<Integer> citiesToBeVisitedCopy = new ArrayList<>(citiesToBeVisited);

        Queue<Integer> queue = new LinkedList<>();
        queue.offer(idBuyerCity);

        if (co.getShops(idBuyerCity) == null) {
            while (!queue.isEmpty()) {
                // Retrieve and remove the first city from the queue, indicate that it has been visited
                int idCurrentCity = queue.poll();

                // Casting to Integer is necessary
                // Without it, 'id' would be interpreted as the index rather than as the element
                citiesToBeVisited.remove(Integer.valueOf(idCurrentCity));

                // Determine the distance from the start city to the current city
                int distanceCurrentCity = (idCurrentCity == idBuyerCity ? 0 : fromBuyerToEachCity.get(idCurrentCity).getDistance());

                // Iterate over the neighboring cities of the current city
                for (Integer idNeighbor : distances.get(idCurrentCity).keySet()) {
                    // Check if neighbor is in 'citiesToBeVisited'. If so, add neighbor to the queue
                    if (citiesToBeVisited.contains(idNeighbor)) queue.offer(idNeighbor);

                    // Check if neighbor was added to 'fromBuyerToEachCity'. If so, create distance [buyer -> neighbor]
                    int distanceNeighborCity = fromBuyerToEachCity.getOrDefault(
                            idNeighbor,
                            new DistancePreviousContainer(-1, -1)
                    ).getDistance();

                    // Create the distance "[buyer -> current] + [current -> neighbor]"
                    int distanceNeighborCityThroughCurrent = distanceCurrentCity + distances.get(idCurrentCity).get(idNeighbor);

                    // Compare the distances directly to neighbor and through the current city to neighbor
                    if (distanceNeighborCity == -1 || distanceNeighborCity > distanceNeighborCityThroughCurrent) {
                        // Add for 'idNeighbor' (the first || another) list [distanceFromStartCity, previousCityInShortestPath]
                        DistancePreviousContainer dpc = new DistancePreviousContainer();
                        dpc.setDistance(distanceNeighborCityThroughCurrent);
                        dpc.setPrevious(idCurrentCity);
                        fromBuyerToEachCity.put(idNeighbor, dpc);

                        // If neighbor contains a shop, and it is closer to buyerCity, it becomes 'nearest city'
                        if (co.getShops(idNeighbor) != null
                                && (minDistanceFromBuyerToNearestCity > distanceNeighborCityThroughCurrent
                                || minDistanceFromBuyerToNearestCity == -1)) {
                            minDistanceFromBuyerToNearestCity = distanceNeighborCityThroughCurrent;
                            idNearestCity = idNeighbor;
                        }
                    }
                }
            }
        } else {
            minDistanceFromBuyerToNearestCity = 0;
            idNearestCity = idBuyerCity;
        }

        /* Step 5:
         * For the second part of the algorithm, we add idNearestCity to the queue.
         * Then we find min distances from 'nearest city' to all other cities.
         */
        queue.offer(idNearestCity);

        while (!queue.isEmpty()) {
            // Retrieve and remove the first city from the queue, indicate that it has been visited
            int idCurrentCity = queue.poll();

            // Casting to Integer is necessary
            // Without it, 'id' would be interpreted as the index rather than as the element
            citiesToBeVisitedCopy.remove(Integer.valueOf(idCurrentCity));

            // Determine the distance from the nearest city to the current city
            int distanceCurrentCity = (idCurrentCity == idNearestCity ? 0 : minDistanceFromNearestToEach.get(idCurrentCity));

            // Iterate over the neighboring cities of the current city
            for (Integer idNeighbor : distances.get(idCurrentCity).keySet()) {
                // Check if neighbor is in 'citiesToBeVisited'. If so, add neighbor to the queue
                if (citiesToBeVisitedCopy.contains(idNeighbor)) queue.offer(idNeighbor);

                // Check if neighbor was added to 'minDistanceFromNearestToEach'. If so, create distance [nearest -> neighbor]
                int distanceNeighborCity = minDistanceFromNearestToEach.getOrDefault(idNeighbor, -1);

                // Create the distance "[nearest -> current] + [current -> neighbor]"
                int distanceNeighborCityThroughCurrent = distanceCurrentCity + distances.get(idCurrentCity).get(idNeighbor);

                // Compare the distances directly to neighbor and through the current city to neighbor
                if (distanceNeighborCity == -1 || distanceNeighborCity > distanceNeighborCityThroughCurrent) {
                    // Add for 'idNeighbor' (the first || overwritten) value minDistanceFromNearestCity
                    minDistanceFromNearestToEach.put(idNeighbor, distanceNeighborCityThroughCurrent);
                }
            }
        }

        // Step 6: Retrieve the list of distinct cities of the shops from which items are ordered
        List<Integer> orderCities = getOrderCities(idOrder);

        /* Step 7:
         * Iterate over each of these cities, finding the maxDistance to any of the cities where the order was made.
         * This allows us to determine the (longest) time required for the order to be assembled in the nearest city.
         * */
        for (Integer idCurrentCity : orderCities) {
            int minDistanceFromNearestToCurrent = minDistanceFromNearestToEach.get(idCurrentCity);

            if (idCurrentCity != idNearestCity && minDistanceFromNearestToCurrent > maxDistanceToNearestCity) {
                maxDistanceToNearestCity = minDistanceFromNearestToCurrent;
            }
        }

        // Step 8: Define the path from the 'nearest city' to the 'buyer city'
        return constructPath(idBuyerCity, fromBuyerToEachCity);
    }

    /**
     * Retrieves all distances between cities from the database.
     *
     * @return a map of distances between cities
     */
    private HashMap<Integer, HashMap<Integer, Integer>> getAllDistances() {
        HashMap<Integer, HashMap<Integer, Integer>> distances = new HashMap<>();
        String query = "SELECT * FROM Line";

        try (
                PreparedStatement ps = c.prepareStatement(query);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                int idCity1 = rs.getInt("Id1");
                int idCity2 = rs.getInt("Id2");
                int distance = rs.getInt("Distance");

                distances.computeIfAbsent(idCity1, k -> new HashMap<>()).put(idCity2, distance);
                distances.computeIfAbsent(idCity2, k -> new HashMap<>()).put(idCity1, distance);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return distances;
    }

    /**
     * Retrieves the list of cities of all shops from which one or more items are ordered.
     *
     * @param idOrder the ID of the order
     * @return the list of city IDs representing the cities from which items are ordered
     */
    private List<Integer> getOrderCities(int idOrder) {
        String query = """
                SELECT DISTINCT S.IdCity AS 'Id'
                FROM OrderItem OI
                    JOIN Article P ON (OI.IdArticle = P.Id)
                    JOIN Shop S ON (P.IdShop = S.Id)
                WHERE OI.IdOrder = ?""";
        List<Integer> orderCities = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(query)) {
            ps.setInt(1, idOrder);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orderCities.add(rs.getInt("Id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return orderCities;
    }

    /**
     * Constructs the path from the nearest city to the buyer city using the previous city information.
     *
     * @param idBuyerCity         the ID of the buyer city
     * @param fromBuyerToEachCity map of distance and previous city information from the buyer to each city
     * @return the list of city IDs representing the shortest path from the nearest city to the buyer city
     */
    private List<Integer> constructPath(int idBuyerCity, HashMap<Integer, DistancePreviousContainer> fromBuyerToEachCity) {
        List<Integer> pathFromNearestToBuyerCity = new ArrayList<>();
        int idCurrentCity = idNearestCity;

        while (idCurrentCity != idBuyerCity) {
            pathFromNearestToBuyerCity.add(idCurrentCity);
            idCurrentCity = fromBuyerToEachCity.get(idCurrentCity).getPrevious(); //get previous in the shortest path
        }
        pathFromNearestToBuyerCity.add(idBuyerCity);

        return pathFromNearestToBuyerCity;
    }

    public int getIdNearestCity() {
        return idNearestCity;
    }

    public int getMaxDistanceToNearestCity() {
        return maxDistanceToNearestCity;
    }

    public int getMinDistanceFromBuyerToNearestCity() {
        return minDistanceFromBuyerToNearestCity;
    }
}
