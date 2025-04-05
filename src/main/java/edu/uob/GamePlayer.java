package edu.uob;

import java.util.HashSet;
import java.util.Iterator;

public class GamePlayer extends GameEntity {
    protected HashSet<GameArtefact> artefacts;
    private int health;

    public GamePlayer(String name, String description, String location) {
        super(name, description, location);
        artefacts = new HashSet<>();
        health = 3;
    }

    /**
     * Retrieves health of a player
     * @return player's health
     */
    public Integer getHealth() {
        return health;
    }

    /**
     * Increments player's health if it is less than 3
     */
    public void incrementHealth() {
        if(health < 3) {
            health++;
        }
    }

    /**
     * Decrements player's health if it is more than 0
     */
    public void decrementHealth() {
        if(health > 0) {
            health--;
        }
    }

    /**
     * Check if player's health has reached zero and set it to new location with full health
     * @param location name of new location after respawn
     */
    public void checkHealthAndRespawn(String location) {
        if(health < 1) {
            health = 3;
        }
        setLocation(location);
    }

    /**
     * Adds an artefact to player's inventory
     * @param item GameArtefact object
     */
    public void addArtefactToInventory(GameArtefact item) {
        item.setLocation(name);
        artefacts.add(item);
    }

    /**
     * Removes an artefact from player's inventory
     * @param artefactName name of artefact
     */
    public void removeArtefactFromInventory(String artefactName) {
        Iterator<GameArtefact> iterator = artefacts.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getName().equalsIgnoreCase(artefactName)) {
                iterator.remove();
                break;
            }
        }
    }

    /**
     * Empties a player's inventory
     */
    public void removeAllArtefacts() {
        artefacts.clear();
    }

    /**
     * Retrieves list of names of artefacts in player's inventory
     * @return Set of artefact names
     */
    public HashSet<String> getArtefactNames() {
        HashSet<String> artefactNames = new HashSet<>();
        if(artefacts != null && !artefacts.isEmpty()) {
            for (GameArtefact artefact : artefacts) {
                artefactNames.add(artefact.getName());
            }
        }
        return artefactNames;
    }

    /**
     * Retrieves set of GameArtefact objects in player's inventory
     * @return Set of GameArtefact objects
     */
    public HashSet<GameArtefact> getArtefacts() {
        return artefacts;
    }

    /**
     * Retrieves game artefact object corresponding to an artefact name
     * @param artefactName name of artefact
     * @return GameArtefact object
     */
    public GameArtefact getArtefact(String artefactName) {
        if(artefacts != null && !artefacts.isEmpty()) {
            for (GameArtefact artefact : artefacts) {
                if (artefact.getName().equalsIgnoreCase(artefactName)) {
                    return artefact;
                }
            }
        }
        return null;
    }

    /**
     * Checks if artefact is present in player's inventory
     * @param artefactName name of artefact
     * @return True if present else false
     */
    public Boolean isArtefactPresentInInventory(String artefactName) {
        if(artefacts != null && !artefacts.isEmpty()) {
            for (GameArtefact artefact : artefacts) {
                if (artefact.getName().equalsIgnoreCase(artefactName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns contents of player's inventory to user
     * @return Text describing what is present in player's inventory
     */
    public String showInventoryContents() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name).append(" has:").append(System.lineSeparator());
        for (GameArtefact item : artefacts) {
            stringBuilder.append(item.getName()).append(System.lineSeparator());
        }
        return stringBuilder.toString();
    }
}
