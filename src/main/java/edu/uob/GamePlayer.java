package edu.uob;

import java.util.HashSet;

public class GamePlayer extends GameEntity {
    protected HashSet<GameArtefact> artefacts;

    public GamePlayer(String name, String description, String location) {
        super(name, description, location);
        artefacts = new HashSet<>();
    }

    public void addItem(GameArtefact item) {
        artefacts.add(item);
    }

    public void removeArtefact(String artefactName) {
        for (GameArtefact artefact : artefacts) {
            if (artefact.getName().equalsIgnoreCase(artefactName)) {
                artefacts.remove(artefact);
                break;
            }
        }
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
        stringBuilder.append("The player has \n");
        for (GameArtefact item : artefacts) {
            stringBuilder.append(item.getName()).append("\n");
        }
        return stringBuilder.toString();
    }
}
