package edu.uob;

import com.sun.source.tree.AssertTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class TestsOnExtendedEntities {
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

    //check that command contains all the inputs
    private boolean commandContainsAll(String command, String... inputs) {
        for(String expected : inputs) {
            if (!command.contains(expected)) {
                return false;
            }
        }
        return true;
    }

    //check that command does not contain any of the inputs
    private boolean commandContainsNone(String command, String... inputs) {
        for(String expected : inputs) {
            if (command.contains(expected)) {
                return false;
            }
        }
        return true;
    }

    @Test
    void testingPlayerHealthAndRespawn() {
        sendCommandToServer("shrirang: get potion");
        sendCommandToServer("shrirang: get axe");
        sendCommandToServer("shrirang: get coin");
        String response = sendCommandToServer("shrirang: inventory");
        assertTrue(response.contains("potion") && response.contains("axe") &&
                response.contains("coin"));

        sendCommandToServer("shrirang: goto forest");
        sendCommandToServer("shrirang: get key");
        response = sendCommandToServer("shrirang: inventory");
        assertTrue(response.contains("potion") && response.contains("axe") &&
                response.contains("coin") && response.contains("key"));

        sendCommandToServer("shrirang: cut tree");
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && response.contains("log") && !response.contains("tree"));

        sendCommandToServer("shrirang: get log");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("potion") && response.contains("axe") &&
                response.contains("coin") && response.contains("key") && response.contains("log"));

        sendCommandToServer("shrirang: goto cabin");
        sendCommandToServer("shrirang: open trapdoor");
        // player should have potion, axe, coin, log but not key
        response = sendCommandToServer("shrirang: inventory");
        assertTrue(response.contains("potion") && response.contains("axe") &&
                response.contains("coin") && response.contains("log") && !response.contains("key"));

        sendCommandToServer("shrirang: goto cellar");
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cellar") && response.contains("elf") && response.contains("cabin"));

        sendCommandToServer("shrirang: attack elf");
        sendCommandToServer("shrirang: attack elf");
        sendCommandToServer("shrirang: attack elf");

        // player should have died and respawned at cabin with empty inventory
        response = sendCommandToServer("shrirang: inventory");
        assertFalse(response.contains("potion") || response.contains("axe") ||
                response.contains("coin") || response.contains("log") || response.contains("key"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("trapdoor"));
        response = sendCommandToServer("shrirang: health");
        assertTrue(response.contains("3"));

        //if player goes to cellar which is possible because there is a path it should see all the dropped item
        sendCommandToServer("shrirang: goto cellar");
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cellar") && response.contains("potion") && response.contains("axe")
                && response.contains("coin") && response.contains("log"));
    }

    @Test
    void performingExtendedActions() {
        sendCommandToServer("shrirang: get potion");
        sendCommandToServer("shrirang: get coin");
        sendCommandToServer("shrirang: get axe");
        String response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("potion") && response.contains("axe") && response.contains("coin"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("trapdoor"));
        assertFalse(response.contains("potion") || response.contains("axe") || response.contains("coin"));

        sendCommandToServer("shrirang: goto forest");
        sendCommandToServer("shrirang: get key");
        sendCommandToServer("shrirang: cut tree");
        sendCommandToServer("shrirang: get log");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("potion") && response.contains("axe") && response.contains("coin")
        && response.contains("log") && response.contains("key"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && !response.contains("tree"));

        sendCommandToServer("shrirang: goto cabin");
        sendCommandToServer("shrirang: open trapdoor");
        sendCommandToServer("shrirang: goto cellar");
        sendCommandToServer("shrirang: attack elf");
        // player's  health should be 2
        response = sendCommandToServer("shrirang: health");
        assertTrue(response.contains("2"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cellar") && response.contains("elf"));

        sendCommandToServer("shrirang: drink potion");
        // player's health should be 3
        response = sendCommandToServer("shrirang: health");
        assertTrue(response.contains("3"));

        sendCommandToServer("shrirang: pay elf");
        // check that shovel has appeared in the cellar
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("shovel") && response.contains("elf"));

        sendCommandToServer("shrirang: get shovel");
        response = sendCommandToServer("shrirang: goto forest"); // should not be possible
        assertTrue(response.contains("ERROR"));

        sendCommandToServer("shrirang: goto cabin");
        sendCommandToServer("shrirang: goto forest");
        sendCommandToServer("shrirang: goto riverbank");
        sendCommandToServer("shrirang: blow horn");
        // see that lumberjack has appeared in the room
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("river") && response.contains("riverbank") && response.contains("lumberjack"));

        sendCommandToServer("shrirang: get lumberjack");
        sendCommandToServer("shrirang: bridge river");
        // see that new path from riverbank to clearing has appeared
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("riverbank") && response.contains("clearing"));

        sendCommandToServer("shrirang: goto clearing");
        sendCommandToServer("shrirang: dig ground");
        // see that gold  has appeared
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("clearing") && response.contains("hole") && response.contains("gold"));
        sendCommandToServer("shrirang: get gold");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("gold"));

        // check that storeroom is empty and player has all the artefacts
    }

    @Test
    void testProducedFromDifferentLocation() {
        sendCommandToServer("shrirang: goto forest");
        sendCommandToServer("shrirang: goto riverbank");
        sendCommandToServer("shrirang: get horn");

        String response = sendCommandToServer("shrirang: blow horn");
        assertEquals("You blow the horn and as if by magic, a lumberjack appears !",response,
                "blow horn command didn't work as expected");

        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("riverbank") && response.contains("lumberjack"));

        sendCommandToServer("shrirang: goto forest");
        response = sendCommandToServer("shrirang: blow horn");
        assertEquals("You blow the horn and as if by magic, a lumberjack appears !",response,
                "blow horn command didn't work as expected");
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && response.contains("lumberjack"));

        sendCommandToServer("shrirang: goto cabin");
        response = sendCommandToServer("shrirang: blow horn");
        assertEquals("You blow the horn and as if by magic, a lumberjack appears !",response,
                "blow horn command didn't work as expected");
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("lumberjack"));

        sendCommandToServer("shrirang: goto forest");
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && !response.contains("lumberjack"));

        sendCommandToServer("shrirang: goto riverbank");
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("riverbank") && !response.contains("lumberjack"));
    }

    @Test
    void testConsumedFromDifferentLocation() {
        sendCommandToServer("shrirang: goto forest");
        sendCommandToServer("shrirang: goto riverbank");
        sendCommandToServer("shrirang: get horn");
        sendCommandToServer("shrirang: goto forest");

        String response = sendCommandToServer("shrirang: blow horn");
        assertTrue(response.contains("You blow the horn and as if by magic, a lumberjack appears !"),
                "blow horn command didn't work as expected");

        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && response.contains("lumberjack"));

        sendCommandToServer("shrirang: goto cabin");

        response = sendCommandToServer("shrirang: tap horn");
        assertTrue(response.contains("You tap the horn and as if by magic, lumberjack disappears !"),
                "tap horn command didn't work as expected");

        sendCommandToServer("shrirang: goto forest");
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && !response.contains("lumberjack"));
    }

    @Test
    void testPlayerNameConsistency() {
        String response = sendCommandToServer("  Shri rang   : inv");
        assertTrue(response.contains("Shri rang"));
        response = sendCommandToServer("  shRi rAng  : inv");
        assertTrue(response.contains("Shri rang"), "Player's name should not be case sensitive");
    }

    @Test
    void testMultiplayerComplexScenario(){
        sendCommandToServer("Shrirang: look"); // sees nobody
        String response = sendCommandToServer("Nilay: look"); // sees Shrirang
        assertTrue(response.contains("Shrirang"));
        response = sendCommandToServer("Mahesh: look"); // sees Shrirang and Nilay
        assertTrue(response.contains("Nilay") && response.contains("Shrirang"));

        sendCommandToServer("Shrirang: get coin");
        sendCommandToServer("Nilay: get axe");
        sendCommandToServer("Mahesh: get potion");

        response = sendCommandToServer("Shrirang: open trapdoor");// can't open without key
        assertTrue(response.contains("ERROR"));
        response = sendCommandToServer("Nilay: open trapdoor");// can't open without key
        assertTrue(response.contains("ERROR"));
        response = sendCommandToServer("Mahesh: open trapdoor");// can't open without key
        assertTrue(response.contains("ERROR"));

        response = sendCommandToServer("Shrirang: get axe");// fail cannot pick artefacts held by others
        assertTrue(response.contains("ERROR"));
        response = sendCommandToServer("Nilay: get potion");//fail cannot pick artefacts held by others
        assertTrue(response.contains("ERROR"));
        response = sendCommandToServer("Mahesh: get coin");// fail cannot pick artefacts held by others
        assertTrue(response.contains("ERROR"));

        response = sendCommandToServer("Shrirang: goto forest"); // sees nobody
        assertFalse(response.contains("Nilay") || response.contains("Mahesh"));
        response = sendCommandToServer("Nilay: goto forest"); // sees Shrirang
        assertTrue(response.contains("Shrirang") && !response.contains("Mahesh"));
        response = sendCommandToServer("Mahesh: goto forest"); // sees Shrirang and Nilay
        assertTrue(response.contains("Shrirang") && response.contains("Nilay"));

        response = sendCommandToServer("Shrirang: get key");
        assertFalse(response.contains("ERROR"));
        response = sendCommandToServer("Shrirang: chop tree");// doesn't have axe
        assertTrue(response.contains("ERROR"));
        sendCommandToServer("Mahesh: chop tree");// fail doesn't axe
        assertTrue(response.contains("ERROR"));
        response = sendCommandToServer("Nilay: chop tree");// success
        assertFalse(response.contains("ERROR"));

        response = sendCommandToServer("Nilay: get log");
        assertFalse(response.contains("ERROR"));
        response = sendCommandToServer("Nilay: goto riverbank");
        assertFalse(response.contains("ERROR"));
        response = sendCommandToServer("Nilay: bridge river");
        assertFalse(response.contains("ERROR"));
        response = sendCommandToServer("Nilay : goto clearing");// success
        assertFalse(response.contains("ERROR"));
        response = sendCommandToServer("Nilay: goto forest"); // fail
        assertTrue(response.contains("ERROR"));

        sendCommandToServer("Shrirang: goto cabin");
        sendCommandToServer("Shrirang: open trapdoor");
        sendCommandToServer("Shrirang: goto cellar");
        response = sendCommandToServer("Shrirang: look");
        assertTrue(response.contains("cellar") && response.contains("elf"));

        sendCommandToServer("Mahesh: goto cabin");
        sendCommandToServer("Mahesh: shut trapdoor");
        response = sendCommandToServer("Mahesh: goto cellar"); // fail, cannot goto cellar
        assertTrue(response.contains("ERROR") && !response.contains("cellar"));

        sendCommandToServer("Shrirang: pay elf");
        sendCommandToServer("Shrirang: get shovel");
        sendCommandToServer("Shrirang: attack elf"); // health is 2
        sendCommandToServer("Shrirang: attack elf"); // health is 1
        sendCommandToServer("Shrirang: attack elf"); // health is 0
        response = sendCommandToServer("Mahesh: attack elf"); //not possible
        assertTrue(response.contains("ERROR"));
        response = sendCommandToServer("Shrirang: look"); // sees Mahesh
        assertTrue(response.contains("cabin") && response.contains("Mahesh"));

        // key is in cellar nobody can open trapdoor
        response = sendCommandToServer("Shrirang: open trapdoor"); // dropped key when died can't open door now
        assertTrue(response.contains("ERROR"));
        response = sendCommandToServer("Nilay: dig ground"); // fail, cannot dig ground without shovel
        assertTrue(response.contains("ERROR"));
        response = sendCommandToServer("Nilay: goto riverbank"); // success
        assertFalse(response.contains("ERROR"));
        response = sendCommandToServer("Nilay: bridge river");// no error
        assertTrue(response.contains("ERROR"));
    }
}
