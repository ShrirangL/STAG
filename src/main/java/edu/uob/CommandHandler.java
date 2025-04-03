package edu.uob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class CommandHandler {
    private HashMap<String, HashSet<GameAction>> gameActions;
    private HashMap<String, HashSet<String>> gamePaths;
    private HashMap<String, GameLocation> gameLocations;
    private HashMap<String, GamePlayer> gamePlayers;
    private final String playersStartLocation;

    //Built in triggers supported by the game
    private final HashSet<String> builtInTriggers = new HashSet<>(Set.of("inventory", "inv", "get", "drop", "goto", "look", "health"));

    //All triggers from all the actions
    private HashSet<String> availableTriggers;

    //All the subjects from all actions
    private HashSet<String> availableSubjects;

    CommandHandler(HashMap<String, HashSet<GameAction>> actions, HashMap<String, HashSet<String>> paths,
                   HashMap<String, GameLocation> locations, HashMap<String, GamePlayer> player, String startLocation) {
        gameActions = actions;
        gamePaths = paths;
        gameLocations = locations;
        gamePlayers = player;
        playersStartLocation = startLocation;
        availableTriggers = new HashSet<>();
        availableSubjects = new HashSet<>();
        this.computeAvailableTriggers();
        this.computeAvailableSubjects();
    }

    /**
     * Compiles a list of all the available Triggers and populate availableTriggers
     */
    private void computeAvailableTriggers() {
        availableTriggers.addAll(builtInTriggers);
        availableTriggers.addAll(gameActions.keySet());
    }

    /**
     * Compiles a list of all the available subjects and populate availableSubjects
     */
    private void computeAvailableSubjects(){
        // action can be performed on artefacts, characters, furniture and location
        for (GameLocation location : gameLocations.values()) {
            availableSubjects.add(location.getLocationName());
            availableSubjects.addAll(location.getArtefactNames());
            availableSubjects.addAll(location.getCharacterNames());
            availableSubjects.addAll(location.getFurnitureNames());
        }
    }

    /**
    * Parses the incoming command to deduce which action needs to be performed
    * based on triggers and subjects in the command
    * @param gamePlayer The current player who is performing the action
    * @param input Command provided by the user in raw form
     * @return Action narration
    */
    public String parseAction(GamePlayer gamePlayer, String input) {
        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());
        if (gameLocation == null) {
            throw new RuntimeException("Invalid player location");
        }
        input = input.trim();
        if(input.equalsIgnoreCase("gamestate")) {
            return this.printGameState();
        }
        StringBuilder command = new StringBuilder(input);
        HashSet<String> triggers = new HashSet<String>();
        compileListOfKeywordsFromCommand(command, triggers, availableTriggers);
        HashSet<String> subjects = new HashSet<>();
        compileListOfKeywordsFromCommand(command, subjects, availableSubjects);
        HashSet<String> players = new HashSet<>();
        compileListOfKeywordsFromCommand(command, players, new HashSet<String>(gamePlayers.keySet()));

        //Ensure that the command does not have names of other players
        if(this.doesContainWordsExcept(players, new HashSet<>(Set.of(gamePlayer.getName())))) {
            throw new RuntimeException("Name(s) of other players are not allowed in command");
        }
        //We must have at least one trigger
        if (triggers.isEmpty()) {
            throw new RuntimeException("No action found");
        }

        return finaliseAction(gamePlayer, triggers, subjects);
    }

    /**
     * Based on the triggers found in the command, tries to perform built-in or custom action
     * @param gamePlayer The current player who is performing the action
     * @param triggers Triggers of actions found in the command
     * @param subjects Subjects of actions found in the command
     * @return Action narration
     */
    private String finaliseAction(GamePlayer gamePlayer, HashSet<String> triggers, HashSet<String> subjects) {
        String trigger = triggers.iterator().next();
        switch (trigger) {
            case "inv":
            case "inventory":
                return this.parseActionInventory(gamePlayer, triggers, subjects);
            case "get":
                return this.parseActionGet(gamePlayer, triggers, subjects);
            case "drop":
                return this.parseActionDrop(gamePlayer, triggers, subjects);
            case "goto":
                return this.parseActionGoto(gamePlayer, triggers, subjects);
            case "look":
                return this.parseActionLook(gamePlayer, triggers, subjects);
            case "health":
                return this.parseActionHealth(gamePlayer, triggers, subjects);
            default:
                return this.parseComplexAction(gamePlayer, triggers, subjects);
        }
    }

    /**
     * Compiles list of keywords(triggers or subjects) found in input command
     * @param command Command entered by user
     * @param foundKeywords Keywords found in the command
     * @param availableKeywords List of available keywords
     */
    private void compileListOfKeywordsFromCommand(StringBuilder command, HashSet<String> foundKeywords, HashSet<String> availableKeywords) {
        for(String keyword : availableKeywords){
            StringBuilder patternString = new StringBuilder();
            patternString.append("\\b").append(keyword).append("\\b");
            Pattern pattern = Pattern.compile(patternString.toString(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(command);

            while (matcher.find()) {
                foundKeywords.add(keyword);
                command.delete(matcher.start(), matcher.end());
                matcher = pattern.matcher(command);
            }
        }
    }

    /**
     * Find whether input set of string contains other words except certain exceptions.
     * @param words Set of strings.
     * @param exceptions Exceptions which are allowed to be present in words.
     * @return True if word other than exceptions is found else false
     */
    private boolean doesContainWordsExcept(HashSet<String> words, HashSet<String> exceptions) {
        for (String word : words) {
            boolean found = false;
            for (String exception : exceptions) {
                if (word.equalsIgnoreCase(exception)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the input subject entity is actually a location
     * @param subject Input subject
     * @return True if subject is location else false
     */
    private boolean isThisSubjectLocation(String subject) {
        for(String location : gameLocations.keySet()) {
            if(location.equalsIgnoreCase(subject)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs a case-insensitive check for a location name in list of locations
     * and retrieves the location.
     * @param location Name of the location.
     * @return GameLocation object corresponding to the input name
     */
    private GameLocation getGameLocation(String location) {
        for(String locationName : gameLocations.keySet()) {
            if(location.equalsIgnoreCase(locationName)) {
                return gameLocations.get(locationName);
            }
        }
        return null;
    }

    /**
     * Creates one way path between origin and destination location if it doesn't exist
     * @param origin Name of the origin location.
     * @param destination Name of destination location.
     */
    private void createPathBetween(String origin, String destination) {
        // See if origin is actually a location
        // see if destination is actually a location
        GameLocation originLocation = this.getGameLocation(origin);
        GameLocation destinationLocation = this.getGameLocation(destination);
        if(originLocation == null || destinationLocation == null) {return;}

        gamePaths.putIfAbsent(originLocation.getLocationName(), new HashSet<>());
        HashSet<String> destinations = gamePaths.get(originLocation.getLocationName());

        if(destinations != null && !destinations.contains(destinationLocation.getLocationName())) {
            destinations.add(destinationLocation.getLocationName());
        }
    }

    /**
     * Removes one way path between origin and destination location if it exists
     * @param origin Name of the origin location.
     * @param destination Name of destination location.
     */
    private void removePathBetween(String origin, String destination) {
        //See if origin is actually a location
        // see if destination is actually a location
        GameLocation originLocation = this.getGameLocation(origin);
        GameLocation destinationLocation = this.getGameLocation(destination);
        if(originLocation == null || destinationLocation == null) {return;}

        // See if a path already exists. if exists then remove path
        HashSet<String> destinations = gamePaths.get(originLocation.getLocationName());
        if(destinations != null && destinations.contains(destinationLocation.getLocationName())) {
            destinations.remove(destinationLocation.getLocationName());
        }
    }

    /**
     * Check whether a one way path exists between two locations
     * @param from Name of start location
     * @param to Name of destination location
     * @return True if path exists else False
     */
    private boolean doesPathExistBetween(String from, String to) {
        GameLocation start = null;
        for(String location : gameLocations.keySet()) {
            if(location.equalsIgnoreCase(from)) {
                start = gameLocations.get(location);
            }
        }

        if(start == null) {
            throw new RuntimeException("Invalid start location");
        }

        for(String destination : gamePaths.get(start.getLocationName())) {
            if(destination.equalsIgnoreCase(to)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs built-in action inventory to show the artefacts carried by a player
     * @param gamePlayer Name of player performing an action
     * @param triggers Triggers of actions found in the command
     * @param subjects Subjects of actions found in the command
     * @return Contents of player's inventory
     */
    private String parseActionInventory(GamePlayer gamePlayer, HashSet<String> triggers, HashSet<String> subjects) {
        //  for inventory, we only need word inventory no extra built-in
        //  trigger or subjects should be present

        if(!subjects.isEmpty()) {
            throw new RuntimeException("Subject not allowed in command");
        }

        if(this.doesContainWordsExcept(triggers, new HashSet<String> (Set.of("inv", "inventory")))) {
            throw new RuntimeException("Multiple triggers now allowed in command");
        }
        return gamePlayer.showInventory();
    }

    /**
     * Performs built-in action 'get' to pick up an artefact and put it in player's inventory
     * @param gamePlayer Name of player performing an action
     * @param triggers Triggers of actions found in the command
     * @param subjects Subjects of actions found in the command
     * @return Description of artefact picked up by the player
     */
    private String parseActionGet(GamePlayer gamePlayer, HashSet<String> triggers, HashSet<String> subjects) {
        if(this.doesContainWordsExcept(triggers, new HashSet<String>(Set.of("get")))) {
            throw new RuntimeException("Multiple triggers not allowed in get command");
        }

        if(subjects.isEmpty()) {
            throw new RuntimeException("Get command requires a subjects");
        } else if(subjects.size() > 1) {
            throw new RuntimeException("Multiple subjects not allowed in get command");
        }

        String subject = subjects.iterator().next();
        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());
        if(gameLocation == null) {
            throw new RuntimeException("Invalid player location");
        }

        // the subject should be artefact and should be present in the current location of the player
        if(!gameLocation.isArtefactPresent(subject)) {
            throw new RuntimeException("Artefact could not be found in current location");
        }

        // Remove the item from the location and add it in player's inventory
        gamePlayer.addItem(gameLocation.getArtefact(subject));
        gameLocation.removeArtefact(subject);
        StringBuilder ret = new StringBuilder();
        ret.append(gamePlayer.getName()).append(" picked up ").append(subject);
        return ret.toString();
    }

    /**
     * Performs built-in action 'drop' to unload up an artefact from player's inventory
     * @param gamePlayer Name of player performing an action
     * @param triggers Triggers of actions found in the command
     * @param subjects Subjects of actions found in the command
     * @return Description of artefact dropped by the player
     */
    private String parseActionDrop(GamePlayer gamePlayer, HashSet<String> triggers, HashSet<String> subjects) {
        if(this.doesContainWordsExcept(triggers, new HashSet<String>(Set.of("drop")))) {
            throw new RuntimeException("Multiple triggers not allowed in drop command");
        }

        if(subjects.isEmpty()) {
            throw new RuntimeException("drop command requires a subjects");
        }
        else if(subjects.size() > 1) {
            throw new RuntimeException("Multiple subjects not allowed in drop command");
        }

        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());
        if(gameLocation == null) {
            throw new RuntimeException("Invalid player location");
        }

        String subject = subjects.iterator().next();
        // the subject should be artefact and should be present in player's inventory
        if(!gamePlayer.isArtefactPresent(subject)) {
            throw new RuntimeException("Artefact could not be found in player's inventory");
        }
        gameLocation.addArtefact(gamePlayer.getArtefact(subject));
        gamePlayer.removeArtefact(subject);

        StringBuilder ret = new StringBuilder();
        ret.append(gamePlayer.getName()).append(" dropped ").append(subject);
        return ret.toString();
    }

    /**
     * Performs built-in action 'goto' to unload up an artefact from player's inventory
     * and drop it at player's current location
     * @param gamePlayer Name of player performing an action
     * @param triggers Triggers of actions found in the command
     * @param subjects Subjects of actions found in the command
     * @return Description of player's perspective in the new location
     */
    private String parseActionGoto(GamePlayer gamePlayer, HashSet<String> triggers, HashSet<String> subjects) {
        if(this.doesContainWordsExcept(triggers, new HashSet<String> (Set.of("goto")))) {
            throw new RuntimeException("Multiple triggers not allowed in goto command");
        }

        if(subjects.size() != 1) {
            throw new RuntimeException("Goto command must have exactly one valid subject");
        }

        String newLocationName = subjects.iterator().next();
        if(!this.isThisSubjectLocation(newLocationName)) {
            throw new RuntimeException("Goto command must have a valid location");
        }
        if(newLocationName.equalsIgnoreCase(gamePlayer.getLocation())) {
            throw new RuntimeException("You are already at this location");
        }
        // see if there is a path from current location to the destination
        if (!this.doesPathExistBetween(gamePlayer.getLocation(), newLocationName)) {
            throw new RuntimeException("Location is not accessible from current location of the player");
        }

        GameLocation oldLocation = gameLocations.get(gamePlayer.getLocation());
        GameLocation newLocation = gameLocations.get(newLocationName);
        newLocation.addPlayer(gamePlayer.getName());
        gamePlayer.setLocation(newLocationName);
        oldLocation.removePlayer(gamePlayer.getName());
        return this.getPlayerPerspective(gamePlayer);
    }

    /**
     * Performs built-in action 'look' to show to user contents of player's
     * current location
     * @param gamePlayer Name of player performing an action
     * @param triggers Triggers of actions found in the command
     * @param subjects Subjects of actions found in the command
     * @return Description of what player can see at its current location
     */
    private String parseActionLook(GamePlayer gamePlayer, HashSet<String> triggers, HashSet<String> subjects) {
        // no other trigger, no subjects and no location is required
        if(this.doesContainWordsExcept(triggers, new HashSet<String>(Set.of("look")))) {
            throw new RuntimeException("Multiple triggers not allowed in look command");
        }
        if(!subjects.isEmpty()) {
            throw new RuntimeException("Look command requires does not require subjects");
        }

        return this.getPlayerPerspective(gamePlayer);
    }

    /**
     * Performs built-in action 'health' to show player's health
     * @param gamePlayer Name of player performing an action
     * @param triggers Triggers of actions found in the command
     * @param subjects Subjects of actions found in the command
     * @return Message mentioning current status of player's health
     */
    private String parseActionHealth(GamePlayer gamePlayer, HashSet<String> triggers, HashSet<String> subjects) {
        // no other trigger, no subjects and no location is required
        if(this.doesContainWordsExcept(triggers, new HashSet<String> (Set.of("health")))) {
            throw new RuntimeException("Multiple triggers not allowed in health command");
        }
        if(!subjects.isEmpty()) {
            throw new RuntimeException("Health command requires does not require subjects");
        }

        StringBuilder ret = new StringBuilder();
        ret.append(gamePlayer.getName()).append("'s health is: ").append(gamePlayer.getHealth()).append(System.lineSeparator());
        return ret.toString();
    }

    /**
     * Return player's perspective i.e; things which can be seen by player at a location
     * @param gamePlayer Name of player performing an action
     * @return String describing what a player can see at its current location
     */
    private String getPlayerPerspective(GamePlayer gamePlayer) {
        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("You are in ").append(gameLocation.getLocationDescription()).append(System.lineSeparator());
        stringBuilder.append("You can see:").append(System.lineSeparator());
        //iterate through characters, artefacts, furniture
        for (GameCharacter character : gameLocation.getCharacters()) {
            stringBuilder.append(character.getName()).append(": ").append(character.getDescription()).append(System.lineSeparator());
        }
        for (GameArtefact gameArtefact : gameLocation.getArtefacts()) {
            stringBuilder.append(gameArtefact.getName()).append(": ").append(gameArtefact.getDescription()).append(System.lineSeparator());
        }
        for (GameFurniture furniture : gameLocation.getFurnitures()) {
            stringBuilder.append(furniture.getName()).append(": ").append(furniture.getDescription()).append(System.lineSeparator());
        }
        for (String player : gameLocation.getPlayers()) {
            if(!gamePlayer.getName().equalsIgnoreCase(player)) {
                stringBuilder.append("Player :").append(player).append(System.lineSeparator());
            }
        }
        stringBuilder.append("You can access from here:").append(System.lineSeparator());
        HashSet<String> gPaths = gamePaths.get(gamePlayer.getLocation());
        for (String path : gPaths) {
            stringBuilder.append(path).append(System.lineSeparator());
        }
        return stringBuilder.toString();
    }

    /**
     * Performs custom actions defined in actions.xml file after checking for extraneous
     * entities, ambiguity and availability of subjects to perform an action. Then if an
     * action is performed return the narration of the action.
     * @param gamePlayer Name of player performing an action
     * @param triggers Triggers of actions found in the command
     * @param subjects Subjects of actions found in the command
     * @return Narration of action after performing it
     */
    private String parseComplexAction(GamePlayer gamePlayer, HashSet<String> triggers, HashSet<String> subjects) {
        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());

        //See if the input command is valid
        GameAction commandAction = this.isCommandValid(gamePlayer, triggers, subjects);
        //See if we can act on valid query
        this.ensureActionIsPerformable(commandAction, gamePlayer, gameLocation);

        //Act on the query/command
        this.produceEntity(commandAction, gamePlayer, gameLocation);
        this.consumeEntity(commandAction, gamePlayer, gameLocation);

        //Print the narration of the action
        return commandAction.getNarration();
    }

    /**
     * Checks if command provided by user is valid i.e; The command does not contain
     * extraneous entities, the command is not ambiguous.
     * @param player Name of player performing an action
     * @param triggers Triggers of actions found in the command
     * @param subjects Subjects of actions found in the command
     * returns GameAction deduced from command
     */
    private GameAction isCommandValid(GamePlayer player, HashSet<String> triggers, HashSet<String> subjects) {
        GameAction commandAction  = null; // Action to be performed
        if(triggers.isEmpty()) {
            throw new RuntimeException("No command triggers in action ");
        }

        HashSet<GameAction> validActions = compilePossibleActionsFromCommand(triggers, subjects);
        if (validActions.size() != 1) {
            throw new RuntimeException("Input command is ambiguous");
        }
        commandAction = validActions.iterator().next();

        //Ensure that the command does not contain subjects other than the ones required to perform this action
        if(this.doesContainWordsExcept(subjects, commandAction.getSubjects())) {
            throw new RuntimeException("All the subjects should be from exactly one action");
        }
        return commandAction;
    }

    /**
     * Compiles a list of all possible actions based on command. An action is possible if we have
     * at least one trigger and at least one subject
     * @param triggers Triggers in the command
     * @param subjects Subjects in the command
     * @return Set of action which can be formed
     */
    private HashSet<GameAction> compilePossibleActionsFromCommand(HashSet<String> triggers, HashSet<String> subjects) {
        HashSet<GameAction> validActions = new HashSet<GameAction>();
        HashSet<GameAction> possibleActions = new HashSet<GameAction>();
        for(String commandTrigger : triggers) {
            possibleActions.addAll(gameActions.get(commandTrigger));
        }

        //keep iterating through all the actions and when a subject from an action is found
        // there should be no other subjects from a different action
        for (GameAction action : possibleActions) {
            // compile a list of subjects for this action
            Iterator<String> itr = action.getSubjects().iterator();
            while (itr.hasNext()) {
                String subject = itr.next();
                if (subjects.contains(subject)) {
                    validActions.add(action);
                    break;
                }
            }
        }
        return validActions;
    }

    /**
     * Checks if an action can actually be performed by a player i.e; the entities
     * required to perform an action is available to the player
     * @param commandAction Action to be performed
     * @param gamePlayer Player performing the action
     * @param gameLocation Current location of the player
     */
    private void ensureActionIsPerformable(GameAction commandAction, GamePlayer gamePlayer, GameLocation gameLocation) {
        //We should now check if all the subject required to perform the actions are either possessed by player or in the room.
        // If even a single subject is unavailable throw exception.
        HashSet<String> availableEntities = new HashSet<>();
        availableEntities.addAll(gamePlayer.getArtefactNames()); // player's inventory
        availableEntities.addAll(gameLocation.getEntityNames()); // Entities at current location
        availableEntities.add(gamePlayer.getLocation()); // Current location

        for(String subject: commandAction.getSubjects()) {
            if(!availableEntities.stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet()).contains(subject.toLowerCase())) {
                throw new RuntimeException("Subject(s) required to execute action are missing");
            }
        }
    }

    /**
     * Moves the produced entity from its current location to player's current location
     * @param commandAction Action to be performed
     * @param gamePlayer Player performing the action
     * @param gameLocation Current location of the player
     */
    private void produceEntity(GameAction commandAction, GamePlayer gamePlayer, GameLocation gameLocation) {
        // produced item is moved from its current location to current location
        HashSet<String> produced = commandAction.getProduced();
        for(String item : produced) {
            if(item.equalsIgnoreCase("health")) {
                gamePlayer.incrementHealth();
            }
            // If it is not in storeroom then the item is location
            else if(gameLocations.keySet().stream()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet()).contains(item.toLowerCase()) && !item.equalsIgnoreCase("storeroom")) {
                // Add new path from current to said path
                this.createPathBetween(gameLocation.getLocationName(),item);
            }
            // see if produced item is entity and present anywhere in the game. If present move to player's current location.
            else {
                for(GameLocation location : gameLocations.values()) {
                    if(!location.getLocationName().equalsIgnoreCase(gameLocation.getLocationName())
                            && location.isEntityPresent(item)) {
                        gameLocation.addEntity(location.getEntity(item));
                        location.removeEntity(item);
                    }
                }
            }
        }
    }

    /**
     * Moves the consumed entities from its current location to storeroom
     * @param commandAction Action to be performed
     * @param gamePlayer Player performing the action
     * @param gameLocation Current location of the player
     */
    private void consumeEntity( GameAction commandAction, GamePlayer gamePlayer, GameLocation gameLocation) {
        HashSet<String> consumed = commandAction.getConsumed();
        for(String item : consumed) {
            // If health is consumed
            if(item.equalsIgnoreCase("health")) {
                consumePlayerHealth(gamePlayer, gameLocation);
            }
            // see if the item is present in player's inventory
            else if(gamePlayer.isArtefactPresent(item)) {
                gameLocations.get("storeroom").addArtefact(gamePlayer.getArtefact(item));
                gamePlayer.removeArtefact(item);
            }
            // else if consumed entity is a location
            else if (gameLocations.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet()).contains(item.toLowerCase())
                    && !item.equalsIgnoreCase("storeroom")) {
                this.removePathBetween(gameLocation.getLocationName(),item);
            }
            // else if consumed item is entity in a location in map
            else {
                for(GameLocation location : gameLocations.values()) {
                    if(!location.getLocationName().equalsIgnoreCase("storeroom")
                            && location.isEntityPresent(item)){
                        gameLocations.get("storeroom").addEntity(location.getEntity(item));
                        location.removeEntity(item);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Decrements player's health and respawns it if health has become 0
     * @param gamePlayer Player performing the action
     * @param gameLocation Current location of the player
     */
    private void consumePlayerHealth(GamePlayer gamePlayer, GameLocation gameLocation) {
        gamePlayer.decrementHealth();
        if(gamePlayer.getHealth() == 0){
            // drop everything
            gameLocation.getArtefacts().addAll(gamePlayer.getArtefacts());
            gameLocation.removePlayer(gamePlayer.getName());
            gamePlayer.removeAllArtefacts();
            //respawn
            gamePlayer.checkHealthAndRespawn(playersStartLocation.toString());
            GameLocation startLocation = gameLocations.get(playersStartLocation.toString());
            if(startLocation != null) {
                startLocation.addPlayer(gamePlayer.getName());
            }
            StringBuilder message = new StringBuilder();
            message.append(gamePlayer.getName());
            message.append(" died because of this action and has respawned at the start location");
        }
    }

    private String printGameState(){
        // Iterate through all the locations and print their contents
        StringBuilder message = new StringBuilder();
        for(GameLocation location : gameLocations.values()) {
            message.append(location.getLocationName()).append(": ");

            if(!location.getArtefacts().isEmpty()) {
                message.append("[artefacts]: ");
                for (GameArtefact artefact : location.getArtefacts()) {
                    message.append(artefact.getName()).append(",");
                }
            }

            if(!location.getCharacters().isEmpty()) {
                message.append("[characters]: ");
                for (GameCharacter character : location.getCharacters()) {
                    message.append(character.getName()).append(",");
                }
            }

            if(!location.getFurnitures().isEmpty()) {
                message.append("[furniture]: ");
                for (GameFurniture furniture : location.getFurnitures()) {
                    message.append(furniture.getName()).append(",");
                }
            }

            if(!location.getPlayers().isEmpty()) {
                message.append("[players]: ");
                for (String player : location.getPlayers()) {
                    message.append(player).append(",");
                }
            }
            message.append(System.lineSeparator());
        }
        message.append("Paths:").append(System.lineSeparator());
        for(String source : gamePaths.keySet()) {
            for(String destination : gamePaths.get(source)){
                message.append(source).append("-->").append(destination).append(System.lineSeparator());
            }
        }

        return message.toString();
    }
}
