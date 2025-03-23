package edu.uob;

import java.util.HashSet;

public class GameAction
{
    private HashSet<String> triggers;
    private HashSet<String> subjects;
    private HashSet<String> consumedItems;
    private HashSet<String> producedItems;
    private String narration;

    GameAction(){
        triggers = new HashSet<String>();
        subjects = new HashSet<String>();
        consumedItems = new HashSet<String>();
        producedItems = new HashSet<String>();
        narration = "";
    }

    public void addTrigger(String trigger){
        triggers.add(trigger);
    }

    public void addSubject(String subject){
        subjects.add(subject);
    }

    public void addConsumed(String c){
        consumedItems.add(c);
    }

    public void addProduced(String p){
        producedItems.add(p);
    }

    public void setNarration(String narration){ this.narration = narration; }

    public HashSet<String> getTriggers() {
        return triggers;
    }

    public HashSet<String> getSubjects() {
        return subjects;
    }

    public HashSet<String> getConsumed() {
        return consumedItems;
    }

    public HashSet<String> getProduced() {
        return producedItems;
    }

    public String getNarration() { return narration; }
}
