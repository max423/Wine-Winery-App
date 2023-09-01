package it.unipi.dii.lsmd.winewineryapp.model;
import java.util.Date;

public class Comment {
    private String username;

    private String text;
    private Date timestamp;

    public Comment(String username, String text, Date timestamp) {
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = username;
    }


    public String getText() {
        return text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "username='" + username + '\'' +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }


}
