package wind.yang.server;

import java.util.List;

public class Message {
    private List<String> users;
    private String user;
    private String text;

    public Message(List<String> users, String user, String text) {
        this.users = users;
        this.user = user;
        this.text = text;
    }

    public Message(List<String> users, String text) {
        this.users = users;
        this.text = text;
    }

    public Message() {
    }

    public int addUser(String userId){
        users.add(userId);
        return users.size();
    }

    public int removeUser(String userId){
        users.remove(userId);
        return users.size();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
