package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameServerTests {

    private GameServer server;
    private String response;
    private String playerName = "santiago";

    // Create a new server _before_ every @Test
    @BeforeEach
    void setup() {
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
        server = new GameServer(entitiesFile, actionsFile);
    }

    @Test
    void basicGameFlow()
    {
        // Look
        response = server.handleCommand(playerName + ": look");
        System.out.println(response);
        assertTrue(response.contains("cabin"));
        assertTrue(response.contains("axe"));
        assertTrue(response.contains("forest"));
        // Get axe, coin and potion
        response = server.handleCommand(playerName + ": get axe");
        System.out.println(response);
        assertTrue(response.contains("axe"));
        response = server.handleCommand(playerName + ": get coin");
        System.out.println(response);
        assertTrue(response.contains("coin"));
        response = server.handleCommand(playerName + ": get potion");
        System.out.println(response);
        assertTrue(response.contains("potion"));
        response = server.handleCommand(playerName + ": look");
        System.out.println(response);
        assertFalse(response.contains("axe"));
        assertFalse(response.contains("coin"));
        assertFalse(response.contains("potion"));
        // Go to forest
        response = server.handleCommand(playerName + ": goto forest");
        System.out.println(response);
        assertTrue(response.contains("forest"));
        response = server.handleCommand(playerName + ": look");
        System.out.println(response);
        assertTrue(response.contains("forest"));
        // Get key
        response = server.handleCommand(playerName + ": get key");
        System.out.println(response);
        assertTrue(response.contains("key"));
        response = server.handleCommand(playerName + ": look");
        System.out.println(response);
        assertFalse(response.contains("key"));
        // Go to cabin
        response = server.handleCommand(playerName + ": goto cabin");
        System.out.println(response);
        assertTrue(response.contains("cabin"));
        response = server.handleCommand(playerName + ": look");
        System.out.println(response);
        assertTrue(response.contains("cabin"));
        // Open trapdoor
        response = server.handleCommand(playerName + ": open trapdoor");
        System.out.println(response);
        assertTrue(response.contains("door"));
        // Go to cellar
        response = server.handleCommand(playerName + ": goto cellar");
        System.out.println(response);
        assertTrue(response.contains("cellar"));
    }

    @Test
    void multiplayerTest() {
        // Start with the default player
        server.handleCommand(playerName + ": look");
        // Add a new player
        String playerName2 = "maria";
        response = server.handleCommand(playerName2 + ": look");
        System.out.println(response);
        assertTrue(response.contains(playerName));
        // Switch again to main player
        response = server.handleCommand(playerName + ": look");
        System.out.println(response);
        assertTrue(response.contains(playerName2));
        // Get something and check the inventory
        response = server.handleCommand(playerName + ": get coin");
        System.out.println(response);
        assertTrue(response.contains("coin"));
        response = server.handleCommand(playerName + ": check inv");
        System.out.println(response);
        assertTrue(response.contains("coin"));
        // Now the other player cannot get the same thing
        response = server.handleCommand(playerName2 + ": get coin");
        System.out.println(response);
        assertFalse(response.contains("coin"));
        assertTrue(response.contains("ERROR"));
        // Get another thing
        response = server.handleCommand(playerName2 + ": get axe");
        System.out.println(response);
        assertTrue(response.contains("axe"));
        response = server.handleCommand(playerName2 + ": see inventory");
        System.out.println(response);
        assertTrue(response.contains("axe"));
        // Go to the forest
        server.handleCommand(playerName2 + ": goto forest");
        // Know neither of both can see each other
        response = server.handleCommand(playerName2 + ": look");
        assertTrue(response.contains("forest"));
        assertFalse(response.contains(playerName));
        response = server.handleCommand(playerName + ": look");
        assertTrue(response.contains("cabin"));
        assertFalse(response.contains(playerName2));
    }

    @Test
    void playerHealth() {
        // Check initial player health
        response = server.handleCommand(playerName + ": health");
        System.out.println(response);
        assertTrue(response.contains("health"));
        assertTrue(response.contains("3"));
        // Get the potion and coin
        System.out.println(server.handleCommand(playerName + ": get potion"));
        System.out.println(server.handleCommand(playerName + ": get coin"));
        // Now go to the cellar
        System.out.println(server.handleCommand(playerName + ": goto forest"));
        System.out.println(server.handleCommand(playerName + ": get key"));
        System.out.println(server.handleCommand(playerName + ": goto cabin"));
        System.out.println(server.handleCommand(playerName + ": open trapdoor"));
        System.out.println(server.handleCommand(playerName + ": goto cellar"));
        // Fight the elf
        response = server.handleCommand(playerName + ": attack elf");
        System.out.println(response);
        // Check again player health
        response = server.handleCommand(playerName + ": health");
        System.out.println(response);
        assertTrue(response.contains("health"));
        assertTrue(response.contains("2"));
        // Now drink potion
        response = server.handleCommand(playerName + ": drink potion");
        System.out.println(response);
        // Check again player health
        response = server.handleCommand(playerName + ": health");
        System.out.println(response);
        assertTrue(response.contains("health"));
        assertTrue(response.contains("3"));
        // Now attack the elf three times to die
        System.out.println(server.handleCommand(playerName + ": again fight the elf"));
        System.out.println(server.handleCommand(playerName + ": again fight the elf"));
        response = server.handleCommand(playerName + ": again fight the elf");
        System.out.println(response);
        //assertTrue(response.contains("died"));
        // Now, the player must have an empty inventory, health at three and be again in the cabin
        response = server.handleCommand(playerName + ": inv");
        System.out.println(response);
        //assertTrue(response.contains("empty"));
        assertFalse(response.contains("coin"));
        response = server.handleCommand(playerName + ": health");
        System.out.println(response);
        assertTrue(response.contains("3"));
        response = server.handleCommand(playerName + ": look");
        System.out.println(response);
        assertTrue(response.contains("cabin"));
    }
}
