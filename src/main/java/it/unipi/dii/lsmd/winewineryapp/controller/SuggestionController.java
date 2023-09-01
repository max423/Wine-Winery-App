package it.unipi.dii.lsmd.winewineryapp.controller;

import it.unipi.dii.lsmd.winewineryapp.model.Session;
import it.unipi.dii.lsmd.winewineryapp.model.User;
import it.unipi.dii.lsmd.winewineryapp.model.Wine;
import it.unipi.dii.lsmd.winewineryapp.model.Winery;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDBManager;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jManager;
import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Pair;
import org.controlsfx.control.spreadsheet.Grid;
import javafx.scene.control.ToggleGroup;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;


public class SuggestionController {
    private ActionEvent event;
    private MongoDBManager mongoManager;
    private Neo4jManager neo4jManager;
    private User user;
    private int page;
    private int special;

    @FXML
    private Label username_side;
    @FXML
    private ComboBox<String> ResearchType;
    @FXML private ComboBox<String> PeriodBox;
    @FXML
    private Button searchBTN;
    @FXML
    private Button backBTN;
    @FXML
    private Button nextBTN;
    @FXML
    private GridPane cardsGrid;
    @FXML
    private Label errorTf;
    @FXML private CheckBox SugCHECK;
    @FXML private CheckBox AnaCHECK;


    public void initialize () {
        mongoManager = new MongoDBManager(MongoDriver.getInstance().openConnection());
        neo4jManager = new Neo4jManager(Neo4jDriver.getInstance().openConnection());
        user = Session.getInstance().getLoggedUser();
        special = 0;


        initSearch();

        nextBTN.setOnMouseClicked(mouseEvent -> goForward());
        backBTN.setOnMouseClicked(mouseEvent -> goBack());
    }

    // inizializza la ricerca
    private void initSearch() {
        page = 0;
        username_side.setText(Session.getInstance().getLoggedUser().getUsername());
        searchBTN.setDisable(true);
        SugCHECK.setSelected(false);
        AnaCHECK.setSelected(false);
        ResearchType.setVisible(false);
        PeriodBox.setVisible(false);

        List<String> timeRange = new ArrayList<>();
        timeRange.add("Week");
        timeRange.add("Month");
        timeRange.add("All");
        ObservableList<String> observableListTimeRange = FXCollections.observableList(timeRange);
        PeriodBox.getItems().clear();
        PeriodBox.setItems(observableListTimeRange);
    }
    @FXML
    private void handleCheckBoxAction(ActionEvent event) {
        CheckBox selectedCheckBox = (CheckBox) event.getSource();
        ResearchType.setVisible(true);

        if (selectedCheckBox == SugCHECK && SugCHECK.isSelected()) {
            // SUGGERIMENTI
            AnaCHECK.setSelected(false);
            loadSuggestions();
        } else if (selectedCheckBox == AnaCHECK && AnaCHECK.isSelected()) {
            // ANALYTICS
            SugCHECK.setSelected(false);
            loadAnalytics();
        }
    }

    private void loadSuggestions() {
        List<String> typeList = new ArrayList<>();
        typeList.add("Wines");
        typeList.add("Users");
        typeList.add("Winerys");

        ObservableList<String> observableListType = FXCollections.observableList(typeList);
        ResearchType.getItems().clear();
        ResearchType.setItems(observableListType);

    }
    private void loadAnalytics() {
        List<String> typeList = new ArrayList<>();
        typeList.add("Most commented wines");
        typeList.add("Most liked wines");
        typeList.add("Most followed users");
        typeList.add("Most followed winerys");
        typeList.add("Most versatile users");

        ObservableList<String> observableListType = FXCollections.observableList(typeList);
        ResearchType.getItems().clear();
        ResearchType.setItems(observableListType);
    }

    @FXML
    void switchSearch() {
        page = 0;
        cleanGrid();
        nextBTN.setDisable(true);
        backBTN.setDisable(true);
        searchBTN.setDisable(false);
        if(ResearchType.getValue()=="Most commented wines")
            PeriodBox.setVisible(true);
        else
            PeriodBox.setVisible(false);
    }

    @FXML
    void startResearch() {
        cleanGrid();
        nextBTN.setDisable(false);
        searchBTN.setDisable(true);
        errorTf.setText("");
        special = 1;

        if (SugCHECK.isSelected()){
            if (ResearchType.getValue() == null) {
                errorTf.setText("You have to select a valid option.");
                return;
            }
            switch (ResearchType.getValue()) {
                case "Wines" -> {
                    List<Wine> sugwines = neo4jManager.getSnapsOfSuggestedWines(user, 2,
                            1, 2 * page, 1 * page);
                    fillWines(sugwines);
                    System.out.println("ok" + sugwines);
                }
                case "Users" -> {
                    List<User> sugguser = neo4jManager.getSnapsOfSuggestedUsers(user, 4,
                            4, 4 * page, 4 * page);
                    fillUsers(sugguser);
                    System.out.println("ok" + sugguser);
                }
                case "Winerys" -> {
                    List<Pair<String, Winery>> suggwinerys = neo4jManager.getSnapsOfSuggestedWinerys(user, 2, 2,
                                    2 * page, 2 * page);
                    fillWinerys(suggwinerys);
                    System.out.println("ok" + suggwinerys);
                }
            }
        } else if (AnaCHECK.isSelected()) {
            if (ResearchType.getValue() == null) {
                errorTf.setText("You have to select a valid option.");
                return;
            }
            switch (ResearchType.getValue()) {
                case "Most versatile users" -> {
                    List<Pair<User, Integer>> users = mongoManager.getTopVersatileUsers(8*page, 8);
                    System.out.println("ok" + users);
                    fillUsers(users, "Categories saved");
                }
                case "Most followed users" -> {
                    List<Pair<User, Integer>> users = neo4jManager.getMostFollowedUsers(8*page, 8);
                    fillUsers(users, "Follower");
                }
                case "Most followed winerys" -> {
                    List<Pair<Pair<String, Winery>, Integer>> lists = neo4jManager.getMostFollowedWinerys(4*page, 4);
                    fillWinerys(lists, "Follower");
                }
                case "Most commented wines" -> {
                    String period = PeriodBox.getValue().toLowerCase(Locale.ROOT);
                    List<Pair<Wine, Integer>> wines = mongoManager.getMostCommentedWines(period, 3*page, 3);
                    fillWines(wines, "Comments");
                }
                case "Most liked wines" -> {
                    List<Pair<Wine, Integer>> wines = neo4jManager.getMostLikedWines(3*page, 3);
                    fillWines(wines, "Likes");
                }

            }
        }
    }

    private void fillUsers(List<Pair<User, Integer>> usersList, String label) {
        // set new layout
        setGridUsers();
        if (usersList.size() != 8)
            nextBTN.setDisable(true);
        int row = 0;
        int col = 0;
        for (Pair<User, Integer> u : usersList) {
            System.out.println(u.getKey() + " " + label + " " + u.getValue());
            Pane card = loadUsersCard(u.getKey(), label, u.getValue());
            cardsGrid.add(card, col, row);
            col++;
            if (col == 4) {
                col = 0;
                row++;
            }
        }
    }



    private void fillWinerys(List<Pair<String, Winery>> winerys) {
        setGridWine();
        if (winerys.size() == 0)
            nextBTN.setDisable(true);

        int row = 0;
        for (Pair<String, Winery> cardInfo : winerys) {
            Pane card = loadWineryElement(cardInfo.getValue(), cardInfo.getKey(), null, 0);
            cardsGrid.add(card, 0, row);
            row++;
        }
    }
    private void fillWinerys(List<Pair<Pair<String, Winery>, Integer>> winerys, String label) {
        setGridWine();
        if (winerys.size() != 4)
            nextBTN.setDisable(true);
        int row = 0;
        for (Pair<Pair<String, Winery>, Integer> cardInfo : winerys) {
            Pane card = loadWineryElement(cardInfo.getKey().getValue(), cardInfo.getKey().getKey(),
                    label, cardInfo.getValue());
            cardsGrid.add(card, 0, row);
            row++;
        }
    }
    private Pane loadWineryElement (Winery winerys, String owner, String analytics, int value) {
        Pane pane = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unipi/dii/lsmd/winewineryapp/layout/wineryElement.fxml"));
            pane = loader.load();
            WineryElementController ctrl = loader.getController();
            ctrl.setWineryElement(winerys, owner, analytics, value);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return pane;
    }


    private void cleanGrid() {
        cardsGrid.getColumnConstraints().clear();
        while (cardsGrid.getChildren().size() > 0) {
            cardsGrid.getChildren().remove(0);
        }
    }

    private void fillWines(List<Wine> WineList) {
        if (WineList.size() == 0)
            nextBTN.setDisable(true);
        setGridWine();
        int row = 0;
        for (Wine w : WineList) {
            Pane card = loadWineElelement(w, null, 0);
            cardsGrid.add(card, 0, row);
            row++;
        }
    }
    private void fillWines(List<Pair<Wine, Integer>> wineList, String label) {
        setGridWine();
        if (wineList.size() != 3)
            nextBTN.setDisable(true);
        int row = 0;
        for (Pair<Wine, Integer> p : wineList) {
            Pane card = loadWineElelement(p.getKey(), label, p.getValue());
            cardsGrid.add(card, 0, row);
            row++;
        }
    }

    private Pane loadWineElelement (Wine wine, String analytics, int value) {
        Pane pane = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unipi/dii/lsmd/winewineryapp/layout/wineElelement.fxml"));
            pane = loader.load();
            WineElementController ctrl = loader.getController();
            ctrl.setWineElement(wine, false, null, analytics, value);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return pane;
    }
    private void setGridWine() {
        cleanGrid();
        cardsGrid.setAlignment(Pos.CENTER);
        cardsGrid.setVgap(25);
        cardsGrid.setPadding(new Insets(15,40,15,100));
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(100);
        cardsGrid.getColumnConstraints().add(constraints);
    }


    private void fillUsers(List<User> usersList) {
        setGridUsers();
        if (usersList.size() != 8)
            nextBTN.setDisable(true);
        int row = 0;
        int col = 0;
        for (User u : usersList) {
            Pane card = loadUsersCard(u, null, 0);
            cardsGrid.add(card, col, row);
            col++;
            if (col == 4) {
                col = 0;
                row++;
            }
        }
    }

    private Pane loadUsersCard (User user, String analytics, int value) {
        Pane pane = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unipi/dii/lsmd/winewineryapp/layout/userElement.fxml"));
            pane = loader.load();
            UserElementController ctrl = loader.getController();
            ctrl.setParameters(user, analytics, value);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return pane;
    }
    private void setGridUsers() {
        cleanGrid();
        cardsGrid.setHgap(20);
        cardsGrid.setVgap(20);
        ColumnConstraints constraints = new ColumnConstraints();
        cardsGrid.getColumnConstraints().add(constraints);
    }
    private void goForward () {
        page++;
        backBTN.setDisable(false);
        switch (special) {
            default -> startResearch();
        }
    }

    private void goBack () {
        page--;
        if (page <= 0) {
            page = 0;
            backBTN.setDisable(true);
        }
        nextBTN.setDisable(false);
        switch (special) {
            default -> startResearch();
        }
    }


        @FXML
    public void gotosearch(ActionEvent event) {
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/main.fxml",event);
    }


    @FXML
    public void gotosuggestion(ActionEvent event) {
        this.event = event;
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/suggestion.fxml",event);
    }

    @FXML
    public void gotomyprofile(ActionEvent event) {
        this.event = event;
        ProfileController ctrl = (ProfileController) utilitis.changeScene(
                "/it/unipi/dii/lsmd/winewineryapp/layout/profile.fxml", event);
        if (ctrl != null) {
            ctrl.setProfilePage(Session.getInstance().getLoggedUser());
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        this.event = event;
        Session.resetInstance();
        utilitis.changeScene("/it/unipi/dii/lsmd/winewineryapp/layout/start.fxml",event);
    }


}

