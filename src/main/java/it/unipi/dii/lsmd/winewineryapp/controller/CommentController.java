package it.unipi.dii.lsmd.winewineryapp.controller;

import it.unipi.dii.lsmd.winewineryapp.model.Comment;
import it.unipi.dii.lsmd.winewineryapp.model.Session;
import it.unipi.dii.lsmd.winewineryapp.model.User;
import it.unipi.dii.lsmd.winewineryapp.model.Wine;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDBManager;
import it.unipi.dii.lsmd.winewineryapp.persistence.MongoDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jDriver;
import it.unipi.dii.lsmd.winewineryapp.persistence.Neo4jManager;
import it.unipi.dii.lsmd.winewineryapp.utils.utilitis;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Optional;

public class CommentController {
    private Comment comment;
    private Wine wine;
    private MongoDBManager mongoMan;
    private Neo4jManager neoMan;
    private StringProperty text = new SimpleStringProperty();
    @FXML
    private ScrollPane scrollpane;
    @FXML
    private Label username;
    @FXML
    private Text timestamp;
    @FXML Text usercomment;
    @FXML ImageView deleteBTN;
    @FXML ImageView modifyBTN;
    @FXML private AnchorPane commentBox;

    public void initialize () {
        mongoMan = new MongoDBManager(MongoDriver.getInstance().openConnection());
        neoMan = new Neo4jManager(Neo4jDriver.getInstance().openConnection());
        scrollpane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        modifyBTN.setOnMouseClicked(mouseEvent -> clickModifyBTN(mouseEvent));
        username.setOnMouseClicked(mouseEvent -> clickUsername(mouseEvent));
    }

    public void setCommentCard (Comment cmt, Wine wine, boolean browser) {
        this.comment = cmt;
        this.wine = wine;
        if (browser)
            deleteBTN.setOnMouseClicked(mouseEvent -> clickOnBinBrowser(mouseEvent));
        else
            deleteBTN.setOnMouseClicked(mouseEvent -> clickOnBin(mouseEvent));

        if(Objects.equals(Session.getInstance().getLoggedUser().getUsername(), comment.getUsername())) {
            deleteBTN.setVisible(true);
            modifyBTN.setVisible(true);
        } else {
            if(Session.getInstance().getLoggedUser().getType() > 0) //If the user is a moderator/admin can delete other comments
                deleteBTN.setVisible(true);
            else
                deleteBTN.setVisible(false);
            modifyBTN.setVisible(false);
        }
        if (comment.getUsername().equals("Deleted user"))
            username.setDisable(true);

        username.setText(comment.getUsername());
        Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        timestamp.setText(formatter.format(comment.getTimestamp()));
        usercomment.setText(comment.getText());
    }
    private void clickOnBin (MouseEvent mouseEvent) {
        mongoMan.deleteComment(wine, comment);
        ((VBox) commentBox.getParent()).getChildren().remove(commentBox);
        int numComm = Integer.parseInt(getText());
        numComm--;
        setText(String.valueOf(numComm));
    }

    private void clickOnBinBrowser (MouseEvent mouseEvent) {
        wine = mongoMan.getWineById(wine);
        mongoMan.deleteComment(wine, comment);
        ((GridPane) commentBox.getParent()).getChildren().remove(commentBox);
    }

    public StringProperty textProperty() {
        return text ;
    }
    private final String getText() {
        return textProperty().get();
    }

    private final void setText(String text) {
        textProperty().set(text);
    }

    private void clickModifyBTN (MouseEvent mouseEvent) {
        TextInputDialog dialog = new TextInputDialog(comment.getText());
        dialog.setHeaderText(null);
        dialog.setTitle("Edit comment");
        Optional<String> result = dialog.showAndWait();
        usercomment.setText(result.get());
        comment.setText(result.get());
        if (result.isPresent()){
            mongoMan.updateComment(wine, comment);
        }
    }

    private void clickUsername(MouseEvent mouseEvent){
        if(!comment.getUsername().equals("Deleted user")) {
            User u = mongoMan.getUserByUsername(comment.getUsername());
            ProfileController ctrl = (ProfileController) utilitis.changeScene(
                    "/it/unipi/dii/lsmd/winewineryapp/layout/profile.fxml", mouseEvent);
            ctrl.setProfilePage(u);
        }
    }
}

