package edu.uob;

import java.util.HashMap;
import java.util.HashSet;

public class GameLocation {
    private final String locationName;
    private final String locationDescription;
    private HashSet<GameCharacter> characters = new HashSet<>();
    private HashSet<GameArtefact> artefacts = new HashSet<>();
    private HashSet<GameFurniture> furnitures = new HashSet<>();

    GameLocation(String locationName, String locationDescription) {
        this.locationName = locationName;
        this.locationDescription = locationDescription;
    }

    public String getLocationName() {
        return locationName;
    }
    public String getLocationDescription() {
        return locationDescription;
    }


    public void addEntity(GameEntity entity) {
        if (entity instanceof GameCharacter) {
            characters.add((GameCharacter) entity);
        } else if (entity instanceof GameArtefact) {
            artefacts.add((GameArtefact) entity);
        } else if (entity instanceof GameFurniture) {
            furnitures.add((GameFurniture) entity);
        }
    }
    public void addCharacter(GameCharacter character){
        this.characters.add(character);
    }
    public void addArtefact(GameArtefact artefact){
        this.artefacts.add(artefact);
    }
    public void addFurniture(GameFurniture furniture){
        this.furnitures.add(furniture);
    }

    public HashSet<GameEntity> getEntities() {
        HashSet<GameEntity> entities = new HashSet<>();
        entities.addAll(characters);
        entities.addAll(artefacts);
        entities.addAll(furnitures);
        return entities;
    }
    public HashSet<GameCharacter> getCharacters(){
        return characters;
    }
    public HashSet<GameArtefact> getArtefacts(){
        return artefacts;
    }
    public HashSet<GameFurniture> getFurnitures(){
        return furnitures;
    }

    public HashSet<String> getEntityNames(){
        HashSet<String> names = new HashSet<>();
        names.addAll(getCharacterNames());
        names.addAll(getArtefactNames());
        names.addAll(getFurnitureNames());
        return names;
    }

    public HashSet<String> getCharacterNames() {
        HashSet<String> names = new HashSet<>();
        for (GameCharacter character : characters) {
            names.add(character.getName());
        }
        return names;
    }

    public HashSet<String> getArtefactNames() {
        HashSet<String> names = new HashSet<>();
        for (GameArtefact artefact : artefacts) {
            names.add(artefact.getName());
        }
        return names;
    }

    public HashSet<String> getFurnitureNames() {
        HashSet<String> names = new HashSet<>();
        for (GameFurniture furniture : furnitures) {
            names.add(furniture.getName());
        }
        return names;
    }

    public boolean isEntityPresent(String entityName){
        return isCharacterPresent(entityName) || isArtefactPresent(entityName) || isFurniturePresent(entityName);
    }

    public Boolean isCharacterPresent(String characterName){
        for(GameCharacter character : characters){
            if(character.getName().equals(characterName)){
                return true;
            }
        }
        return false;
    }

    public Boolean isArtefactPresent(String artefactName){
        for(GameArtefact artefact : artefacts){
            if(artefact.getName().equals(artefactName)){
                return true;
            }
        }
        return false;
    }

    public Boolean isFurniturePresent(String characterName){
        for(GameFurniture furniture : furnitures){
            if(furniture.getName().equals(characterName)){
                return true;
            }
        }
        return false;
    }

    public GameEntity getEntity(String entityName){
        if(isCharacterPresent(entityName)) {
            return getCharacter(entityName);
        }
        else if(isArtefactPresent(entityName)) {
            return getArtefact(entityName);
        }
        else if(isFurniturePresent(entityName)) {
            return getFurniture(entityName);
        }
        return null;
    }

    public GameCharacter getCharacter(String name) {
        for (GameCharacter character : characters) {
            if (character.getName().equalsIgnoreCase(name)) {
                return character;
            }
        }
        return null;
    }

    public GameArtefact getArtefact(String name) {
        for (GameArtefact artefact : artefacts) {
            if (artefact.getName().equalsIgnoreCase(name)) {
                return artefact;
            }
        }
        return null;
    }

    public GameFurniture getFurniture(String name) {
        for (GameFurniture furniture : furnitures) {
            if (furniture.getName().equalsIgnoreCase(name)) {
                return furniture;
            }
        }
        return null;
    }

    public void removeEntity(String entityName){
        if(isCharacterPresent(entityName)) {
            removeCharacter(entityName);
        }
        else if(isArtefactPresent(entityName)) {
            removeArtefact(entityName);
        }
        else if(isFurniturePresent(entityName)) {
            removeFurniture(entityName);
        }
    }


    public void removeCharacter(String characterName){
        characters.removeIf(character -> character.getName().equalsIgnoreCase(characterName));
    }

    public void removeArtefact(String artefactName){
        artefacts.removeIf(artefact -> artefact.getName().equalsIgnoreCase(artefactName));
    }

    public void removeFurniture(String furnitureName){
        furnitures.removeIf(furniture -> furniture.getName().equalsIgnoreCase(furnitureName));
    }
}
