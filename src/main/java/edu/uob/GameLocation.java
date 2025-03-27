package edu.uob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class GameLocation {
    private final String locationName;
    private final String locationDescription;
    private HashSet<GameCharacter> characters = new HashSet<>();
    private HashSet<GameArtefact> artefacts = new HashSet<>();
    private HashSet<GameFurniture> furnitures = new HashSet<>();
    private HashSet<String> players = new HashSet<>();

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
        entity.setLocation(locationName);
        if (entity instanceof GameCharacter) {
            characters.add((GameCharacter) entity);
        } else if (entity instanceof GameArtefact) {
            artefacts.add((GameArtefact) entity);
        } else if (entity instanceof GameFurniture) {
            furnitures.add((GameFurniture) entity);
        }
    }
    public void addCharacter(GameCharacter character){
        character.setLocation(locationName);
        this.characters.add(character);
    }
    public void addArtefact(GameArtefact artefact){
        artefact.setLocation(locationName);
        this.artefacts.add(artefact);
    }
    public void addFurniture(GameFurniture furniture){
        furniture.setLocation(locationName);
        this.furnitures.add(furniture);
    }
    public void addPlayer(String playerName){
        this.players.add(playerName);
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
    public HashSet<String> getPlayers(){return players;}

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
            names.add(character.getName().toLowerCase());
        }
        return names;
    }

    public HashSet<String> getArtefactNames() {
        HashSet<String> names = new HashSet<>();
        for (GameArtefact artefact : artefacts) {
            names.add(artefact.getName().toLowerCase());
        }
        return names;
    }

    public HashSet<String> getFurnitureNames() {
        HashSet<String> names = new HashSet<>();
        for (GameFurniture furniture : furnitures) {
            names.add(furniture.getName().toLowerCase());
        }
        return names;
    }

    public boolean isEntityPresent(String entityName){
        return this.isCharacterPresent(entityName) || this.isArtefactPresent(entityName) || this.isFurniturePresent(entityName);
    }

    public Boolean isCharacterPresent(String characterName){
        for(GameCharacter character : characters){
            if(character.getName().equalsIgnoreCase(characterName)){
                return true;
            }
        }
        return false;
    }

    public Boolean isArtefactPresent(String artefactName){
        for(GameArtefact artefact : artefacts){
            if(artefact.getName().equalsIgnoreCase(artefactName)){
                return true;
            }
        }
        return false;
    }

    public Boolean isFurniturePresent(String characterName){
        for(GameFurniture furniture : furnitures){
            if(furniture.getName().equalsIgnoreCase(characterName)){
                return true;
            }
        }
        return false;
    }

    public GameEntity getEntity(String entityName){
        if(this.isCharacterPresent(entityName)) {
            return this.getCharacter(entityName);
        }
        else if(this.isArtefactPresent(entityName)) {
            return this.getArtefact(entityName);
        }
        else if(this.isFurniturePresent(entityName)) {
            return this.getFurniture(entityName);
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
        if(this.getArtefactNames().contains(entityName)) {
            this.removeArtefact(entityName);
        }
        else if(this.getCharacterNames().contains(entityName)) {
            this.removeCharacter(entityName);
        }
        else if(this.getFurnitureNames().contains(entityName)) {
            this.removeFurniture(entityName);
        }
    }


    public void removeCharacter(String characterName){
        if(this.isCharacterPresent(characterName)) {
            Iterator<GameCharacter> iterator = characters.iterator();
            while (iterator.hasNext()) {
                GameCharacter character = iterator.next();
                if (character.getName().equalsIgnoreCase(characterName)) {
                    iterator.remove();
                }
            }
        }
    }

    public void removeArtefact(String artefactName){
        if(this.isArtefactPresent(artefactName)) {
            Iterator<GameArtefact> iterator = artefacts.iterator();
            while (iterator.hasNext()) {
                GameArtefact artefact = iterator.next();
                if (artefact.getName().equalsIgnoreCase(artefactName)) {
                    iterator.remove();
                }
            }
        }
    }

    public void removeFurniture(String furnitureName){
        Iterator<GameFurniture> iterator = furnitures.iterator();
        while (iterator.hasNext()) {
            GameFurniture furniture = iterator.next();
            if (furniture.getName().equalsIgnoreCase(furnitureName)) {
                iterator.remove();  // Safely remove the element
            }
        }
    }

    public void removePlayer(String playerName){
        players.remove(playerName);
    }
}
