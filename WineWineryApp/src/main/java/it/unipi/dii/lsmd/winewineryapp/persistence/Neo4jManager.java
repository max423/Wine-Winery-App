package it.unipi.dii.lsmd.winewineryapp.persistence;

import it.unipi.dii.lsmd.winewineryapp.model.User;
import it.unipi.dii.lsmd.winewineryapp.model.Wine;
import it.unipi.dii.lsmd.winewineryapp.model.Winery;
import javafx.util.Pair;
import org.neo4j.driver.Driver;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

public class Neo4jManager {

    Driver driver;

    public Neo4jManager(Driver driver) {
        this.driver = driver;
    }

    /**
     * Function that add the info of a new user to GraphDB
     * @param u new User
     */
    public boolean addUser(User u) {
        boolean res = false;
        try(Session session = driver.session()) {
            res = session.writeTransaction((TransactionWork<Boolean>) tx -> {
                tx.run("CREATE (u:User {username: $username, email: $email})",
                        parameters("username", u.getUsername(), "email", u.getEmail()));

                return true;
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return res;
    }


    public int getNumFollowersUser(final String username) {
        int numFollowers;
        try (Session session = driver.session()) {
            numFollowers = session.writeTransaction((TransactionWork<Integer>) tx -> {
                Result result = tx.run("MATCH (:User {username: $username})<-[r:FOLLOWS]-() " +
                        "RETURN COUNT(r) AS numFollowers", parameters("username", username));
                return result.next().get("numFollowers").asInt();
            });
        }
        return numFollowers;
    }
    public int getNumFollowingUser(final String username) {
        int numFollowers;
        try (Session session = driver.session()) {
            numFollowers = session.writeTransaction((TransactionWork<Integer>) tx -> {
                Result result = tx.run("MATCH (:User {username: $username})-[r:FOLLOWS]->() " +
                        "RETURN COUNT(r) AS numFollowers", parameters("username", username));
                return result.next().get("numFollowers").asInt();
            });
        }
        return numFollowers;
    }
    public boolean userAFollowsUserB (String userA, String userB) {
        boolean res = false;
        try(Session session = driver.session()) {
            res = session.readTransaction((TransactionWork<Boolean>) tx -> {
                Result r = tx.run("MATCH (a:User{username:$userA})-[r:FOLLOWS]->(b:User{username:$userB}) " +
                        "RETURN COUNT(*)", parameters("userA", userA, "userB", userB));
                Record record = r.next();
                if (record.get(0).asInt() == 0)
                    return false;
                else
                    return true;
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public boolean createWinery (final String title, final String owner) {
        try (Session session = driver.session()){
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("CREATE (:Winery {title: $title, owner: $owner})"
                        , parameters("title", title, "owner", owner));
                return null;
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public boolean updateUser(User u) {
        try(Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User {username: $username}) SET u.email = $newEmail",
                        parameters("username", u.getUsername(), "newEmail", u.getEmail()));
                return null;
            });
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void followUser (final String username, final String target) {
        try (Session session = driver.session()){
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User {username: $username}), (t:User {username: $target}) " +
                                "MERGE (u)-[p:FOLLOWS]->(t) " +
                                "ON CREATE SET p.date = datetime()",
                        parameters("username", username, "target", target));
                return null;
            });
        }
    }

    public void unfollowUser (final String username, final String target) {
        try (Session session = driver.session()){
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (:User {username: $username})-[r:FOLLOWS]->(:User {username: $target}) " +
                                "DELETE r",
                        parameters("username", username, "target", target));
                return null;
            });
        }
    }

    public int getNumFollowersWinery(final String title, final String owner) {
        int numFollowers;
        try (Session session = driver.session()) {
            numFollowers = session.writeTransaction((TransactionWork<Integer>) tx -> {
                Result result = tx.run("MATCH (:Winery {title: $title, owner: $owner})<-[r:FOLLOWS]-() " +
                        "RETURN COUNT(r) AS numFollowers", parameters("title", title, "owner", owner));
                return result.next().get("numFollowers").asInt();
            });
        }
        return numFollowers;
    }

    public boolean isUserFollowingWinery (String user, String owner, Winery winery) {
        boolean res = false;
        try(Session session = driver.session()) {
            res = session.readTransaction((TransactionWork<Boolean>) tx -> {
                Result r = tx.run("MATCH (a:User{username:$user})-[r:FOLLOWS]->(b:Winery{title:$title, owner:$owner }) " +
                        "RETURN COUNT(*)", parameters("user", user, "title", winery.getTitle(), "owner", owner));
                Record record = r.next();
                if (record.get(0).asInt() == 0)
                    return false;
                else
                    return true;
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }




    public void followWinery (final String title, final String owner, final String username) {
        try (Session session = driver.session()){
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User {username: $username}), (w:Winery {title: $title, owner: $owner}) " +
                                "MERGE (u)-[p:FOLLOWS]->(w) " +
                                "ON CREATE SET p.date = datetime()",
                        parameters("username", username, "title", title, "owner", owner));
                return null;
            });
        }
    }

    public void unfollowWinery (final String title, final String owner, final String username) {
        try (Session session = driver.session()){
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (:User {username: $username})-[r:FOLLOWS]->(:Winery {title: $title, owner: $owner}) " +
                                "DELETE r",
                        parameters("username", username, "title", title, "owner", owner));
                return null;
            });
        }
    }

    public boolean deleteWinery (final String title, final String owner) {
        boolean res = false;
        try (Session session = driver.session()){
            res = session.writeTransaction((TransactionWork<Boolean>) tx -> {
                tx.run("MATCH (w:Winery {title: $title, owner: $owner}) " +
                                "DETACH DELETE w",
                        parameters("title", title, "owner", owner));
                return true;
            });
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return res;
    }

    public boolean userLikeWine (String user, Wine wine){
        boolean res = false;
        try(Session session = driver.session()){
            res = session.readTransaction((TransactionWork<Boolean>) tx -> {
                Result r = tx.run("MATCH (:User{username:$user})-[r:LIKES]->(w:Wine) WHERE (w.vivino_id = $vivino_id AND w.glugulp_id =$glugulp_id) " +
                        "RETURN COUNT(*)", parameters("user", user, "vivino_id", wine.getVivino_id(), "glugulp_id", wine.getGlugulp_id()));
                Record record = r.next();
                if (record.get(0).asInt() == 0)
                    return false;
                else
                    return true;
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return res;
    }

    public int getNumLikes(final Wine wine) {
        int numLikes;
        try (Session session = driver.session()) {
            numLikes = session.writeTransaction((TransactionWork<Integer>) tx -> {
                Result result = tx.run("MATCH (w:Wine)<-[r:LIKES]-() WHERE w.vivino_id = $vivino_id OR w.glugulp_id = $glugulp_id " +
                        "RETURN COUNT(r) AS numLikes", parameters("vivino_id", wine.getVivino_id(), "glugulp_id", wine.getGlugulp_id()));
                return result.next().get("numLikes").asInt();
            });
        }
        return numLikes;
    }

    public void like(User u, Wine w) {
        try(Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (a:User), (b:Wine) " +
                                "WHERE a.username = $username AND (b.vivino_id = $vivino_id OR b.glugulp_id = $glugulp_id) " +
                                "MERGE (a)-[r:LIKES]->(b)",
                        parameters("username", u.getUsername(),
                                "vivino_id", w.getVivino_id(),
                                "glugulp_id", w.getGlugulp_id()));
                return null;
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean unlike(User u, Wine w) {
        try (Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User{username:$username})-[r:LIKES]->(w:Wine) " +
                                "WHERE w.vivino_id = $vivino_id OR w.glugulp_id = $glugulp_id" +
                                " DELETE r",
                        parameters("username", u.getUsername(),
                                "vivino_id", w.getVivino_id(),
                                "glugulp_id", w.getGlugulp_id()));
                return null;
            });
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Function that returns a list of suggested users snapshots for the logged user.
     * Suggestions are based on most followed users who are 2 FOLLOWS hops far from the
     * logged user (first level);
     * The second level of suggestion returns most followed users that have likes in common with
     * the logged user.
     *
     * @param u user who need suggestions
     * @param numberFirstLv how many users suggest from first level suggestion
     * @param numberSecondLv how many users suggest from second level
     * @return A list of suggested users snapshots
     */
    public List<User> getSnapsOfSuggestedUsers(User u, int numberFirstLv, int numberSecondLv, int skipFirstLv, int skipSecondLv) {
        List<User> usersSnap = new ArrayList<>();

        try (Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (me:User {username: $username})-[:FOLLOWS*2..2]->(target:User), " +
                                "(target)<-[r:FOLLOWS]-() " +
                                "WHERE NOT EXISTS((me)-[:FOLLOWS]->(target)) " +
                                "RETURN DISTINCT target.username AS Username, target.email AS Email, " +
                                "COUNT(DISTINCT r) as numFollower " +
                                "ORDER BY numFollower DESC, Username " +
                                "SKIP $skipFirstLevel " +
                                "LIMIT $firstLevel " +
                                "UNION " +
                                "MATCH (me:User {username: $username})-[:LIKES]->()<-[:LIKES]-(target:User), " +
                                "(target)<-[r:FOLLOWS]-() " +
                                "WHERE NOT EXISTS((me)-[:FOLLOWS]->(target)) " +
                                "RETURN target.username AS Username, target.email AS Email, " +
                                "COUNT(DISTINCT r) as numFollower " +
                                "ORDER BY numFollower DESC, Username " +
                                "SKIP $skipSecondLevel " +
                                "LIMIT $secondLevel",
                        parameters("username", u.getUsername(), "firstLevel", numberFirstLv, "secondLevel", numberSecondLv,  "skipFirstLevel", skipFirstLv, "skipSecondLevel", skipSecondLv));
                while (result.hasNext()) {
                    Record r = result.next();
                    User snap = new User(r.get("Username").asString(), r.get("Email").asString(),
                            "","","",-1,"", new ArrayList<>(), 0);

                    usersSnap.add(snap);
                }
                return null;
            });
        }
        return usersSnap;
    }

    /**
     * Function that returns a list of suggested wineries snapshots for the logged user.
     * Suggestions are based on most followed wineries followed by followed users (first level)
     * and most followed wineries followed by users that are 2 FOLLOWS hops far from the
     * logged user (second level).
     *
     * @param u Logged User
     * @param numberFirstLv how many wineries suggest from first level
     * @param numberSecondLv how many wineries suggest from second level
     * @return A list of suggested wineries snapshots
     */
    public List<Pair<String, Winery>> getSnapsOfSuggestedWinerys(User u, int numberFirstLv, int numberSecondLv, int skipFirstLv, int skipSecondLv){
        List<Pair<String, Winery>> winerysSnap = new ArrayList<>();
        try(Session session = driver.session()){
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (target:Winery)<-[f:FOLLOWS]-(u:User)<-[:FOLLOWS]-(me:User{username:$username}), " +
                                "(target)<-[r:FOLLOWS]-(n:User) WITH DISTINCT me, target, " +
                                "COUNT(DISTINCT r) AS numFollower, COUNT(DISTINCT u) AS follow " +
                                "WHERE NOT EXISTS((me)-[:FOLLOWS]->(target)) " +
                                "RETURN target.owner AS Owner, target.title AS Title, numFollower + follow AS followers " +
                                "ORDER BY followers DESC, Title " +
                                "SKIP $skipFirstLevel " +
                                "LIMIT $firstLevel " +
                                "UNION " +
                                "MATCH (target:Winery)<-[f:FOLLOWS]-(u:User)<-[:FOLLOWS*2..2]-(me:User{username:$username}), " +
                                "(target)<-[r:FOLLOWS]-(n:User) WITH DISTINCT me, target, " +
                                "COUNT(DISTINCT r) AS numFollower, COUNT(DISTINCT u) AS follow " +
                                "WHERE NOT EXISTS((me)-[:FOLLOWS]->(target))" +
                                "RETURN target.owner AS Owner, target.title AS Title, numFollower + follow AS followers " +
                                "ORDER BY followers DESC, Title " +
                                "SKIP $skipSecondLevel " +
                                "LIMIT $secondLevel",
                        parameters("username", u.getUsername(), "firstLevel", numberFirstLv, "secondLevel", numberSecondLv, "skipFirstLevel", skipFirstLv, "skipSecondLevel", skipSecondLv));

                while(result.hasNext()){
                    Record r = result.next();
                    Winery snap = new Winery(r.get("Title").asString(), null);
                    winerysSnap.add(new Pair<>(r.get("Owner").asString(), snap));
                }

                return null;
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        return winerysSnap;
    }


    /**
     * Function that returns a list of suggested wines snapshots for the logged user.
     * Suggestions are based on wines liked by followed users (first level) and likes liked by users
     * that are 2 FOLLOWS hops far from the logged user (second level).
     * Wines returned are ordered by the number of times they appeared in the results, so wines
     * that appear more are most likely to be similar to the interests of the logged user.
     *
     * @param u Logged User
     * @param numberFirstLv how many wines suggest from first level
     * @param numberSecondLv how many wines suggest from second level
     * @return A list of suggested wines snapshots
     */
    public List<Wine> getSnapsOfSuggestedWines(User u, int numberFirstLv, int numberSecondLv, int skipFirstLv, int skipSecondLv) {
        List<Wine> winesSnap = new ArrayList<>();
        try(Session session = driver.session()){
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (target:Wine)<-[r:LIKES]-(u:User)<-[:FOLLOWS]-(me:User{username:$username}) " +
                                "WHERE NOT EXISTS((me)-[:LIKES]->(target)) " +
                                "RETURN target.vivino_id AS VivinoId, target.glugulp_id AS GlugulpId, target.name as Name, " +
                                "target.winemaker AS Winemaker, target.country AS Country, target.varietal AS Varietal, " +
                                "target.grapes AS Grapes, target.year AS Year, target.price AS Price, COUNT(*) AS nOccurences " +
                                "ORDER BY nOccurences DESC, Name " +
                                "SKIP $skipFirstLevel " +
                                "LIMIT $firstlevel " +
                                "UNION " +
                                "MATCH (target:Wine)<-[r:LIKES]-(u:User)<-[:FOLLOWS*2..2]-(me:User{username:$username}) " +
                                "WHERE NOT EXISTS((me)-[:LIKES]->(target)) " +
                                "RETURN target.vivino_id AS VivinoId, target.glugulp_id AS GlugulpId, target.name as Name, " +
                                "target.winemaker AS Winemaker, target.country AS Country, target.varietal AS Varietal, " +
                                "target.grapes AS Grapes, target.year AS Year, target.price AS Price, COUNT(*) AS nOccurences " +
                                "ORDER BY nOccurences DESC, Name " +
                                "SKIP $skipSecondLevel " +
                                "LIMIT $secondLevel",
                        parameters("username", u.getUsername(), "firstlevel", numberFirstLv, "secondLevel", numberSecondLv, "skipFirstLevel", skipFirstLv, "skipSecondLevel", skipSecondLv));
                while(result.hasNext()){
                    Record r = result.next();
                    List<String> authors = new ArrayList<>();

                    String vivino_id = null;
                    String glugulp_id = null;
                    if(r.get("VivinoId").isNull())
                        glugulp_id = r.get("GlugulpId").asString();
                    else
                        vivino_id = r.get("VivinoId").asString();

                    Wine snap = new Wine( vivino_id,
                            glugulp_id,
                            r.get("Name").asString(),
                            r.get("Winemaker").asString(),
                            r.get("Country").asString(),
                            r.get("Varietal").asString(),
                            r.get("Grapes").asString(),
                            r.get("Year").asInt(),
                            r.get("Price").asDouble(),
                            "",
                            "",
                            new ArrayList<>());

                    winesSnap.add(snap);
                }

                return null;
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        return winesSnap;
    }



    /**
     * Return a hashmap with the most popular user
     * @param num num of rank
     * @return pair (name, numFollower)
     */
    public List<Pair<User, Integer>> getMostFollowedUsers (int skip, int num) {
        List<Pair<User, Integer>> rank;
        try (Session session = driver.session()) {
            rank = session.readTransaction(tx -> {
                Result result = tx.run("MATCH (target:User)<-[r:FOLLOWS]-(:User) " +
                                "RETURN DISTINCT target.username AS Username, target.email AS Email, " +
                                "COUNT(DISTINCT r) as numFollower " +
                                "ORDER BY numFollower DESC, Username " +
                                "SKIP $skip " +
                                "LIMIT $num",
                        parameters("skip", skip, "num", num));
                List<Pair<User, Integer>> popularUser = new ArrayList<>();
                while (result.hasNext()) {
                    Record r = result.next();
                    User snap = new User(r.get("Username").asString(), r.get("Email").asString(),
                            "","","",-1,"", new ArrayList<>(), 0);

                    popularUser.add(new Pair(snap, r.get("numFollower").asInt()));
                }
                return popularUser;
            });
        }
        return rank;
    }

    /**
     * Return a hashmap with the most popular Winery
     * @param num num of rank
     * @return pair (name, numFollower)
     */
    public List<Pair<Pair<String, Winery>, Integer>> getMostFollowedWinerys (int skip, final int num) {
        List<Pair<Pair<String, Winery>, Integer>> rank;
        try (Session session = driver.session()) {
            rank = session.readTransaction(tx -> {
                Result result = tx.run("MATCH (target:Winery)<-[r:FOLLOWS]-(:User) " +
                                "RETURN DISTINCT target.title AS Title, target.owner AS Owner, " +
                                "COUNT(DISTINCT r) as numFollower " +
                                "ORDER BY numFollower DESC, Owner " +
                                "SKIP $skip " +
                                "LIMIT $num",
                        parameters("skip", skip, "num", num));
                List<Pair<Pair<String, Winery>, Integer>> popularWinerys = new ArrayList<>();
                while (result.hasNext()) {
                    Record r = result.next();
                    Winery snap = new Winery(r.get("Title").asString(), null);

                    popularWinerys.add(new Pair(new Pair(r.get("Owner").asString(), snap)
                            , r.get("numFollower").asInt()));
                }
                return popularWinerys;
            });
        }
        return rank;
    }


    /**
     * Method that returns wines with the highest number of likes
     * @param limit
     * @return List of Wines
     */
    public List<Pair<Wine, Integer>> getMostLikedWines(int skip, int limit) {
        List<Pair<Wine, Integer>> topWines = new ArrayList<>();

        try(Session session = driver.session()) {
            session.readTransaction(tx -> {
                Result result = tx.run("MATCH (:User)-[l:LIKES]->(w:Wine) " +
                                "RETURN w.vivino_id AS VivinoId, w.glugulp_id AS GlugulpId, w.name AS Name, " +
                                "w.winemaker AS Winemaker, w.country AS Country, " +
                                "w.varietal AS Varietal, w.grapes AS Grapes, w.year AS Year, w.price AS Price, " +
                                "COUNT(l) AS like_count " +
                                "ORDER BY like_count DESC, Name " +
                                "SKIP $skip " +
                                "LIMIT $limit",
                        parameters( "skip", skip, "limit", limit));

                while(result.hasNext()){
                    Record r = result.next();
                    List<String> authors = new ArrayList<>();

                    String vivino_id = null;
                    String glugulp_id = null;
                    if(r.get("VivinoId").isNull())
                        glugulp_id = r.get("GlugulpId").asString();
                    else
                        vivino_id = r.get("VivinoId").asString();

                    Wine snap = new Wine( vivino_id,
                            glugulp_id,
                            r.get("Name").asString(),
                            r.get("Winemaker").asString(),
                            r.get("Country").asString(),
                            r.get("Varietal").asString(),
                            r.get("Grapes").asString(),
                            r.get("Year").asInt(),
                            r.get("Price").asDouble(),
                            "",
                            "",
                            new ArrayList<>());

                    topWines.add(new Pair(snap, r.get("like_count").asInt()));
                }
                return null;
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return topWines;
    }

}
