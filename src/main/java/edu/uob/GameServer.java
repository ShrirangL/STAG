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
        entitiesFilePath.append("config").append(File.separator).append("basic-entities.dot");
        actionsFilePath.append("config").append(File.separator).append("basic-actions.xml");

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
            String locationName = locationDetails.getId().getId().toLowerCase();
            String locationDescription = locationDetails.getAttribute("description").toLowerCase();

            if(first){
                playersStartLocation = locationName;
                first = false;
            }

            GameLocation gameLocation = new GameLocation(locationName, locationDescription);

            Iterator<Graph> entities = location.getSubgraphs().iterator();
            // Iterate through all the entity categories inside a location
            while (entities.hasNext()) {
                Graph entity = entities.next();
                String graphName = entity.getId().getId().toLowerCase();
                Iterator<Node> items = entity.getNodes(false).iterator();
                while (items.hasNext()) {
                    Node item = items.next();
                    String itemName = item.getId().getId().toLowerCase();
                    String itemDescription = item.getAttribute("description").toLowerCase();
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
        if(!gameLocations.containsKey("storeroom")){
            gameLocations.put("storeroom", new GameLocation("storeroom", "storage for any entities not placed in the game"));
        }

        Iterator<Edge>paths = sections.next().getEdges().iterator();
        //Iterate through all the paths
        while (paths.hasNext()) {
            Edge path = paths.next();
            Node fromLocation = path.getSource().getNode();
            String fromName = fromLocation.getId().getId().toLowerCase();
            Node toLocation = path.getTarget().getNode();
            String toName = toLocation.getId().getId().toLowerCase();
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
                    gameAction.addTrigger(keyphrases.item(k).getTextContent().toLowerCase());
                }
            }

            // Add subjects
            NodeList subjects = action.getElementsByTagName("subjects");
            for (int j = 0; j < subjects.getLength(); j++) {
                NodeList subjectEntities = ((Element)subjects.item(j)).getElementsByTagName("entity");
                for(int k = 0; k < subjectEntities.getLength(); k++) {
                    gameAction.addSubject(subjectEntities.item(k).getTextContent().toLowerCase());
                }
            }

            // Add consumed
            NodeList consumed = action.getElementsByTagName("consumed");
            for (int j = 0; j < consumed.getLength(); j++) {
                NodeList consumedEntities = ((Element)consumed.item(j)).getElementsByTagName("entity");
                for(int k = 0; k < consumedEntities.getLength(); k++) {
                    gameAction.addConsumed(consumedEntities.item(k).getTextContent().toLowerCase());
                }
            }

            // Add produced
            NodeList produced = action.getElementsByTagName("produced");
            for (int j = 0; j < produced.getLength(); j++) {
                NodeList producedEntities = ((Element)produced.item(j)).getElementsByTagName("entity");
                for(int k = 0; k < producedEntities.getLength(); k++) {
                    gameAction.addProduced(producedEntities.item(k).getTextContent().toLowerCase());
                }
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
        command = command.toLowerCase();
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

            if(!gamePlayers.containsKey(name)) {
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
        StringBuilder command = new StringBuilder(input.toLowerCase());

        // Keep iterating through the input string and check if a trigger or subject has matches in the input
        LinkedList<String> words = new LinkedList<String>();

        HashSet<String> triggersAndSubjects = new HashSet<>();
        triggersAndSubjects.addAll(availableTriggers);
        triggersAndSubjects.addAll(availableSubjects);
        for(String triggerOrSubject : triggersAndSubjects){
            StringBuilder patternString = new StringBuilder();
            patternString.append("\\b").append(triggerOrSubject).append("\\b");

            Pattern pattern = Pattern.compile(patternString.toString(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(command);

            while (matcher.find()) {
                words.add(triggerOrSubject);
                command.delete(matcher.start(), matcher.end());
                matcher = pattern.matcher(command);
            }
        }

        if (words.isEmpty()) {
            throw new RuntimeException("No action found");
        }

        // Only words which are either triggers or subjects or locations should remain
        Iterator<String> iterator = words.iterator();
        while (iterator.hasNext()) {
            String word = iterator.next();
            if (!(availableTriggers.contains(word) || availableSubjects.contains(word) || gameLocations.containsKey(word))) {
                iterator.remove();
            }
        }

        //If we have no trigger and subjects we can't do anything
        if (words.isEmpty()) {
            throw new RuntimeException("Ensure that there is valid trigger and subject in ");
        }

        String trigger = this.doesCommmandContainTrigger(words);

        switch (trigger) {
            case "inv":
            case "inventory":
                return this.parseActionInventory(gamePlayer, words);
            case "get":
                return this.parseActionGet(gamePlayer, words);
            case "drop":
                return this.parseActionDrop(gamePlayer, words);
            case "goto":
                return this.parseActionGoto(gamePlayer, words);
            case "look":
                return this.parseActionLook(gamePlayer, words);
            case "health":
                return this.parseActionHealth(gamePlayer, words);
            default:
                return this.parseComplexAction(gamePlayer, words);
        }
    }

    private String doesCommmandContainTrigger(LinkedList<String> words) {
        for(String word : words) {
            if(availableTriggers.contains(word)) {
                return word;
            }
        }
        return "";
    }

    private String doesCommandContainSubject(LinkedList<String> words) {
        for(String word : words) {
            if(availableSubjects.contains(word)) {
                return word;
            }
        }
        return "";
    }

    private String doesCommandContainLocation(LinkedList<String> words) {
        for (String word : words ) {
            if(gameLocations.containsKey(word)) {
                return word;
            }
        }
        return "";
    }

    private boolean doesCommandContainTriggerExcept(LinkedList<String> words, String... exceptions) {
        //copy available command and remove the exception
        HashSet<String> copyOfavailableTriggers = new HashSet<>(availableTriggers);
        for(String exception : exceptions) {
            copyOfavailableTriggers.remove(exception);
        }
        for(String word : words) {
            if(copyOfavailableTriggers.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean doesCommandContainSubjectsExcept(LinkedList<String> words, String... exceptions) {
        HashSet<String> copyOfAvailableSubjects = new HashSet<>(availableSubjects);
        for(String exception : exceptions) {
            copyOfAvailableSubjects.remove(exception);
        }
        for(String word : words) {
            if(copyOfAvailableSubjects.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean doesCommandContainLocationExcept(LinkedList<String> words, String... exceptions) {
        HashSet<String> copyOfAvailableLocations = new HashSet<>(gameLocations.keySet());
        for(String exception : exceptions) {
            copyOfAvailableLocations.remove(exception);
        }
        for(String word : words) {
            if(copyOfAvailableLocations.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String parseActionInventory(GamePlayer gameplayer, LinkedList<String> words) {
        //  for inventory, we only need word inventory no extra built-in
        //  trigger or subjects should be present

        if(!this.doesCommandContainSubject(words).isEmpty()) {
            throw new RuntimeException("Subject not allowed in command");
        }
        if(this.doesCommandContainTriggerExcept(words, "inv", "inventory")) {
            throw new RuntimeException("Multiple triggers now allowed in command");
        }
        return gameplayer.showInventory();
    }

    private String parseActionGet(GamePlayer gamePlayer, LinkedList<String> words) {
        if(this.doesCommandContainTriggerExcept(words, "get")) {
            throw new RuntimeException("Multiple triggers not allowed in get command");
        }

        String subject = this.doesCommandContainSubject(words);
        if(subject.isEmpty()) {
            throw new RuntimeException("Get command requires a subjects");
        }

        if(this.doesCommandContainSubjectsExcept(words, subject)) {
            throw new RuntimeException("Multiple subjects not allowed in get command");
        }
        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());

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

    private String parseActionDrop(GamePlayer gamePlayer, LinkedList<String> words) {
        if(this.doesCommandContainTriggerExcept(words, "drop")) {
            throw new RuntimeException("Multiple triggers not allowed in drop command");
        }

        String subject = this.doesCommandContainSubject(words);
        if(subject.isEmpty()) {
            throw new RuntimeException("drop command requires a subjects");
        }
        if(this.doesCommandContainSubjectsExcept(words, subject)) {
            throw new RuntimeException("Multiple subjects not allowed in drop command");
        }
        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());

        // the subject should be artefact and should be present in the current location of the player
        if(!gamePlayer.isArtefactPresent(subject)) {
            throw new RuntimeException("Artefact could not be found in player's inventory");
        }
        gameLocation.addArtefact(gamePlayer.getArtefact(subject));
        gamePlayer.removeArtefact(subject);

        StringBuilder ret = new StringBuilder();
        ret.append(gamePlayer.getName()).append(" dropped ").append(subject);
        return ret.toString();
    }

    private String parseActionGoto(GamePlayer gamePlayer, LinkedList<String> words) {
        if(this.doesCommandContainTriggerExcept(words, "goto")) {
            throw new RuntimeException("Multiple triggers not allowed in goto command");
        }

        String subject = this.doesCommandContainSubject(words);
        if(subject.isEmpty()) {
            throw new RuntimeException("Goto command requires a subjects");
        }
        if(this.doesCommandContainSubjectsExcept(words, subject)) {
            throw new RuntimeException("Multiple subjects not allowed in goto command");
        }

        String newLocation = this.doesCommandContainLocation(words);
        if(newLocation.isEmpty()) {
            throw new RuntimeException("Goto command requires a destination location");
        }
        if(this.doesCommandContainLocationExcept(words, newLocation)) {
            throw new RuntimeException("Multiple locations not allowed in goto command");
        }

        if(!newLocation.equalsIgnoreCase(gamePlayer.getLocation())) {
            // see if there is a path from current location to the destination
            if (!gamePaths.get(gamePlayer.getLocation()).contains(newLocation)) {
                throw new RuntimeException("Location is not accessible from current location of the player");
            }

            GameLocation oldLocation = gameLocations.get(gamePlayer.getLocation());
            oldLocation.removePlayer(gamePlayer.getName());
            gamePlayer.setLocation(newLocation);
        }

        return this.getPlayerPerspective(gamePlayer);
    }

    private String parseActionLook(GamePlayer gamePlayer, LinkedList<String> words) {
        // no other trigger, no subjects and no location is required
        if(this.doesCommandContainTriggerExcept(words, "look")) {
            throw new RuntimeException("Multiple triggers not allowed in look command");
        }
        if(!this.doesCommandContainSubject(words).isEmpty()) {
            throw new RuntimeException("Look command requires does not require subjects");
        }
        if(!this.doesCommandContainLocation(words).isEmpty()) {
            throw new RuntimeException("Look command requires does not require location");
        }
        return this.getPlayerPerspective(gamePlayer);
    }

    private String parseActionHealth(GamePlayer gamePlayer, LinkedList<String> words) {
        // no other trigger, no subjects and no location is required
        if(this.doesCommandContainTriggerExcept(words, "health")) {
            throw new RuntimeException("Multiple triggers not allowed in health command");
        }
        if(!doesCommandContainSubject(words).isEmpty()) {
            throw new RuntimeException("Health command requires does not require subjects");
        }
        if(!doesCommandContainLocation(words).isEmpty()) {
            throw new RuntimeException("Health command requires does not require location");
        }
        StringBuilder ret = new StringBuilder();
        ret.append(gamePlayer.getName()).append("'s health is: ").append(gamePlayer.getHealth()).append("\n");
        return ret.toString();
    }

    private String getPlayerPerspective(GamePlayer gamePlayer) {
        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("You are in ").append(gameLocation.getLocationDescription()).append(":\n");
        stringBuilder.append("You can see:").append("\n");
        //iterate through characters, artefacts, furniture
        for (GameCharacter character : gameLocation.getCharacters()) {
            stringBuilder.append(character.getDescription()).append("\n");
        }
        for (GameArtefact gameArtefact : gameLocation.getArtefacts()) {
            stringBuilder.append(gameArtefact.getDescription()).append("\n");
        }
        for (GameFurniture furniture : gameLocation.getFurnitures()) {
            stringBuilder.append(furniture.getDescription()).append("\n");
        }
        for (String player : gameLocation.getPlayers()) {
            if(!gamePlayer.getName().equalsIgnoreCase(player)) {
                stringBuilder.append("Player :").append(player).append("\n");
            }
        }
        stringBuilder.append("You can access from here:").append("\n");
        HashSet<String> gPaths = gamePaths.get(gamePlayer.getLocation());
        for (String path : gPaths) {
            stringBuilder.append(path).append("\n");
        }
        return stringBuilder.toString();
    }

    private String parseComplexAction(GamePlayer gamePlayer, LinkedList<String> wordList) {
        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());
        HashSet<String> commandTriggers = new HashSet<>();

        for(String word : wordList) {
            if(availableTriggers.contains(word)) {
                commandTriggers.add(word);
            }
        }

        GameAction commandAction  = null; // Action to be performed
        // if command triggers is empty throw error
        if(commandTriggers.isEmpty()) {
            throw new RuntimeException("No command triggers in action ");
        }

        HashSet<HashSet<String>> subjectsSet = new HashSet<HashSet<String>>();
        HashSet<GameAction> actions = new HashSet<>();
        for(String commandTrigger : commandTriggers) {
            actions.addAll(gameActions.get(commandTriggers.iterator().next()));
        }

        //keep iterating through all the actions and when a subject from an action is found
        // there should be no other subjects from a different action
        boolean first = true;
        for(GameAction action : actions) {
            // compile a list of subjects for this action
            HashSet<String> foundSubjects = new HashSet<>();
            Iterator<String> itr = action.getSubjects().iterator();
            while(itr.hasNext()) {
                String subject = itr.next();
                if(wordList.contains(subject)) {
                    foundSubjects.add(subject);
                }
            }
            if(foundSubjects.size() > 1){
                subjectsSet.add(foundSubjects);
                if(first){
                    commandAction = action;
                    first = false;
                }
            }
        }

        // We should have only one entry in subjects for a query to be valid else throw error
        if(subjectsSet.size() != 1) {
            throw new RuntimeException("All triggers the subjects should be from exactly one action");
        }

        //Ensure that the command does not contain subjects other than the ones required to perform this action
        HashSet<String> subjects = new HashSet<>(commandAction.getSubjects());
        for(String word : wordList){
            if(availableSubjects.contains(word) && !subjects.contains(word)) {
                throw new RuntimeException("All the subjects should be from exactly one action");
            }
        }

        //See if we can act on valid query
        //We should now check if all the subject related to that trigger is either possessed by player or in the room.
        // If even a single subject is unavailable throw exception.
        // compile list of all the subjects available to the player
        HashSet<String> availableEntities = new HashSet<>();
        availableEntities.addAll(gamePlayer.getArtefactNames()); // player's inventory
        availableEntities.addAll(gameLocation.getEntityNames()); // Entities at current location
        availableEntities.addAll(gamePaths.get(gamePlayer.getLocation())); // Current location

        for(String subject: commandAction.getSubjects()) {
            if(!availableEntities.contains(subject)) {
                throw new RuntimeException("Subject(s) required to execute action are missing");
            }
        }

        //Act on the query/command
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
                }
            }
            // see if the item is present in player's inventory
            else if(gamePlayer.isArtefactPresent(item)) {
                gameLocations.get("storeroom").addArtefact(gamePlayer.getArtefact(item));
                gamePlayer.removeArtefact(item);
            }
            // else if consumed entity is a location
            else if (gameLocations.containsKey(item) && !item.equalsIgnoreCase("storeroom")) {
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

        // produced item is moved from store room to current location
        HashSet<String> produced = commandAction.getProduced();
        for(String item : produced) {
            if(item.equalsIgnoreCase("health")) {
                gamePlayer.incrementHealth();
            }
            // If it is not in storeroom then the item is location
            else if(gameLocations.containsKey(item) && !item.equalsIgnoreCase("storeroom")) {
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

        //Print the narration of the action
        return commandAction.getNarration();
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
            System.out.println(message.toString());
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
