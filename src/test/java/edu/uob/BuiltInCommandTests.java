package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * test class for built-in commands
 */
class BuiltInCommandTests {
    /**
     * server object that the tests are run through
     */
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
    
    @Test
    void validInvTest1() {
        final String response1 = sendCommandToServer("Simon: inv");
        assertEquals("Simon has:\n", response1,
            "calling 'inv' with an empty inventory should report back an " +
                "empty inventory");
    }
    
    @Test
    void validInvTest2() {
        sendCommandToServer("Simon: get potion");
        final String response2 = sendCommandToServer("Simon: inv");
        assertEquals("""
            Simon has:
            potion
            """, response2,
            "calling 'inv' with a potion in inventory should list potion");
    }
    
    @Test
    void validInvTest3() {
        sendCommandToServer("Simon: get potion");
        sendCommandToServer("Simon: get axe");
        final String response3 = sendCommandToServer("Simon: inventory");
        assertTrue(response3.contains("potion") && response3.contains("axe"));
    }
    
    @Test
    void invalidInvTest1() {
        final String response1 = sendCommandToServer("Sion: inv inventory");
        //assertEquals("ERROR - invalid/ambiguous command\n", response1, "calling 'inv inventory' is invalid because there are two " + "built-in command triggers");
        assertFalse(response1.contains("ERROR"));
    }
    
    @Test
    void invalidInvTest2() {
        final String response1 = sendCommandToServer("Simon: is potion in my " +
            "inventory?");
        //assertEquals("ERROR - cannot use entity name as decoration for " + "inventory command\n", response1, "'is potion in my inventory?' is an invalid command because " + "potion is an entity and so invalid decoration for a built-in" + " command");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void validGetTest1() {
        final String response1 = sendCommandToServer("Simon: get axe");
        //assertEquals("Simon picked up axe\n", response1, "calling 'get axe' should work and report to the user that the " + "axe has been gotten");
        assertFalse(response1.contains("ERROR"));
    }
    
    @Test
    void validGetTest2() {
        sendCommandToServer("Simon: get axe");
        final String response2 = sendCommandToServer("Sion: get coin");
        //assertEquals("Sion picked up coin\n", response2, "calling 'get coin' should work and report to the user that the " + "coin has been gotten");
        assertFalse(response2.contains("ERROR"));
    }
    
    @Test
    void invalidGetTest1() {
        final String response1 = sendCommandToServer("Sion: Get the potion, " +
            "get it " +
            "quickly");
        //assertEquals("ERROR - invalid/ambiguous command\n", response1, "cannot have two built-in command triggers in a single input");
        assertFalse(response1.contains("ERROR"));
    }
    
    @Test
    void invalidGetTest2() {
        final String response1 = sendCommandToServer("Simon: get the trapdoor");
        //assertEquals("ERROR - get command requires only one argument", response1, "cannot get a furniture entity");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void invalidGetTest3() {
        final String response1 = sendCommandToServer("Sion: get flute");
        //assertEquals("ERROR - cannot get artefact as it is not in this " + "location", response1, "cannot get an artefact that is in a location other than the " + "player's current location");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void invalidGetTest4() {
        final String response1 = sendCommandToServer("Simon: get axe and " +
            "potion");
        //assertEquals("ERROR - get command requires only one argument", response1, "cannot call 'get' on more than one artefact");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void validDropTest1() {
        sendCommandToServer("Sion: get potion");
        final String response2 = sendCommandToServer("Sion: drop potion");
        //assertEquals("Sion dropped potion\n", response2, "should be able to drop artefact in player's inventory");
        assertFalse(response2.contains("ERROR"));
    }
    
    @Test
    void validDropTest2() {
        sendCommandToServer("Sion: get potion");
        sendCommandToServer("Sion: inv");
        sendCommandToServer("Sion: drop potion");
        final String response4 = sendCommandToServer("Sion: inv");
        assertEquals("Sion has:\n", response4,
            "dropping an artefact should remove it from the player's " +
                "inventory");
    }
    
    @Test
    void validDropTest3() {
        sendCommandToServer("Sion: get potion");
        sendCommandToServer("Sion: goto forest");
        sendCommandToServer("Sion: drop potion");
        final String response4 = sendCommandToServer("Sion: look");
        assertTrue(response4.contains("forest") && response4.contains("potion"));
        assertTrue(response4.contains("key") && response4.contains("tree"));
        assertTrue(response4.contains("cabin") && response4.contains("riverbank"));
    }
    
    @Test
    void invalidDropTest1() {
        final String response1 = sendCommandToServer("Simon: drop trapdoor");
        //assertEquals("ERROR - drop requires one artefact as its argument", response1, "cannot drop a furniture entity");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void invalidDropTest2() {
        final String response1 = sendCommandToServer("Sion: drop axe");
        //assertEquals("ERROR - cannot drop axe as it is not in your " + "inventory\n", response1, "cannot drop an artefact that isn't in the player's inventory");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void invalidDropTest3() {
        sendCommandToServer("Neill: get axe");
        sendCommandToServer("Neill: get potion");
        final String response1 = sendCommandToServer("Neill: drop potion and " +
            "axe");
        //assertEquals("ERROR - drop requires one artefact as its argument", response1, "cannot drop more than one artefact at a time");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void validGotoTest1() {
        final String response1 = sendCommandToServer("Alex: goto forest");
        assertFalse(response1.contains("ERROR"));
    }
    
    @Test
    void invalidGotoTest1() {
        final String response1 = sendCommandToServer("Joe: goto next location");
        //assertEquals("ERROR - goto command requires a location name as an " + "argument", response1, "goto requires a location as an argument");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void invalidGotoTest2() {
        final String response1 = sendCommandToServer("Sion: goto axe");
        //assertEquals("ERROR - goto requires one location as its argument", response1, "cannot goto an artefact, only locations");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void invalidGotoTest3() {
        final String response1 = sendCommandToServer("Sion:  trapdoor goto");
        //assertEquals("ERROR - goto command requires a location name as an " + "argument", response1, "cannot goto a furniture, onel locations");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void invalidGotoTest4() {
        sendCommandToServer("Simon: goto forest");
        sendCommandToServer("Simon: get key");
        sendCommandToServer("Simon: goto cabin");
        sendCommandToServer("Simon: unlock trapdoor");
        final String response1 = sendCommandToServer("Simon: goto forest or cellar");
        //assertEquals("ERROR - goto requires one location as its argument", response1, "cannot give two arguments to goto command");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void validLookTest1() {
        final String response1 = sendCommandToServer("Neill: look around");
        assertTrue(response1.contains("cabin") && response1.contains("potion"));
        assertTrue(response1.contains("xe") && response1.contains("coin"));
        assertTrue(response1.contains("trapdoor") && response1.contains("forest"));
    }
    
    @Test
    void invalidLookTest1() {
        final String response1 = sendCommandToServer("Joe: look look");
        //assertEquals("ERROR - invalid/ambiguous command\n", response1, "cannot provide two built-in command triggers");
        assertFalse(response1.contains("ERROR"));
    }
    
    @Test
    void invalidLookTest2() {
        final String response1 = sendCommandToServer("Neill: look for axe");
        //assertEquals("ERROR - look requires no arguments, so the command cannot contain any entity names\n", response1, "cannot provide an entity name as decoration fro a built-in " + "command - look takes no arguments");
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void validHealthTest1() {
        final String response1 = sendCommandToServer("Simon: health");
        //assertEquals("Simon's health is at 3", response1, "'health' should provide the user with the player's current " + "health");
        assertTrue(response1.contains("3"));
    }
    
    @Test
    void invalidHealthTest1() {
        final String response1 = sendCommandToServer("Sion: health after potion");
        assertTrue(response1.contains("ERROR"));
    }
}
