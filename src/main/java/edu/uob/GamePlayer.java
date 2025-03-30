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

    public Integer getHealth() {
        return health;
    }

    public void incrementHealth() {
        if(health < 3) {
            health++;
        }
    }

    public void decrementHealth() {
        if(health > 0) {
            health--;
        }
    }

    public void checkHealthAndRespawn(String location) {
        if(health < 1) {
            health = 3;
        }
        setLocation(location);
    }

    public void addItem(GameArtefact item) {
        item.setLocation(name);
        artefacts.add(item);
    }

    public void removeArtefact(String artefactName) {
        Iterator<GameArtefact> itererator = artefacts.iterator();
        while (itererator.hasNext()) {
            if (itererator.next().getName().equalsIgnoreCase(artefactName)) {
                itererator.remove();
                break;
            }
        }
    }

    public void removeAllArtefacts() {
        artefacts.clear();
    }

    public HashSet<String> getArtefactNames() {
        HashSet<String> artefactNames = new HashSet<>();
        for (GameArtefact artefact : artefacts) {
            artefactNames.add(artefact.getName());
        }
        return artefactNames;
    }

    public HashSet<GameArtefact> getArtefacts() {
        return artefacts;
    }

    public GameArtefact getArtefact(String artefactName) {
        for (GameArtefact artefact : artefacts) {
            if (artefact.getName().equalsIgnoreCase(artefactName)) {
                return artefact;
            }
        }
        return null;
    }

    public Boolean isArtefactPresent(String artefactName) {
        for (GameArtefact artefact : artefacts) {
            if (artefact.getName().equalsIgnoreCase(artefactName)) {
                return true;
            }
        }
        return false;
    }

    public String showInventory() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name).append(" has:\n");
        for (GameArtefact item : artefacts) {
            stringBuilder.append(item.getName()).append("\n");
        }
        return stringBuilder.toString();
    }
}
