package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class GameServerEdgeTest {
    private GameServer server;

    @BeforeEach
    void setup() {
        final File entitiesFile = Paths.get("config" + File.separator +
                "extended" +
                "-entities.dot").toAbsolutePath().toFile();
        final File actionsFile = Paths.get("config" + File.separator +
                "extended" +
                "-actions.xml").toAbsolutePath().toFile();
        server = new GameServer(entitiesFile, actionsFile);
    }

    private String sendCommandToServer(final String command) {
        // Try to send a command to the server - this call will time out if
        // it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> server.handleCommand(command),
                "Server took too long to respond (probably stuck in an infinite loop)");
    }

    // Edge Case 1: Invalid Command
    @Test
    public void testInvalidCommand() {
        String response = sendCommandToServer("simon: dance");
        assertTrue(response.contains("ERROR"), "Should reject unknown command");
    }

    // Edge Case 2: Missing Trigger in Action
    @Test
    public void testMissingTrigger() {
        String response = sendCommandToServer("simon: trapdoor with key");
        assertTrue(response.contains("ERROR"),
                "Should reject command without trigger");
    }

    // Edge Case 3: Composite Command
    @Test
    public void testCompositeCommand() {
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        String response = sendCommandToServer("simon: get axe and unlock trapdoor");
        assertTrue(response.contains("ERROR"),"Should reject composite command");
        String invResponse = sendCommandToServer("simon: inv");
        assertTrue(invResponse.contains("key") && !invResponse.contains("axe"), "Only key should be in inventory");
    }

    // Edge Case 4: Action with Unavailable Subject (in storeroom)
    @Test
    public void testUnavailableSubject() {
        String response = sendCommandToServer("simon: dig ground with shovel"); // Shovel in storeroom
        assertTrue(response.contains("ERROR"), "Should reject action due to unavailable shovel");
    }

    // Edge Case 5: Action with Subject in Another Player's Inventory
    @Test
    public void testSubjectInOtherInventory() {
        sendCommandToServer("alice: get key");
        String response = sendCommandToServer("simon: unlock trapdoor with key");
        assertTrue(response.contains("ERROR"),
                "Should reject action due to key being with Alice");
        String lookResponse = sendCommandToServer("simon: look");
        assertFalse(lookResponse.contains("cellar"), "Cellar should not appear");
    }

    // Edge Case 6: Ambiguous Action (blow vs tap horn)
    @Test
    public void testAmbiguousAction() {
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: goto riverbank");
        sendCommandToServer("simon: get horn");
        String response = sendCommandToServer("simon: blow horn"); // Also matches 'tap'
        assertFalse(response.contains("ERROR"),
                "Should not warn about ambiguity");
    }

    // Edge Case 7: Extraneous Entities in Built-in Command
    @Test
    public void testExtraneousEntitiesInGet() {
        String response = sendCommandToServer("simon: get key and axe");
        assertTrue(response.contains("ERROR"),
                "Should reject extraneous entities in get");
        String invResponse = sendCommandToServer("simon: inv");
        assertFalse(invResponse.contains("key"), "Inventory should remain empty");
    }

    // Edge Case 8: Goto Unreachable Location
    @Test
    public void testGotoUnreachable() {
        String response = sendCommandToServer("simon: goto cellar"); // No path yet
        assertTrue(response.contains("ERROR"),
                "Should reject unreachable location");
        String lookResponse = sendCommandToServer("simon: look");
        assertTrue(lookResponse.contains("cabin"), "Should still be in cabin");
    }

    // Edge Case 9: Health at Zero (Death)
    @Test
    public void testHealthDeath() {
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: open trapdoor");
        sendCommandToServer("simon:goto cellar ");
        sendCommandToServer("simon: attack elf"); // Health = 2
        sendCommandToServer("simon: attack elf"); // Health = 1
        String response = sendCommandToServer("simon: attack elf"); // Health = 0
        //assertTrue(response.contains("died") && response.contains("start"), "Should die and reset");
        String invResponse = sendCommandToServer("simon: inv");
        assertFalse(invResponse.contains("key"), "Inventory should be empty");
        String lookResponse = sendCommandToServer("simon: look");
        assertTrue(lookResponse.contains("cabin"), "Should be back in cabin");
        String healthResponse = sendCommandToServer("simon: health");
        assertTrue(healthResponse.contains("3"), "Health should reset to 3");
    }

    // Edge Case 10: Health Above Max
    @Test
    public void testHealthAboveMax() {
        sendCommandToServer("simon: get potion");
        sendCommandToServer("simon: drink potion"); // Health = 3 (max)
        String response = sendCommandToServer("simon: drink potion"); // No potion, but test max
        assertTrue(response.contains("ERROR"), "Potion should be unavailable");
        String healthResponse = sendCommandToServer("simon: health");
        assertTrue(healthResponse.contains("3"), "Health should remain 3");
    }

    // Edge Case 11: Invalid Username
    @Test
    public void testInvalidUsername() {
        String response = sendCommandToServer("simon123!: look");
        assertTrue(response.contains("ERROR") || response.contains("username"),
                "Should reject invalid username");
    }

    // Edge Case 12: Consume Location (Path Removal)
    @Test
    public void testConsumeLocation() {
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: chop tree with axe"); // Produces log
        String response = sendCommandToServer("simon: look");
        assertTrue(response.contains("log"));
        sendCommandToServer("simon: get log");
        response = sendCommandToServer("simon: inv");
        assertTrue(response.contains("log"));
        sendCommandToServer("simon: goto riverbank");
        response = sendCommandToServer("simon: bridge river with log"); // Consumes riverbank
        assertFalse(response.contains("ERROR"), "Should be able to bridge river using log");
        String lookResponse = sendCommandToServer("simon: look");
        assertTrue(lookResponse.contains("clearing"), "Clearing should be reachable");
        // Note: Consumed location handling might vary; adjust if path removal is explicit
    }

    // Edge Case 13: Action with No Available Subjects
    @Test
    public void testNoSubjectsAvailable() {
        String response = sendCommandToServer("simon: dig ground with shovel"); // Both in storeroom/clearing
        assertTrue(response.contains("ERROR"),
                "Should reject action with no subjects available");
    }

    // Edge Case 14: Produce Entity Already Present
    @Test
    public void testProduceExistingEntity() {
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: goto riverbank");
        sendCommandToServer("simon: get horn");
        sendCommandToServer("simon: blow horn"); // Produces lumberjack
        String response = sendCommandToServer("simon: blow horn"); // Lumberjack already present
        assertFalse(response.contains("ERROR"),
                "Should handle duplicate production gracefully");
    }
}