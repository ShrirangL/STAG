package edu.uob;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;

public class GameServerHardTests {
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

    // Complex Action Sequences
    @Test
    public void testFullGameSequence() {
        // Simon collects items and progresses through the game
        server.handleCommand("simon: get axe");
        server.handleCommand("simon: get coin");
        server.handleCommand("simon: goto forest");
        server.handleCommand("simon: get key");
        server.handleCommand("simon: chop tree with axe"); // Produces log
        server.handleCommand("simon: goto cabin");
        server.handleCommand("simon: open trapdoor with key"); // Produces cellar
        server.handleCommand("simon: goto cellar");
        server.handleCommand("simon: pay elf with coin"); // Produces shovel
        server.handleCommand("simon: goto cabin");
        server.handleCommand("simon: goto forest");
        server.handleCommand("simon: goto riverbank");
        server.handleCommand("simon: get horn");
        server.handleCommand("simon: bridge river with log"); // Produces clearing
        server.handleCommand("simon: goto clearing");
        String response = server.handleCommand("simon: dig ground with shovel"); // Produces hole, gold
        assertTrue(response.contains("You dig into the soft ground and unearth a pot of gold"), 
            "Should complete dig action");
        String lookResponse = server.handleCommand("simon: look");
        assertTrue(lookResponse.contains("gold") && lookResponse.contains("hole"), 
            "Gold and hole should be in clearing");
        assertFalse(lookResponse.contains("ground"), "Ground should be consumed");
    }

    // Multi-player Race Condition
    @Test
    public void testMultiPlayerArtefactConflict() {
        // Simon and Mary race for the key
        server.handleCommand("simon: goto forest");
        server.handleCommand("mary: goto forest");
        server.handleCommand("simon: get key");
        String maryResponse = server.handleCommand("mary: get key");
        assertFalse(maryResponse.contains("key"), "Mary shouldn’t get key after Simon takes it");
        server.handleCommand("simon: goto cabin");
        server.handleCommand("simon: open trapdoor with key"); // Key consumed
        String maryLook = server.handleCommand("mary: look");
        assertFalse(maryLook.contains("key"), "Key should not reappear in forest");
    }

    // Health Stress Test (if implemented)
    @Test
    public void testHealthDepletionAndRevival() {
        // Simon starts in cabin, grabs potion and key, then heads to cellar
        server.handleCommand("simon: get potion"); // Health starts at 3
        server.handleCommand("simon: goto forest");
        server.handleCommand("simon: get key");
        server.handleCommand("simon: goto cabin");
        server.handleCommand("simon: open trapdoor with key"); // Opens cellar
        server.handleCommand("simon: goto cellar");

        // Fight elf twice to reduce health from 3 to 1
        server.handleCommand("simon: fight elf"); // Health 3 -> 2
        String healthAfterFirstFight = server.handleCommand("simon: health");
        assertTrue(healthAfterFirstFight.contains("2"), "Health should be 2 after first fight");

        server.handleCommand("simon: fight elf"); // Health 2 -> 1
        String healthAfterSecondFight = server.handleCommand("simon: health");
        assertTrue(healthAfterSecondFight.contains("1"), "Health should be 1 after second fight");

        // Use potion to recover health back to 2
        String potionResponse = server.handleCommand("simon: drink potion");
        assertTrue(potionResponse.contains("your health improves"), "Should drink potion successfully");
        String healthAfterPotion = server.handleCommand("simon: health");
        assertTrue(healthAfterPotion.contains("2"), "Health should be 2 after drinking potion");
        String invResponse = server.handleCommand("simon: inv");
        assertFalse(invResponse.contains("potion"), "Potion should be consumed");

        // Fight elf again to die (health 2 -> 1 -> 0)
        server.handleCommand("simon: fight elf"); // Health 2 -> 1
        String deathResponse = server.handleCommand("simon: fight elf"); // Health 1 -> 0
        assertTrue(deathResponse.contains("died"), "Should indicate death");

        // Check revival: back to cabin, health reset, inventory dropped
        String locationResponse = server.handleCommand("simon: look");
        assertTrue(locationResponse.contains("cabin"), "Should respawn in cabin");
        String healthResponse = server.handleCommand("simon: health");
        assertTrue(healthResponse.contains("3"), "Health should reset to 3");

        // Verify cellar state separately
        server.handleCommand("simon: goto cellar");
        String cellarLook = server.handleCommand("simon: look");
        assertFalse(cellarLook.contains("potion"), "Potion should not reappear in cellar");
    }

    // Ambiguous Action Resolution
    @Test
    public void testAmbiguousChopCommand() {
        // Assuming a hypothetical second "chop" action existed (e.g., chop log with axe)
        server.handleCommand("simon: get axe");
        server.handleCommand("simon: goto forest");
        server.handleCommand("simon: chop tree with axe"); // Produces log
        String ambiguousResponse = server.handleCommand("simon: chop"); // Tree gone, log present
        assertFalse(ambiguousResponse.contains("You cut down"), "Should reject ambiguous command");
        assertTrue(ambiguousResponse.contains("more than one"), "Should indicate ambiguity");
    }

    // Path Manipulation Edge Case
    @Test
    public void testConsumeLocationPath() {
        // Hypothetical: Add an action to consume forest (not in XML, but tests logic)
        // e.g., <action> <trigger>burn</trigger> <subject>forest</subject> <consumed>forest</consumed> ...
        server.handleCommand("simon: goto forest");
        String gotoResponse = server.handleCommand("simon: goto cabin"); // Assume burn consumes forest later
        assertTrue(gotoResponse.contains("cabin"), "Should move to cabin");
        // Simulate forest consumption (modify XML or mock this in your code if possible)
        String backResponse = server.handleCommand("simon: goto forest");
        assertFalse(backResponse.contains("forest"), "Path to consumed forest should be gone");
    }

    // Storeroom Interaction Attempt
    @Test
    public void testAccessStoreroomIllegally() {
        String response = server.handleCommand("simon: goto storeroom");
        assertFalse(response.contains("storeroom"), "Should not allow direct access to storeroom");
        server.handleCommand("simon: get axe");
        server.handleCommand("simon: goto forest");
        server.handleCommand("simon: chop tree with axe"); // Log from storeroom
        String lookResponse = server.handleCommand("simon: look");
        assertTrue(lookResponse.contains("log"), "Log should be in forest, not storeroom");
    }

    // Overloaded Command with Extraneous Entities
    @Test
    public void testExtraneousEntitiesInAction() {
        server.handleCommand("simon: get axe");
        server.handleCommand("simon: get coin");
        server.handleCommand("simon: goto forest");
        String response = server.handleCommand("simon: chop tree with axe and coin");
        assertFalse(response.contains("You cut down the tree"), "Should reject extraneous coin");
        String lookResponse = server.handleCommand("simon: look");
        assertTrue(lookResponse.contains("tree"), "Tree should remain");
    }

    // Rapid Multi-player Interaction
    @Test
    public void testConcurrentActionInterference() {
        server.handleCommand("simon: goto forest");
        server.handleCommand("mary: goto forest");
        server.handleCommand("simon: get key");
        server.handleCommand("mary: goto cabin"); // Mary tries to use key Simon has
        server.handleCommand("simon: goto cabin");
        String maryResponse = server.handleCommand("mary: open trapdoor with key");
        assertFalse(maryResponse.contains("You unlock the door"), "Mary can’t use Simon’s key");
        String simonResponse = server.handleCommand("simon: open trapdoor with key");
        assertTrue(simonResponse.contains("You unlock the door"), "Simon should succeed");
    }

    // Health Overlap with Actions (if implemented)
    @Test
    public void testHealthAndActionRace() {
        server.handleCommand("simon: goto forest");
        server.handleCommand("simon: get key");
        server.handleCommand("simon: goto cabin");
        server.handleCommand("simon: open trapdoor with key");
        server.handleCommand("simon: goto cellar");
        server.handleCommand("simon: fight elf"); // Health 3 -> 2
        server.handleCommand("simon: fight elf"); // Health 2 -> 1
        String deathResponse = server.handleCommand("simon: pay elf with coin"); // Health 1, but action attempted
        assertFalse(deathResponse.contains("died"), "Pay should succeed before death");
        assertTrue(deathResponse.contains("produces a shovel"), "Shovel should appear");
        server.handleCommand("simon: fight elf"); // Health 1 -> 0
        String lookResponse = server.handleCommand("simon: look");
        assertTrue(lookResponse.contains("cabin"), "Should respawn after death");
    }

    // Extreme Command Flexibility
    @Test
    public void testHighlyDecoratedCommand() {
        server.handleCommand("simon: get axe");
        server.handleCommand("simon: goto forest");
        String response = server.handleCommand("simon: kindly please chop down the tall tree using my sharp axe now");
        assertTrue(response.contains("You cut down the tree"), "Should handle excessive decoration");
        String lookResponse = server.handleCommand("simon: look");
        assertTrue(lookResponse.contains("log"), "Log should appear");
    }
}