package edu.uob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Collectors;

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


    /**
     * Adds a new entity to list of entities based on its type
     * @param entity GameEntity object
     */
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

    /**
     * Adds a new character to list of characters for this location
     * @param character GameCharacter object
     */
    public void addCharacter(GameCharacter character){
        character.setLocation(locationName);
        this.characters.add(character);
    }

    /**
     * Adds a new artefact to list of artefacts for this location
     * @param artefact GameArtefact object
     */
    public void addArtefact(GameArtefact artefact){
        artefact.setLocation(locationName);
        this.artefacts.add(artefact);
    }

    /**
     * Add new furniture to list of furniture for this location
     * @param furniture GameFurniture object
     */
    public void addFurniture(GameFurniture furniture){
        furniture.setLocation(locationName);
        this.furnitures.add(furniture);
    }

    /**
     * Add new player to the list of players at this location
     * @param playerName Player name
     */
    public void addPlayer(String playerName){
        this.players.add(playerName);
    }

    /**
     * Retrieves list of all the entities present at the location
     * @return Set of entities
     */
    public HashSet<GameEntity> getEntities() {
        HashSet<GameEntity> entities = new HashSet<>();
        entities.addAll(characters);
        entities.addAll(artefacts);
        entities.addAll(furnitures);
        return entities;
    }

    /**
     * Retrieves set of characters present at the location
     * @return Set of character
     */
    public HashSet<GameCharacter> getCharacters(){
        return characters;
    }

    /**
     * Retrieves list of artefacts present at the location
     * @return Set of artefacts
     */
    public HashSet<GameArtefact> getArtefacts(){
        return artefacts;
    }

    /**
     * Retrieves list of furniture items present at the location
     * @return Set of furniture items
     */
    public HashSet<GameFurniture> getFurnitures(){
        return furnitures;
    }

    /**
     * Retrieves list of player present at the location
     * @return Set of player names
     */
    public HashSet<String> getPlayers(){return players;}

    /**
     * Retrieves names of all the entities present at the location
     * @return Set of names of entities
     */
    public HashSet<String> getEntityNames(){
        HashSet<String> names = new HashSet<>();
        names.addAll(getCharacterNames());
        names.addAll(getArtefactNames());
        names.addAll(getFurnitureNames());
        return names;
    }

    /**
     * Retrieves names of all the characters present at the location
     * @return Set of names of characters
     */
    public HashSet<String> getCharacterNames() {
        HashSet<String> names = new HashSet<>();
        for (GameCharacter character : characters) {
            names.add(character.getName().toLowerCase());
        }
        return names;
    }

    /**
     * Retrieves names of all the artefacts present at the location
     * @return Set of names of artefacts
     */
    public HashSet<String> getArtefactNames() {
        HashSet<String> names = new HashSet<>();
        for (GameArtefact artefact : artefacts) {
            names.add(artefact.getName().toLowerCase());
        }
        return names;
    }

    /**
     * Retrieves name of all the furniture items present at the location
     * @return Set of names of artefacts
     */
    public HashSet<String> getFurnitureNames() {
        HashSet<String> names = new HashSet<>();
        for (GameFurniture furniture : furnitures) {
            names.add(furniture.getName().toLowerCase());
        }
        return names;
    }

    /**
     * Checks whether an entity is present at the location
     * @param entityName name of entity
     * @return True if present else false
     */
    public boolean isEntityPresent(String entityName){
        return this.isCharacterPresent(entityName) || this.isArtefactPresent(entityName) || this.isFurniturePresent(entityName);
    }

    /**
     * Checks whether a character is present at the location
     * @param characterName name of character
     * @return True if present else false
     */
    public Boolean isCharacterPresent(String characterName){
        for(GameCharacter character : characters){
            if(character.getName().equalsIgnoreCase(characterName)){
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether an artefact is present at the location
     * @param artefactName name of artefact
     * @return True if present else false
     */
    public Boolean isArtefactPresent(String artefactName){
        for(GameArtefact artefact : artefacts){
            if(artefact.getName().equalsIgnoreCase(artefactName)){
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether a character is present at the location
     * @param characterName name of character
     * @return True if present else false
     */
    public Boolean isFurniturePresent(String characterName){
        for(GameFurniture furniture : furnitures){
            if(furniture.getName().equalsIgnoreCase(characterName)){
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves GameEntity object corresponding to an entity name
     * @param entityName name of entity
     * @return GameEntity object
     */
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

    /**
     * Retrieves GameCharacter object corresponding to a character name
     * @param name name of character
     * @return GameCharacter object
     */
    public GameCharacter getCharacter(String name) {
        for (GameCharacter character : characters) {
            if (character.getName().equalsIgnoreCase(name)) {
                return character;
            }
        }
        return null;
    }

    /**
     * Retrieves GameArtefact object corresponding to a character name
     * @param name name of artefact
     * @return GameArtefact object
     */
    public GameArtefact getArtefact(String name) {
        for (GameArtefact artefact : artefacts) {
            if (artefact.getName().equalsIgnoreCase(name)) {
                return artefact;
            }
        }
        return null;
    }

    /**
     * Retrieves GameFurniture object corresponding to a character name
     * @param name name of furniture item
     * @return GameFurniture object
     */
    public GameFurniture getFurniture(String name) {
        for (GameFurniture furniture : furnitures) {
            if (furniture.getName().equalsIgnoreCase(name)) {
                return furniture;
            }
        }
        return null;
    }

    /**
     * Removes an entity corresponding to an entity name
     * @param entityName name of entity
     */
    public void removeEntity(String entityName){
        if(this.isArtefactPresent(entityName)) {
            this.removeArtefact(entityName);
        }
        else if(this.isCharacterPresent(entityName)) {
            this.removeCharacter(entityName);
        }
        else if(this.isFurniturePresent(entityName)) {
            this.removeFurniture(entityName);
        }
    }


    /**
     * Removes a character corresponding to a character name
     * @param characterName name of character
     */
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

    /**
     * Removes an artefact corresponding to artefact  name
     * @param artefactName name of artefact
     */
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

    /**
     * Removes a furniture corresponding to furniture  name
     * @param furnitureName name of furniture
     */
    public void removeFurniture(String furnitureName){
        Iterator<GameFurniture> iterator = furnitures.iterator();
        while (iterator.hasNext()) {
            GameFurniture furniture = iterator.next();
            if (furniture.getName().equalsIgnoreCase(furnitureName)) {
                iterator.remove();
            }
        }
    }

    /**
     * Removes a player corresponding to player name
     * @param playerName name of player
     */
    public void removePlayer(String playerName){
        Iterator<String> iterator = players.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equalsIgnoreCase(playerName)) {
                iterator.remove();
            }
        }
    }
}
