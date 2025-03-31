package edu.uob;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExtendedMahesh {

    private GameServer server;

    // Create a new server _before_ every @Test
    @BeforeEach
    void setup() {
        File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
        File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
        server = new GameServer(entitiesFile, actionsFile);
    }

    String sendCommandToServer(String command) {
        // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
        "Server took too long to respond (probably stuck in an infinite loop)");
    }

    // Two players
    @Test
    void testDeathWithSecondPlayer() {
        // Players should see each other
        sendCommandToServer("simon: look");
        sendCommandToServer("mahesh: look");
        String response = sendCommandToServer("simon: look");
        assertTrue(response.contains("mahesh"), "Player can't see other player");
        response = sendCommandToServer("mahesh: look");
        assertTrue(response.contains("simon"), "Player can't see other player");

        // Open trapdoor and goto cellar
        sendCommandToServer("simon: goto forest");
        response = sendCommandToServer("mahesh: look");
        assertFalse(response.contains("simon"), "Player should not see other player who has left the location");
        response = sendCommandToServer("simon: look");
        assertFalse(response.contains("mahesh"), "Player should not see other player who is at other location");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto riverbank");
        sendCommandToServer("mahesh: goto forest");
        sendCommandToServer("mahesh: goto riverbank");
        response = sendCommandToServer("mahesh: look");
        assertTrue(response.contains("horn"), "Player did not see horn in riverbank");
        sendCommandToServer("simon: get horn");
        response = sendCommandToServer("mahesh: look");
        assertFalse(response.contains("horn"), "Player did not see horn in riverbank");
        sendCommandToServer("mahesh: get horn");
        response = sendCommandToServer("mahesh: inv");
        assertFalse(response.contains("horn"), "Player should not get things already taken by other player");
        response = sendCommandToServer("simon: horn     blow");
        assertTrue(response.contains("lumberjack"), "Player can't see lumberjack after blowing horn");

        // Open trapdoor and goto cellar
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("mahesh: goto forest");
        sendCommandToServer("mahesh: goto cabin");
        response = sendCommandToServer("mahesh: open trapdoor");
        assertFalse(response.contains("unlock"), "Player should not unlock door without key in his inventory");
        response = sendCommandToServer("simon: open trapdoor");
        assertTrue(response.contains("unlock"), "Player could not unlock door with key");
        sendCommandToServer("simon: goto cellar");
        sendCommandToServer("mahesh: goto cellar");
        response = sendCommandToServer("mahesh: look");
        assertTrue(response.contains("simon"), "Player could not see other player");

        // Attack the elf 3 times (each attack reduces 1 point of health)
        response = sendCommandToServer("simon: health");
        assertTrue(response.contains("3"), "Player's health should be 3 before fight");
        response = sendCommandToServer("mahesh: health");
        assertTrue(response.contains("3"), "Player's health should be 3 before fight");
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("elf"), "Player can't see elf in cellar");
        sendCommandToServer("simon: attack elf");
        response = sendCommandToServer("simon: health");
        assertTrue(response.contains("2"), "Player's health should be 2");
        response = sendCommandToServer("mahesh: health");
        assertTrue(response.contains("3"), "Player's health should be 3 before fight");
        sendCommandToServer("simon: attack elf");
        response = sendCommandToServer("simon: health");
        assertTrue(response.contains("1"), "Player's health should be 1");
        response = sendCommandToServer("mahesh: look");
        assertFalse(response.contains("horn"), "Horn is with other player. Still could see in location.");
        response = sendCommandToServer("simon: attack elf");
        //assertTrue(response.contains("died"), "Player did not die after being attacked 3 times");
        response = sendCommandToServer("mahesh: look");
        assertFalse(response.contains("simon"), "Player should not see other player who died");
        assertTrue(response.contains("horn"), "Player should see dead player's inventory at the location");
        sendCommandToServer("mahesh: goto cabin");
        response = sendCommandToServer("mahesh: look");
        assertTrue(response.contains("simon"), "Player should see died player respawned at the start location");

        // Simulate respawn and check if the player is revived at the original location
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("cabin"), "Player did not respawn at the original location");
        response = sendCommandToServer("simon: health");
        assertTrue(response.contains("3"), "Player's health should be 3 after rebirth");

        // Check if the player's inventory was dropped at the place of death
        response = sendCommandToServer("simon: inv");
        assertFalse(response.contains("horn"), "Player's inventory still contains items that should have been dropped at the place of death");

        // Ensure that items were dropped at the place of death (assuming the items should be at the cellar)
        sendCommandToServer("simon: goto cellar");
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("horn"), "Player's inventory was not dropped at the place of death");
    }
}
