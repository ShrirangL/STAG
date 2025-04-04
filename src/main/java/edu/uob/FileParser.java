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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Collectors;

// singleton class
public class FileParser {
    private static FileParser instance;
    private FileParser() {}

    public static FileParser getInstance() {
        if (instance == null) {
            instance = new FileParser();
        }
        return instance;
    }

    /**
     * Parses entities file to retrieve all the entities from all the locations
     * Retrieves one way paths between locations
     * @param entitiesFile Input file reference
     * @param gameLocations Map of location objects to be populated
     * @param gamePaths Map of one way paths to be populated
     * @param playersStartLocation Start location of new and respawned player
     */
    public void parseEntitiesFile(File entitiesFile, HashMap<String, GameLocation> gameLocations, HashMap<String, HashSet<String>> gamePaths, StringBuilder playersStartLocation) throws IOException, ParseException {
        Parser parser = new Parser();
        FileReader reader = new FileReader(entitiesFile.getAbsolutePath());
        parser.parse(reader);
        Graph wholeDocument = parser.getGraphs().get(0);
        Iterator<Graph> sections = wholeDocument.getSubgraphs().iterator();
        Iterator<Graph> locations = sections.next().getSubgraphs().iterator();
        //Iterate through all the locations
        boolean first = true;
        while (locations.hasNext()) {
            Graph location = locations.next();
            Node locationDetails = location.getNodes(false).get(0);
            String locationName = locationDetails.getId().getId();
            String locationDescription = locationDetails.getAttribute("description");
            if(first){
                playersStartLocation.setLength(0);
                playersStartLocation.append(locationName);
                first = false;
            }
            GameLocation gameLocation = new GameLocation(locationName, locationDescription);
            this.parseEntitiesOfLocation(location, gameLocation, locationName);
            gameLocations.put(locationName, gameLocation);
        }
        this.ensureStoreroomExists(gameLocations);
        this.parseGamePaths(sections.next().getEdges().iterator(), gamePaths);
    }

    /**
     * Retrieves entities from entities file for a specific location and adds them to input game location.
     * @param location Graph to read the file
     * @param gameLocation Game location object to hold the retrieved data
     * @param locationName Name of the location to be provided to newly created entities to set their location
     */
    private void parseEntitiesOfLocation(Graph location, GameLocation gameLocation, String locationName){
        // Iterate through all the entity categories inside a location
        if(location.getSubgraphs() != null && !location.getSubgraphs().isEmpty()) {
            for (Graph entity : location.getSubgraphs()) {
                String graphName = entity.getId().getId();
                if(entity.getNodes(false) != null && !entity.getNodes(false).isEmpty()) {
                    for (Node item : entity.getNodes(false)) {
                        String itemName = item.getId().getId();
                        String itemDescription = item.getAttribute("description");
                        switch (graphName) {
                            case "characters":
                                gameLocation.addCharacter(new GameCharacter(itemName, itemDescription, locationName));
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
            }
        }
    }

    /**
     * Retrieves the paths from entities file and stores it in HashMap gamePaths
     * @param paths Iterator to iterate over all the paths
     * @param gamePaths HashMap to be populated
     */
    private void parseGamePaths(Iterator<Edge>paths, HashMap<String, HashSet<String>> gamePaths){
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

    /**
     * Creates a location named storeroom if it does not already exist in set of game locations
     * @param gameLocations Map of game locations to be populated
     */
    private void ensureStoreroomExists(HashMap<String, GameLocation> gameLocations) {
        // check if storeroom was found, if not create an empty location named storeroom
        if(!gameLocations.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet()).contains("storeroom")){
            gameLocations.put("storeroom", new GameLocation("storeroom", "storage for any entities not placed in the game"));
        }
    }

    /**
     * Parses actions file provided to the GameServer
     * @param actionsFile Name of action file
     * @param gameActions Set of actions to be filled
     */
    public void parseActionsFile(File actionsFile, HashMap<String, HashSet<GameAction>> gameActions) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(actionsFile.getAbsolutePath());
        Element root = document.getDocumentElement();
        NodeList actions = root.getChildNodes();
        if(actions.getLength() > 0) {
            for (int i = 1; i < actions.getLength(); i += 2) {
                GameAction gameAction = new GameAction();
                Element action = (Element) actions.item(i);
                // Add triggers
                this.parseActionTriggers(gameAction, action.getElementsByTagName("triggers"));
                // Add subjects
                this.parseActionSubjects(gameAction, action.getElementsByTagName("subjects"));
                // Add consumed
                this.parseConsumedEntities(gameAction, action.getElementsByTagName("consumed"));
                // Add produced
                this.parseProducedEntities(gameAction, action.getElementsByTagName("produced"));
                //Get action narration
                this.parseNarration(gameAction, action.getElementsByTagName("narration"));
                // Add game action to list of game actions
                this.addActionToGameActions(gameActions, gameAction);
            }
        }
    }

    /**
     * Adds trigger words to the newly created action
     * @param gameAction Newly created action
     * @param triggers Triggers from actions file
     */
    private void parseActionTriggers(GameAction gameAction, NodeList triggers){
        if(triggers!= null && triggers.getLength() > 0) {
            for (int j = 0; j < triggers.getLength(); j++) {
                NodeList keyphrases = ((Element) triggers.item(j)).getElementsByTagName("keyphrase");
                if(keyphrases.getLength() > 0) {
                    for (int k = 0; k < keyphrases.getLength(); k++) {
                        gameAction.addTrigger(keyphrases.item(k).getTextContent());
                    }
                }
            }
        }
    }

    /**
     * Adds subject entities to the newly created action
     * @param gameAction Newly created action
     * @param subjects Subject from actions file
     */
    private void parseActionSubjects(GameAction gameAction, NodeList subjects){
        if(subjects != null && subjects.getLength() > 0) {
            for (int j = 0; j < subjects.getLength(); j++) {
                NodeList subjectEntities = ((Element)subjects.item(j)).getElementsByTagName("entity");
                if(subjectEntities.getLength() > 0) {
                    for (int k = 0; k < subjectEntities.getLength(); k++) {
                        gameAction.addSubject(subjectEntities.item(k).getTextContent());
                    }
                }
            }
        }
    }

    /**
     * Adds produced entities to the newly created action
     * @param gameAction Newly created action
     * @param consumed Consumed item from actions file
     */
    private void parseConsumedEntities(GameAction gameAction, NodeList consumed){
        if(consumed != null && consumed.getLength() > 0) {
            for (int j = 0; j < consumed.getLength(); j++) {
                NodeList consumedEntities = ((Element)consumed.item(j)).getElementsByTagName("entity");
                if(consumedEntities.getLength() > 0) {
                    for (int k = 0; k < consumedEntities.getLength(); k++) {
                        gameAction.addConsumed(consumedEntities.item(k).getTextContent());
                    }
                }
            }
        }
    }

    /**
     * Adds produced entities to the newly created action
     * @param gameAction Newly created action
     * @param produced Produced item from actions file
     */
    private void parseProducedEntities(GameAction gameAction, NodeList produced){
        if(produced != null && produced.getLength() > 0) {
            for (int j = 0; j < produced.getLength(); j++) {
                NodeList producedEntities = ((Element)produced.item(j)).getElementsByTagName("entity");
                if(producedEntities.getLength() > 0) {
                    for (int k = 0; k < producedEntities.getLength(); k++) {
                        gameAction.addProduced(producedEntities.item(k).getTextContent());
                    }
                }
            }
        }
    }

    /**
     * Adds action narration from action.xml file to newly created action object
     * @param gameAction Newly created action
     * @param narrations narration retrieved from the actions file
     */
    private void parseNarration(GameAction gameAction, NodeList narrations){
        if (narrations != null && narrations.getLength() > 0) {
            gameAction.setNarration(narrations.item(0).getTextContent());
        }
    }

    /**
     * Adds the finalised game action to list of game actions in the memory
     * @param gameActions Set of unique actions
     * @param gameAction Newly created action
     */
    private void addActionToGameActions(HashMap<String, HashSet<GameAction>> gameActions, GameAction gameAction){
        if(gameAction.getTriggers() != null && !gameAction.getTriggers().isEmpty()) {
            for (String trigger : gameAction.getTriggers()) {
                gameActions.putIfAbsent(trigger, new HashSet<>());
                gameActions.get(trigger).add(gameAction);
            }
        }
    }
}


