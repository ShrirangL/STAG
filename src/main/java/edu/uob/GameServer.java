package edu.uob;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class GameServer {

    private HashMap<String, HashSet<GameAction>> gameActions;
    private HashMap<String, HashSet<String>> gamePaths;
    private HashMap<String, GamePlayer> gamePlayers;
    private HashMap<String, GameLocation> gameLocations;
    private String playersStartLocation = "";

    private final HashSet<String> builtInTriggers = new HashSet<>(Set.of("inventory", "inv", "get", "drop", "goto", "look", "health"));
    private HashSet<String> availableTriggers;
    private HashSet<GameAction> availableActions;
    private HashSet<String> availableSubjects;

    // compute a list of action triggers
    private void computeAvailableTriggers() {
        availableTriggers.addAll(builtInTriggers);
        availableTriggers.addAll(gameActions.keySet());
    }

    // compute a list of all game actions
    private void computeAvailableActions() {
        for(String key : gameActions.keySet()) {
            availableActions.addAll(gameActions.get(key));
        }
    }

    // make a list of all the subjects
    private void computeAvailableSubjects(){
        // action can be performed on artefacts, characters, furniture and location
        for (GameLocation location : gameLocations.values()) {
            availableSubjects.add(location.getLocationName());
            availableSubjects.addAll(location.getArtefactNames());
            availableSubjects.addAll(location.getCharacterNames());
            availableSubjects.addAll(location.getFurnitureNames());
        }
    }

    private static final char END_OF_TRANSMISSION = 4;

    public static void main(String[] args) throws IOException {
        StringBuilder entitiesFilePath = new StringBuilder();
        StringBuilder actionsFilePath = new StringBuilder();
        entitiesFilePath.append("config").append(File.separator).append("extended-entities.dot");
        actionsFilePath.append("config").append(File.separator).append("extended-actions.xml");

        File entitiesFile = Paths.get(entitiesFilePath.toString()).toAbsolutePath().toFile();
        File actionsFile = Paths.get(actionsFilePath.toString()).toAbsolutePath().toFile();
        GameServer server = new GameServer(entitiesFile, actionsFile);
        server.blockingListenOn(8888);
    }

    /**
     * Do not change the following method signature or we won't be able to mark your submission
     * Instanciates a new server instance, specifying a game with some configuration files
     *
     * @param entitiesFile The game configuration file containing all game entities to use in your game
     * @param actionsFile The game configuration file containing all game actions to use in your game
     */
    public GameServer(File entitiesFile, File actionsFile) {
        // TODO implement your server logic here
        gameActions = new HashMap<>();
        gamePaths = new HashMap<>();
        gamePlayers = new HashMap<>();
        gameLocations = new HashMap<>();

        availableTriggers = new HashSet<>();
        availableActions = new HashSet<>();
        availableSubjects = new HashSet<>();
        // read entities file
        try {
            this.parseEntitiesFile(entitiesFile);
            this.parseActionsFile(actionsFile);
            this.computeAvailableTriggers();
            this.computeAvailableActions();
            this.computeAvailableSubjects();
        }
        catch (Exception e) {
            e.getStackTrace();
        }
    }

    private void parseEntitiesFile(File entitiesFile) throws IOException, ParseException {
        Parser parser = new Parser();
        FileReader reader = new FileReader(entitiesFile.getAbsolutePath());
        parser.parse(reader);
        Graph wholeDocument = parser.getGraphs().get(0);

        Iterator<Graph> sections = wholeDocument.getSubgraphs().iterator();

        Iterator<Graph>locations = sections.next().getSubgraphs().iterator();
        //Iterate through all the locations
        boolean first = true;
        while (locations.hasNext()) {
            Graph location = locations.next();
            Node locationDetails = location.getNodes(false).get(0);
            String locationName = locationDetails.getId().getId();
            String locationDescription = locationDetails.getAttribute("description");

            if(first){
                playersStartLocation = locationName;
                first = false;
            }

            GameLocation gameLocation = new GameLocation(locationName, locationDescription);

            Iterator<Graph> entities = location.getSubgraphs().iterator();
            // Iterate through all the entity categories inside a location
            while (entities.hasNext()) {
                Graph entity = entities.next();
                String graphName = entity.getId().getId();
                Iterator<Node> items = entity.getNodes(false).iterator();
                while (items.hasNext()) {
                    Node item = items.next();
                    String itemName = item.getId().getId();
                    String itemDescription = item.getAttribute("description");
                    switch (graphName){
                        case "characters":
                            gameLocation.addCharacter(new GameCharacter(itemName, itemDescription, locationName, -1));
                            break;
                        case "artefacts":
                            gameLocation.addArtefact(new GameArtefact(itemName, itemDescription, locationName));
                            break;
                        case "furniture":
                            gameLocation.addFurniture(new GameFurniture(itemName, itemDescription, locationName));
                            break;
                    }
                }
            }
            gameLocations.put(locationName, gameLocation);
        }

        // check if storeroom was found, if not create an empty location named storeroom
        if(!gameLocations.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet()).contains("storeroom")){
            gameLocations.put("storeroom", new GameLocation("storeroom", "storage for any entities not placed in the game"));
        }

        Iterator<Edge>paths = sections.next().getEdges().iterator();
        //Iterate through all the paths
        while (paths.hasNext()) {
            Edge path = paths.next();
            Node fromLocation = path.getSource().getNode();
            String fromName = fromLocation.getId().getId();
            Node toLocation = path.getTarget().getNode();
            String toName = toLocation.getId().getId();
            gamePaths.putIfAbsent(fromName, new HashSet<>());
            gamePaths.get(fromName).add(toName);
        }
    }

    private void parseActionsFile(File actionsFile) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(actionsFile.getAbsolutePath());
        Element root = document.getDocumentElement();
        NodeList actions = root.getChildNodes();
        for(int i = 1; i < actions.getLength(); i+=2) {
            GameAction gameAction = new GameAction();
            Element action = (Element)actions.item(i);

            // Add triggers
            NodeList triggers = action.getElementsByTagName("triggers");
            for (int j = 0; j < triggers.getLength(); j++) {
                NodeList keyphrases = ((Element)triggers.item(j)).getElementsByTagName("keyphrase");
                for(int k = 0; k < keyphrases.getLength(); k++) {
                    gameAction.addTrigger(keyphrases.item(k).getTextContent());
                }
            }

            // Add subjects
            NodeList subjects = action.getElementsByTagName("subjects");
            for (int j = 0; j < subjects.getLength(); j++) {
                NodeList subjectEntities = ((Element)subjects.item(j)).getElementsByTagName("entity");
                for(int k = 0; k < subjectEntities.getLength(); k++) {
                    gameAction.addSubject(subjectEntities.item(k).getTextContent());
                }
            }

            // Add consumed
            NodeList consumed = action.getElementsByTagName("consumed");
            for (int j = 0; j < consumed.getLength(); j++) {
                NodeList consumedEntities = ((Element)consumed.item(j)).getElementsByTagName("entity");
                for(int k = 0; k < consumedEntities.getLength(); k++) {
                    gameAction.addConsumed(consumedEntities.item(k).getTextContent());
                }
            }

            // Add produced
            NodeList produced = action.getElementsByTagName("produced");
            for (int j = 0; j < produced.getLength(); j++) {
                NodeList producedEntities = ((Element)produced.item(j)).getElementsByTagName("entity");
                for(int k = 0; k < producedEntities.getLength(); k++) {
                    gameAction.addProduced(producedEntities.item(k).getTextContent());
                }
            }

            //Get action narration
            NodeList narrations = action.getElementsByTagName("narration");
            if (narrations.getLength() > 0) {
                gameAction.setNarration(narrations.item(0).getTextContent());
            }

            // Add game action to list of game actions
            for(String trigger : gameAction.getTriggers()) {
                gameActions.putIfAbsent(trigger, new HashSet<>());
                gameActions.get(trigger).add(gameAction);
            }
        }
    }

    /**
     * Do not change the following method signature or we won't be able to mark your submission
     * This method handles all incoming game commands and carries out the corresponding actions.</p>
     *
     * @param command The incoming command to be processed
     */
    public String handleCommand(String command) {
        // TODO implement your server logic here
        // Handle standard built in commands first;
        // find ":" in the incoming string. anything to its left is username
        try {
            Iterator<String> iterator = Arrays.stream(command.split(":")).iterator();
            String name = iterator.next().trim();
            String regex = "^[A-Za-z '-]+$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(name);
            if (!matcher.matches()) {
                throw new RuntimeException("Name must consist only of letters, spaces, apostrophes and hyphens");
            }

            if(!gamePlayers.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet()).contains(name.toLowerCase())) {
                gamePlayers.put(name, new GamePlayer(name, "", playersStartLocation));
                gameLocations.get(playersStartLocation).addPlayer(name);
            }

            String action = iterator.next();
            return this.parseAction(gamePlayers.get(name), action);
        }
        catch (Exception e){
            StringBuilder error = new StringBuilder();
            error.append("[ERROR]: ").append(e.getMessage());
            return error.toString();
        }
    }

    private String parseAction(GamePlayer gamePlayer, String input) {
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

        if (triggers.isEmpty()) {
            throw new RuntimeException("No action found");
        }

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

    private boolean isThisSubjectLocation(String subject) {
        for(String location : gameLocations.keySet()) {
            if(location.equalsIgnoreCase(subject)) {
                return true;
            }
        }
        return false;
    }

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

    private String parseActionInventory(GamePlayer gameplayer, HashSet<String> triggers, HashSet<String> subjects) {
        //  for inventory, we only need word inventory no extra built-in
        //  trigger or subjects should be present

        if(!subjects.isEmpty()) {
            throw new RuntimeException("Subject not allowed in command");
        }

        if(this.doesContainWordsExcept(triggers, new HashSet<String> (Set.of("inv", "inventory")))) {
            throw new RuntimeException("Multiple triggers now allowed in command");
        }
        return gameplayer.showInventory();
    }

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

    private String parseActionGoto(GamePlayer gamePlayer, HashSet<String> triggers, HashSet<String> subjects) {
        if(this.doesContainWordsExcept(triggers, new HashSet<String> (Set.of("goto")))) {
            throw new RuntimeException("Multiple triggers not allowed in goto command");
        }

        if(subjects.isEmpty()) {
            throw new RuntimeException("Goto command requires a subjects");
        } else if(subjects.size() > 1) {
            throw new RuntimeException("Multiple subjects not allowed in goto command");
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

    private String parseComplexAction(GamePlayer gamePlayer, HashSet<String> triggers, HashSet<String> subjects) {
        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());

        GameAction commandAction = this.isCommandValid(triggers, subjects);
        //See if we can act on valid query
        this.isActionPerformable(commandAction, gamePlayer, gameLocation);

        //Act on the query/command
        this.produceEntity(commandAction, gamePlayer, gameLocation);
        this.consumeEntity(commandAction, gamePlayer, gameLocation);

        //Print the narration of the action
        return commandAction.getNarration();
    }

    private GameAction isCommandValid(HashSet<String> triggers, HashSet<String> subjects) {
        GameAction commandAction  = null; // Action to be performed
        // if command triggers is empty throw error
        if(triggers.isEmpty()) {
            throw new RuntimeException("No command triggers in action ");
        }

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

    private void isActionPerformable(GameAction commandAction, GamePlayer gamePlayer, GameLocation gameLocation) {
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

    private void produceEntity(GameAction commandAction, GamePlayer gamePlayer, GameLocation gameLocation) {
        // produced item is moved from store room to current location
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
                gamePaths.putIfAbsent(gameLocation.getLocationName(), new HashSet<>());
                gamePaths.get(gameLocation.getLocationName()).add(item);
            }
            // see if produced item is present anywhere in the game. If present move to player's current location.
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

    private void consumeEntity( GameAction commandAction, GamePlayer gamePlayer, GameLocation gameLocation) {
        //Delete the consumed item from where it was located (either player's inventory or room)
        // and place it in storeroom.
        HashSet<String> consumed = commandAction.getConsumed();
        for(String item : consumed) {
            if(item.equalsIgnoreCase("health")) {
                gamePlayer.decrementHealth();
                if(gamePlayer.getHealth() == 0){
                    // drop everything
                    gameLocation.getArtefacts().addAll(gamePlayer.getArtefacts());
                    gameLocation.removePlayer(gamePlayer.getName());
                    gamePlayer.removeAllArtefacts();
                    //respawn
                    gamePlayer.checkHealthAndRespawn(playersStartLocation);
                    GameLocation startLocation = gameLocations.get(playersStartLocation);
                    if(startLocation != null) {
                        startLocation.addPlayer(gamePlayer.getName());
                    }
                    StringBuilder message = new StringBuilder();
                    message.append(gamePlayer.getName());
                    message.append(" died because of this action and has respawned at the start location");
                    throw new RuntimeException(message.toString());
                }
            }
            // see if the item is present in player's inventory
            else if(gamePlayer.isArtefactPresent(item)) {
                gameLocations.get("storeroom").addArtefact(gamePlayer.getArtefact(item));
                gamePlayer.removeArtefact(item);
            }
            // else if consumed entity is a location
            else if (gameLocations.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet()).contains(item.toLowerCase())
                    && !item.equalsIgnoreCase("storeroom")) {
                // get all the paths from current location
                // remove path from current location to this location
                Iterator<String> iterator = gamePaths.get(gamePlayer.getLocation()).iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().equalsIgnoreCase(item)) {
                        iterator.remove();
                        break;
                    }
                }
            }
            // or any game location and remove it from there and move it to storeroom
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

    /**
     * Do not change the following method signature, or we won't be able to mark your submission
     * Starts a *blocking* socket server listening for new connections.
     *
     * @param portNumber The port to listen on.
     * @throws IOException If any IO related operation fails.
     */
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            StringBuilder message = new StringBuilder();
            message.append("Server listening on port ").append(portNumber);
            System.out.println(message);
            while (!Thread.interrupted()) {
                try {
                    this.blockingHandleConnection(s);
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            }
        }
    }

    /**
     * Do not change the following method signature or we won't be able to mark your submission
     * Handles an incoming connection from the socket server.
     *
     * @param serverSocket The client socket to read/write from.
     * @throws IOException If any IO related operation fails.
     */
    private void blockingHandleConnection(ServerSocket serverSocket) throws IOException {
        try (Socket s = serverSocket.accept();
             BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()))) {
            System.out.println("Connection established");
            String incomingCommand = reader.readLine();
            if(incomingCommand != null) {
                StringBuilder message = new StringBuilder();
                message.append("Received message from ").append(incomingCommand);
                System.out.println(message);
                String result = this.handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n");
                writer.write(END_OF_TRANSMISSION);
                writer.write("\n");
                writer.flush();
            }
        }
    }
}
