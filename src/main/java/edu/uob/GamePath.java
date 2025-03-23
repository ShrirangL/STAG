package edu.uob;

public class GamePath {
    private String fromName;
    private String toName;

    public GamePath(){
        fromName = "";
        toName = "";
    }

    public GamePath(String fromName, String toName) {
        this.fromName = fromName;
        this.toName = toName;
    }
}
