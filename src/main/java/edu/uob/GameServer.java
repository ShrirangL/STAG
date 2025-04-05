package edu.uob;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameServer {
    // Map of actions in the game with trigger as key and set of action as value
    HashMap<String, HashSet<GameAction>> gameActions;

    //Paths between location in the game. paths are one-way
    private final HashMap<String, HashSet<String>> gamePaths;

    //Map of players in the game with player name being key
    private final HashMap<String, GamePlayer> gamePlayers;

    //Map of locations with name of location as key
    private final HashMap<String, GameLocation> gameLocations;

    //First location in the map is constant for all new and respawned player
    private final StringBuilder playersStartLocation;

    //Handles incoming user command and
    private final CommandHandler commandHandler;

    //Parses action and entities file
    private final FileParser fileParser = FileParser.getInstance();

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
     * Do not change the following method signature, or we won't be able to mark your submission
     * Instantiates a new server instance, specifying a game with some configuration files
     *
     * @param entitiesFile The game configuration file containing all game entities to use in your game
     * @param actionsFile The game configuration file containing all game actions to use in your game
     */
    public GameServer(File entitiesFile, File actionsFile) {
        gameActions = new HashMap<>();
        gamePaths = new HashMap<>();
        gamePlayers = new HashMap<>();
        gameLocations = new HashMap<>();
        playersStartLocation = new StringBuilder();
        // read entities file
        try {
            fileParser.parseEntitiesFile(entitiesFile, gameLocations, gamePaths, playersStartLocation);
            fileParser.parseActionsFile(actionsFile, gameActions);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        commandHandler = new CommandHandler(gameActions, gamePaths, gameLocations, gamePlayers, playersStartLocation.toString());
    }

    /**
     * Do not change the following method signature, or we won't be able to mark your submission
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
            String action = iterator.next();
            if(!gamePlayers.isEmpty()) {
                for (GamePlayer player : gamePlayers.values()) {
                    if (player.getName().equalsIgnoreCase(name)) {
                        return commandHandler.parseIncomingCommand(player, action);
                    }
                }
            }
            gamePlayers.put(name, new GamePlayer(name, "", playersStartLocation.toString()));
            gameLocations.get(playersStartLocation.toString()).addPlayer(name);
            return commandHandler.parseIncomingCommand(gamePlayers.get(name), action);
        }
        catch (Exception e){
            StringBuilder error = new StringBuilder();
            error.append("[ERROR]: ").append(e.getMessage());
            return error.toString();
        }
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
     * Do not change the following method signature, or we won't be able to mark your submission
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
