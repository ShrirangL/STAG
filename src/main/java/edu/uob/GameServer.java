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

    private static final char END_OF_TRANSMISSION = 4;

    public static void main(String[] args) throws IOException {
        File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();
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
        // read entities file
        try {
            this.parseEntitiesFile(entitiesFile);
            this.parseActionsFile(actionsFile);
        }
        catch (Exception e) {
            e.getStackTrace();
        }
        return;
    }

    private void parseEntitiesFile(File entitiesFile) throws IOException, ParseException {
        Parser parser = new Parser();
        FileReader reader = new FileReader(entitiesFile.getAbsolutePath());
        parser.parse(reader);
        Graph wholeDocument = parser.getGraphs().get(0);

        Iterator<Graph> sections = wholeDocument.getSubgraphs().iterator();

        Iterator<Graph>locations = sections.next().getSubgraphs().iterator();
        //Iterate through all the locations
        Boolean first = true;
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
        if(!gameLocations.containsKey("storeroom")){
            gameLocations.put("storeroom", new GameLocation("storeroom", "Storage for any entities not placed in the game"));
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
            String name = iterator.next();
            String regex = "^[A-Za-z '-]+$";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(name);
            if (!matcher.matches()) {
                throw new RuntimeException("Name '" + name + "' must consist only of letters, spaces, apostrophes and hyphens");
            }

            if(!gamePlayers.containsKey(name)) {
                gamePlayers.put(name, new GamePlayer(name, "", playersStartLocation));
            }

            String action = iterator.next();
            return this.parseAction(gamePlayers.get(name), action);
        }
        catch (Exception e){
            return e.getMessage();
        }
    }

    private String parseAction(GamePlayer gamePlayer, String action) {
        action = action.toLowerCase();
        LinkedList<String> words = new LinkedList<>(Arrays.stream(action.split(" ")).toList());
        if(words.isEmpty()) {
            throw new RuntimeException("No action found");
        }
        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());
        if(gameLocation == null) {
            throw new RuntimeException("Location " + gamePlayer.getLocation() + " not found");
        }
        HashSet<String> availableTriggers = new HashSet<>(Arrays.asList("inventory", "inv", "get", "drop", "goto", "look"));
        availableTriggers.addAll(gameActions.keySet());
        HashSet<String> foundTriggers = new HashSet<>();
        for(String trigger : availableTriggers) {
            if(words.contains(trigger)) {
                foundTriggers.add(trigger);
            }
        }

        //If we have more than one trigger throw exception
        if(foundTriggers.size() != 1) {
            throw new RuntimeException("Ensure that there is exactly one valid trigger");
        }

        HashSet<String> items = new HashSet<>();
        switch(foundTriggers.iterator().next()) {
            // first check if the incoming command contains the built-in actions
            case "inventory":
            case "inv":
                return gamePlayer.showInventory();
            case "get":
                // get all the words from action and check all the words except get in the current location.
                for (String word : words) {
                    // is the current word present in current location of player
                    if (!word.equalsIgnoreCase("get") && gameLocation.isArtefactPresent(word)) {
                        items.add(word);
                    }
                }

                // if more or less than one item is being picked throw error
                if (items.isEmpty()) {
                    throw new RuntimeException("Artefact in action get cannot be found or could not be picked up.");
                } else if (items.size() > 1) {
                    throw new RuntimeException("Multiple artefacts cannot be picked in a single action.");
                }

                // insert the item into player's inventory
                String item = items.iterator().next();
                gamePlayer.addItem(gameLocation.getArtefact(item));
                // Remove the item from the location
                gameLocation.removeArtefact(item);
                break;
            case "drop":
                // get all the words from action and check all the words except drop in the player's inventory
                for (String word : words) {
                    if (!word.equalsIgnoreCase("drop") && gamePlayer.isArtefactPresent(word)) {
                        items.add(word);
                    }
                }
                // if more or less than one item is being picked throw error
                if (items.isEmpty()) {
                    throw new RuntimeException("No items in action " + action);
                } else if (items.size() > 1) {
                    throw new RuntimeException("Too many items in action " + action);
                }

                // add  it in the current location
                String artefact = items.iterator().next();
                gameLocation.addArtefact(gamePlayer.getArtefact(artefact));
                // Remove the item from player
                gamePlayer.removeArtefact(artefact);
                break;

            case "goto":
                // check if there is a single valid destination
                HashSet<String> destinations = new HashSet<>();
                for (String word : words) {
                    if (!word.equalsIgnoreCase("goto") && gameLocations.containsKey(word)) {
                        destinations.add(word);
                    }
                }
                // if more or less than one destination throw error
                if (destinations.isEmpty()) {
                    throw new RuntimeException("No destination in action " + action);
                } else if (destinations.size() > 1) {
                    throw new RuntimeException("Too many destinations in action " + action);
                }

                String destination = destinations.iterator().next();

                // see if there is a path from current location to the destination
                // get all the paths from current location
                HashSet<String> paths = gamePaths.get(gamePlayer.getLocation());
                // see if destination is one of them
                for (String path : paths) {
                    if (path.equalsIgnoreCase(destination)) {
                        // change the player location to new location
                        gamePlayer.setLocation(destination);
                        return "";
                    }
                }
                throw new RuntimeException("The path from current location '" + gamePlayer.getLocation() + "' to '" + destination);
            case "look":
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("You are in ").append(gameLocation.getLocationDescription()).append("\n");
                stringBuilder.append("You can see:").append("\n");
                //iterate through characters, artefacts, furnitures
                for (GameCharacter character : gameLocation.getCharacters()) {
                    stringBuilder.append(character.getDescription()).append("\n");
                }
                for (GameArtefact gameArtefact : gameLocation.getArtefacts()) {
                    stringBuilder.append(gameArtefact.getDescription()).append("\n");
                }
                for (GameFurniture furniture : gameLocation.getFurnitures()) {
                    stringBuilder.append(furniture.getDescription()).append("\n");
                }
                stringBuilder.append("You can access from here:").append("\n");
                HashSet<String> gPaths = gamePaths.get(gamePlayer.getLocation());
                for (String path : gPaths) {
                    stringBuilder.append(path).append("\n");
                }
                return stringBuilder.toString();
            default:
                return this.parseComplexAction(gamePlayer, action);
        }
        return "";
    }

    private String parseComplexAction(GamePlayer gamePlayer, String command) {
        GameLocation gameLocation = gameLocations.get(gamePlayer.getLocation());
        LinkedList<String> wordList = new LinkedList<>(Arrays.stream(command.split(" ")).toList());

        //See if the query is valid
        //for a custom action we need a trigger and it least one of the subjects.
        // If we have more or less than one trigger we throw exception.
        //if we have zero or more than its subjects we throw error. A trigger can
        // be associated with more than one game action so If we have a trigger but
        // subjects are not from exactly one action we throw error.
        // In the end we should have one trigger and subjects should not be empty, and they should all be from one action.
        HashSet<String> availableTriggers = new HashSet<>(gameActions.keySet());
        HashSet<String> commandTriggers = new HashSet<>();
        HashSet<String> commandSubjects = new HashSet<>();

        for(String word : wordList) {
            if(availableTriggers.contains(word)) {
                commandTriggers.add(word);
            }
        }

        if(commandTriggers.size() != 1) {
            throw new RuntimeException("There should be exactly one trigger in the command");
        }

        String trigger = commandTriggers.iterator().next();

        // for all the actions associated with this command
        HashSet<GameAction>actions = gameActions.get(trigger);

        HashSet<HashSet<String>> subjects = new HashSet<HashSet<String>>();
        //keep iterating through all the actions and when a subject from an action is found
        // there should be no other subject from a different action
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
                subjects.add(foundSubjects);
            }
        }

        // We should have only one entry in subjects for a query to be valid else throw error
        if(subjects.size() != 1) {
            throw new RuntimeException("All the subjects should be from exactly one action");
        }

        commandSubjects = subjects.iterator().next();
        if(commandSubjects == null || commandSubjects.isEmpty()) {
            throw new RuntimeException("Valid subjects for trigger :'" + trigger + "' are missing.");
        }

        //See if we can act on valid query
        //We should now check if all the subject related to that trigger is either possessed by player or in the room.
        // If even a single subject is unavailable throw exception.
        // compile list of all the subjects available to the player
        HashSet<String> availableEntities = new HashSet<>();
        availableEntities.addAll(gamePlayer.getArtefactNames());
        availableEntities.addAll(gameLocation.getEntityNames());
        availableEntities.addAll(gameLocations.keySet());
        availableEntities.remove("storeroom");

        for(String subject: commandSubjects) {
            if(!availableEntities.contains(subject)) {
                throw new RuntimeException("Artefact :'" + subject + "' is not in available to player");
            }
        }

        //Act on the query/command
        //Delete the consumed item from where it was located (either player's inventory or room)
        GameAction commandAction = actions.iterator().next();
        HashSet<String> consumed = commandAction.getConsumed();
        HashSet<String> produced = commandAction.getProduced();
        for(String item : consumed) {
            // see if the item is present in player's inventory or current location and remove it from there
            if(gamePlayer.isArtefactPresent(item)) {
                gameLocations.get("storeroom").addArtefact(gamePlayer.getArtefact(item));
                gamePlayer.removeArtefact(item);
            }else if(gameLocation.isEntityPresent(item)) {
                gameLocations.get("storeroom").addEntity(gameLocation.getEntity(item));
                gameLocation.removeEntity(item);
            } else if (gameLocations.containsKey(item)) {
                // remove path from current location to this location
                // get all the paths from current location
                Iterator<String> iterator = gamePaths.get(gamePlayer.getLocation()).iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().equalsIgnoreCase(item)) {
                        iterator.remove();
                    }
                }
            }
        }

        // produced item is moved from store room to current location
        for(String item : produced) {
            // see if produced item is present in storeroom. If present move to player's current location.
            GameLocation storeRoom = gameLocations.get("storeroom");
            if(storeRoom.isEntityPresent(item)){
                gameLocation.addEntity(storeRoom.getEntity(item));
                storeRoom.removeEntity(item);
            }
            // If it is not in storeroom then the item is location
            else if(gameLocations.containsKey(item)){
                // Add new path from current to said path
                gamePaths.putIfAbsent(gameLocation.getLocationName(), new HashSet<>());
                gamePaths.get(gameLocation.getLocationName()).add(item);
            }

        }

        //Print the narration of the action
        return commandAction.getNarration();
    }

    /**
    * Do not change the following method signature or we won't be able to mark your submission
    * Starts a *blocking* socket server listening for new connections.
    *
    * @param portNumber The port to listen on.
    * @throws IOException If any IO related operation fails.
    */
    public void blockingListenOn(int portNumber) throws IOException {
        try (ServerSocket s = new ServerSocket(portNumber)) {
            System.out.println("Server listening on port " + portNumber);
            while (!Thread.interrupted()) {
                try {
                    blockingHandleConnection(s);
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
                System.out.println("Received message from " + incomingCommand);
                String result = handleCommand(incomingCommand);
                writer.write(result);
                writer.write("\n" + END_OF_TRANSMISSION + "\n");
                writer.flush();
            }
        }
    }
}
