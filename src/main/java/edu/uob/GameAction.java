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

    /**
     * Adds trigger word for action
     * @param trigger Trigger text
     */
    public void addTrigger(String trigger){
        triggers.add(trigger);
    }

    /**
     * Adds subject entity of action
     * @param subject subject entity name
     */
    public void addSubject(String subject){
        subjects.add(subject);
    }

    /**
     * Add consumed entity of action
     * @param consumed consumed entity name
     */
    public void addConsumed(String consumed){
        consumedItems.add(consumed);
    }

    /**
     * Add produces entity of action
     * @param produced produced entity name
     */
    public void addProduced(String produced){
        producedItems.add(produced);
    }

    /**
     * Set narration for this action
     * @param narration Narration text
     */
    public void setNarration(String narration){ this.narration = narration; }

    /**
     * Get set of trigger of this action
     * @return trigger words
     */
    public HashSet<String> getTriggers() {
        return triggers;
    }

    /**
     * Get set of subjects of this action
     * @return subject entity names
     */
    public HashSet<String> getSubjects() {
        return subjects;
    }

    /**
     * Get set of consumed entities of this action
     * @return consumed entities names
     */
    public HashSet<String> getConsumed() {
        return consumedItems;
    }

    /**
     * Get set of produced entities of this action
     * @return produced entities names
     */
    public HashSet<String> getProduced() {
        return producedItems;
    }

    /**
     * Get narration for this action
     * @return Narration text
     */
    public String getNarration() { return narration; }
}
