package it.unipi.dii.lsmd.winewineryapp.persistence;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import it.unipi.dii.lsmd.winewineryapp.model.*;
import javafx.util.Pair;
import org.bson.Document;
import com.google.gson.Gson;
import org.bson.conversions.Bson;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.inc;

public class MongoDBManager {
    public MongoDatabase db;
    private MongoCollection usersCollection;
    private MongoCollection winesCollection;

    public MongoDBManager(MongoClient client) {
        this.db = client.getDatabase("winewinery");
        usersCollection = db.getCollection("Users");
        winesCollection = db.getCollection("Wines");
    }

    /**
     * Method used to perform the login
     * @param username User that is logging in
     * @param password
     * @return User informations related to the username
     */
    public User login (String username, String password) {
        Document result = (Document) usersCollection.find(Filters.and(eq("username", username),
                        eq("password", password))).
                first();

        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(result), User.class);
    }

    public boolean addUser (User u) {
        try {
            Document doc = new Document("username", u.getUsername())
                    .append("email", u.getEmail())
                    .append("password", u.getPassword());

            if (u.getFirstName() != null)
                doc.append("firstName", u.getFirstName());
            if (u.getLastName() != null)
                doc.append("lastName", u.getLastName());
            if (u.getAge() != -1)
                doc.append("age", u.getAge());
            if (u.getLocation() != null)
                doc.append("location", u.getLocation());

            doc.append("winerys", u.getWinerys());

            usersCollection.insertOne(doc);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public User getUserByUsername (String username) {
        Document result = (Document) usersCollection.find((eq("username", username))).first();
        if (result == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(result), User.class);
    }

    public boolean deleteUser(User u) {
        try {
            Bson find = eq("comments.username", u.getUsername());
            Bson update = Updates.set("comments.$.username", "Deleted user");
            winesCollection.updateMany(find, update);
            usersCollection.deleteOne(eq("username", u.getUsername()));
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
    public boolean createWinery(User user, String title) {
        // check if there are other wineries with the same name
        Document document = (Document) usersCollection.find(and(eq("username", user.getUsername()),
                eq("winerys.title", title))).first();
        if (document != null) {
            System.err.println("ERROR: name already in use.");
            return false;
        }
        // create the new winery
        Document winery = new Document("title", title)
                .append("wines", Arrays.asList());
        // insert the new winery
        usersCollection.updateOne(
                eq("username", user.getUsername()),
                new Document().append(
                        "$push",
                        new Document("winerys", winery)
                )
        );
        return true;
    }
    public boolean deleteWinery(String username, String title){
        try {
            Bson filter = new Document().append("username", username);
            Bson fields = new Document().append("winerys", new Document("title", title));
            Bson update = new Document("$pull", fields);
            UpdateResult updateResult = usersCollection.updateOne(filter, update);
            if (updateResult.getModifiedCount() == 0) {
                System.err.println("ERROR: can not delete the winery " + title);
                return false;
            } else {
                return true;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean updateUser (User u){
        try {
            Document doc = new Document().append("username", u.getUsername());
            if (!u.getEmail().isEmpty())
                doc.append("email", u.getEmail());
            if (!u.getPassword().isEmpty())
                doc.append("password", u.getPassword());
            if (!u.getFirstName().isEmpty())
                doc.append("firstName", u.getFirstName());
            if (!u.getLastName().isEmpty())
                doc.append("lastName", u.getLastName());
            if (u.getAge() != -1)
                doc.append("age", u.getAge());
            if (u.getLocation().isEmpty())
                doc.append("location", u.getLocation());
            doc.append("type", u.getType());

            Bson updateOperation = new Document("$set", doc);
            usersCollection.updateOne(new Document("username", u.getUsername()), updateOperation);
            return true;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public UpdateResult removeWineFromWinery(String user, String title, Wine w) {
        try {
            Document wineReduced = new Document("vivino_id", w.getVivino_id())
                    .append("glugulp_id", w.getGlugulp_id())
                    .append("name", w.getName())
                    .append("winemaker", w.getWinemaker())
                    .append("country", w.getCountry())
                    .append("varietal", w.getVarietal())
                    .append("grapes", w.getGrapes())
                    .append("year", w.getYear())
                    .append("price", w.getPrice());

            Bson find = and(eq("username", user),
                    eq("winerys.title", title));
            Bson delete = Updates.pull("winerys.$.wines", wineReduced);
            UpdateResult result = usersCollection.updateOne(find, delete);
            return result;
        }
        catch (Exception e)
        {
            System.out.println("Error in removing a wine from a Winery");
            e.printStackTrace();
            return null;
        }
    }

    public Wine getWineById (Wine wine) {
        try {
            Wine w = null;
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();
            Document myDoc = (Document) winesCollection.find(
                    and(eq("vivino_id", wine.getVivino_id()), eq("glugulp_id", wine.getGlugulp_id()))).first();
            w = gson.fromJson(gson.toJson(myDoc), Wine.class);
            return w;
        }
        catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public UpdateResult addWineToWinery(String user, String title, Wine w) {
        Document wineReduced;
        if (w.getVivino_id() != null) {
            wineReduced = new Document("vivino_id", w.getVivino_id());
        }
        else {
            wineReduced = new Document("glugulp_id", w.getGlugulp_id());
        }

        wineReduced.append("name", w.getName())
                .append("winemaker", w.getWinemaker())
                .append("country", w.getCountry())
                .append("varietal", w.getVarietal())
                .append("grapes", w.getGrapes())
                .append("year", w.getYear())
                .append("price", w.getPrice());

        Bson find = and(eq("username", user),
                eq("winerys.title", title));
        Bson update = Updates.addToSet("winerys.$.wines", wineReduced);
        UpdateResult result = usersCollection.updateOne(find, update);
        return result;
    }
    public boolean addComment (Wine wine, Comment comment) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Document doc = new Document("username", comment.getUsername())
                    .append("text", comment.getText())
                    .append("timestamp", dateFormat.format(comment.getTimestamp()));

            Bson find = and(eq("vivino_id", wine.getVivino_id()), eq("glugulp_id", wine.getGlugulp_id()));
            Bson update = Updates.addToSet("comments", doc);
            winesCollection.updateOne(find, update);
            return true;
        }
        catch (Exception e)
        {
            System.out.println("Error in adding a comment to a Wine");
            e.printStackTrace();
            return false;
        }
    }
    public void deleteComment (Wine wine, Comment comment) {
        List<Comment> comments = wine.getComments();
        int n = 0;
        int d = 0;
        for (Comment c : comments){
            if (c.getTimestamp().equals(comment.getTimestamp()) && c.getUsername().equals(comment.getUsername())) {
                d = n;
                break;
            }
            n++;
        }
        comments.remove(d);
        if (Session.getInstance().getLoggedUser().getType() > 0)
            incrementDeletedCommentsCounter(comment.getUsername());
        updateComments(wine, comments);
    }
    public boolean updateComments(Wine w, List<Comment> comments){
        try{
            Bson update = new Document("comments", comments);
            Bson updateOperation = new Document("$set", update);
            if(w.getVivino_id() != null)
                winesCollection.updateOne(new Document("vivino_id", w.getVivino_id()), updateOperation);
            else
                winesCollection.updateOne(new Document("glugulp_id", w.getGlugulp_id()), updateOperation);
            return true;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            System.err.println("Error in updating user on MongoDB");
            return false;
        }
    }

    public void incrementDeletedCommentsCounter(String username) {
        usersCollection.updateOne(eq("username", username), inc("deletedComments", 1));
    }

    public void updateComment(Wine wine, Comment comment){
        List<Comment> comments = wine.getComments();
        int i=0;
        for (Comment c: comments
        ) {
            if(c.getUsername().equals(comment.getUsername()) && c.getTimestamp().equals(
                    comment.getTimestamp())){
                comments.set(i, comment);
                break;
            }
            i++;
        }
        updateComments(wine, comments);
    }

    public Winery getWinery(String owner, String title) {
        Winery winery = null;
        Gson gson = new GsonBuilder().serializeNulls().create();
        Bson match = match(eq("username", owner));
        Bson unwind = unwind("$winerys");
        Bson match2 = match(eq("winerys.title", title));
        Bson project = project(fields(excludeId(), computed("Winery", "$winerys")));
        MongoCursor<Document> iterator = (MongoCursor<Document>) usersCollection.aggregate(Arrays.asList(match, unwind,
                match2, project)).iterator();
        if(iterator.hasNext()){
            Document document = iterator.next();
            Document WineryDocument = (Document) document.get("Winery");
            winery = gson.fromJson(gson.toJson(WineryDocument), Winery.class);
        }
        return winery;
    }


    /**
     * Method that searches wines given parameters.
     * @param name partial name of the wines to match
     * @param winemaker partial name of the winemakers to match
     * @param country
     * @param varietal
     * @param grapes
     * @param min_year
     * @param max_year
     * @param min_price
     * @param max_price
     * @return a list of wines that match the parameters
     */

    public List<Wine> searchWinesByParameters (String name, String winemaker, String country,
                                               String varietal, String grapes, int min_year, int max_year,
                                               double min_price, double max_price, int skip, int limit) {
        List<Wine> wines = new ArrayList<>();
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd hh:mm:ss").create();

        List<Bson> pipeline = new ArrayList<>();

        if (!name.isEmpty()) {
            Pattern pattern1 = Pattern.compile("^.*" + name + ".*$", Pattern.CASE_INSENSITIVE);
            pipeline.add(match(regex("name", pattern1)));
        }

        if (!winemaker.isEmpty()) {
            Pattern pattern2 = Pattern.compile("^.*" + winemaker + ".*$", Pattern.CASE_INSENSITIVE);
            pipeline.add(match(regex("winemaker", pattern2)));
        }

        if (!country.isEmpty()) {
            pipeline.add(match(eq("country", country)));
        }

        if (!varietal.isEmpty()) {
            pipeline.add(match(eq("varietal", varietal)));
        }

        if (!grapes.isEmpty()) {
            pipeline.add(match(eq("grapes", grapes)));
        }

        if (min_year != 0) {
            pipeline.add(match(and(gte("year", min_year))));
        }

        if(max_year != 0) {
            pipeline.add(match(and(lte("year", max_year))));
        }

        if (min_price != 0)
            pipeline.add(match(and(gte("price", min_price))));

        if(max_price != 0) {
            pipeline.add(match(and(lte("price", max_price))));
        }

        pipeline.add(sort(ascending("price")));
        pipeline.add(skip(skip * limit));
        pipeline.add(limit(limit));

        List<Document> results = (List<Document>) winesCollection.aggregate(pipeline).into(new ArrayList<>());
        Type winesListType = new TypeToken<ArrayList<Wine>>(){}.getType();
        wines = gson.fromJson(gson.toJson(results), winesListType);
        return wines;
    }

    /**
     * Return users that contains the keyword, if we give a list of user
     * the research is added only inside this sublist
     * @param next select the portion of result
     * @param keyword keyword to search users
     * @return list of users
     */
    public List<User> getUsersByKeyword (String keyword, boolean moderator, int next) {
        List<User> results = new ArrayList<>();
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        Consumer<Document> convertInUser = doc -> {
            User user = gson.fromJson(gson.toJson(doc), User.class);
            results.add(user);
        };
        Pattern pattern= Pattern.compile("^.*" + keyword + ".*$", Pattern.CASE_INSENSITIVE);
        Bson filter = Aggregates.match(Filters.regex("username", pattern));
        Bson limit = limit(8);
        Bson skip = skip(next*8);
        if (moderator) {
            Bson moderatorFilter = match(eq("type", 1));
            usersCollection.aggregate(Arrays.asList(filter, moderatorFilter, skip, limit)).forEach(convertInUser);
        } else
            usersCollection.aggregate(Arrays.asList(filter, skip, limit)).forEach(convertInUser);
        return results;
    }


    /**
     * Function that return the Wineries given the title
     * @param keyword part of the title
     * @return  The list of wineries and its owner
     */
    public List<Pair<String, Winery>> getWineryByKeyword (String keyword, int skipDoc, int limitDoc) {
        List<Pair<String, Winery>> winerys = new ArrayList<>();
        Gson gson = new GsonBuilder().serializeNulls().create();
        Bson unwind = unwind("$winerys");
        Pattern pattern= Pattern.compile("^.*" + keyword + ".*$", Pattern.CASE_INSENSITIVE);
        Bson filter = Aggregates.match(Filters.regex("winerys.title", pattern));
        Bson skip = skip(skipDoc);
        Bson limit = limit(limitDoc);
        MongoCursor<Document> iterator = (MongoCursor<Document>) usersCollection.aggregate(Arrays.asList(unwind,
                filter, skip, limit)).iterator();
        while(iterator.hasNext()){
            Document document = iterator.next();
            String username = document.getString("username");
            Document WineryDocument = (Document) document.get("winerys");
            Winery winery = gson.fromJson(gson.toJson(WineryDocument), Winery.class);
            winerys.add(new Pair<>(username, winery));
        }
        return winerys;
    }



    /*Users with the highest number of wines in their wineries  */
    /**
     * Function that return a list of User with the highest number of wines in their wineries
     * @param limitDoc First "number" users
     * @param skipDoc Skip users
     * @return  The list of users
     */
    public List<Pair<User, Integer>> getTopVersatileUsers (int skipDoc, int limitDoc) {
        List<Pair<User, Integer>> results = new ArrayList<>();
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        Consumer<Document> convertInUser = doc -> {
            User user = gson.fromJson(gson.toJson(doc), User.class);
            results.add(new Pair(user, doc.getInteger("totalCategory")));
        };

        Bson unwind1 = unwind("$winerys");
        Bson unwind2 = unwind("$winerys.wines");
        // Distinct occurrences
        Bson groupMultiple = new Document("$group",
                new Document("_id", new Document("username", "$username")
                        .append("email", "$email")
                        .append("password", "$password")
                        .append("firstName", "$firstName")
                        .append("lastName", "$lastName")
                        .append("age", "$age")
                        .append("location", "$location")
                        .append("name", "$winerys.wines.name")
                ));
        // Sum all occurrences
        Bson group = new Document("$group",
                new Document("_id",
                        new Document("username", "$_id.username")
                                .append("email", "$_id.email")
                                .append("password", "$_id.password")
                                .append("firstName", "$_id.firstName")
                                .append("lastName", "$_id.lastName")
                                .append("age", "$_id.age")
                                .append("location", "$_id.location"))
                        .append("totalWine",
                                new Document("$sum", 1)));

        Bson project = project(fields(excludeId(),
                computed("username", "$_id.username"),
                computed("email", "$_id.email"),
                computed("password", "$_id.password"),
                computed("firstName", "$_id.firstName"),
                computed("lastName", "$_id.lastName"),
                computed("age", "$_id.age"),
                computed("location", "$_id.location"),
                include("totalWine")));
        Bson sort = sort(descending("totalWine"));
        Bson skip = skip(skipDoc);
        Bson limit = limit(limitDoc);

        usersCollection.aggregate(Arrays.asList(unwind1, unwind2, groupMultiple, group, project, sort, skip, limit)).forEach(convertInUser);

        return results;
    }


    /**
     * Method that returns wines with the highest number of comments in the specified period of time.
     * @param period (all, month, week)
     * @param skipDoc (positive integer)
     * @param limitDoc (positive integer)
     * @return HashMap with the name and the number of comments
     */
    public List<Pair<Wine, Integer>> getMostCommentedWines(String period, int skipDoc, int limitDoc) {
        LocalDateTime localDateTime = LocalDateTime.now();
        LocalDateTime startOfDay = null;
        switch (period) {
            case "all" -> startOfDay = LocalDateTime.MIN;
            case "month" -> startOfDay = localDateTime.toLocalDate().atStartOfDay().minusMonths(1);
            case "week" -> startOfDay = localDateTime.toLocalDate().atStartOfDay().minusWeeks(1);
            default -> {
                System.err.println("ERROR: Wrong period.");
                return null;
            }
        }
        String filterDate = startOfDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        List<Pair<Wine, Integer>> results = new ArrayList<>();
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        Consumer<Document> convertInWine = doc -> {
            Wine wine = gson.fromJson(gson.toJson(doc), Wine.class);
            results.add(new Pair(wine, doc.getInteger("totalComments")));
        };

        Bson unwind = unwind("$comments");
        Bson filter = match(gte("comments.timestamp", filterDate));
        Bson group = new Document("$group",
                new Document("_id",
                        new Document("vivino_id", "$vivino_id")
                                .append("glugulp_id", "$glugulp_id")
                                .append("name", "$name")
                                .append("winemaker", "$winemaker")
                                .append("country", "$country")
                                .append("varietal", "$varietal")
                                .append("grapes", "$grapes")
                                .append("year", "$year")
                                .append("price", "$price"))
                        .append("totalComments",
                                new Document("$sum", 1)));
        Bson project = project(fields(excludeId(),
                computed("vivino_id", "$_id.vivino_id"),
                computed("glugulp_id", "$_id.glugulp_id"),
                computed("name", "$_id.name"),
                computed("winemaker", "$_id.winemaker"),
                computed("country", "$_id.country"),
                computed("varietal", "$_id.varietal"),
                computed("grapes", "$_id.grapes"),
                computed("year", "$_id.year"),
                computed("price", "$_id.price"),
                include("totalComments")));
        Bson sort = sort(Indexes.descending("totalComments", "published"));
        Bson skip = skip(skipDoc);
        Bson limit = limit(limitDoc);
        winesCollection.aggregate(Arrays.asList(unwind, filter, group, project, sort, skip, limit)).forEach(convertInWine);

        return results;
    }




}
