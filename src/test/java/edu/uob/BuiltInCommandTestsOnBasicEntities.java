package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuiltInCommandTestsOnBasicEntities {
    /**
     * server object that the tests are run through
     */
    private GameServer server;

    @BeforeEach
    void setup() {
        final File entitiesFile = Paths.get("config" + File.separator +
                "basic" +
                "-entities.dot").toAbsolutePath().toFile();
        final File actionsFile = Paths.get("config" + File.separator +
                "basic" +
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
    void validTest1() {
        //1
        String response = sendCommandToServer("shrirang: inventory");
        assertEquals("The player has:\n", response,
                "calling 'inv' with an empty inventory should report back an " +
                        "empty inventory");
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("trapdoor")
                && response.contains("potion") && response.contains("axe"));

        //2
        sendCommandToServer("shrirang: get axe");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("axe"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("trapdoor")
        && response.contains("potion") && !response.contains("axe"));

        //3
        sendCommandToServer("shrirang: get potion");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("potion") && response.contains("axe"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin ") && response.contains("trapdoor")
        && !response.contains("potion") && !response.contains("axe"));

        //4
        sendCommandToServer("shrirang: get trapdoor");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("potion") && response.contains("axe")
                && !response.contains("trapdoor"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("trapdoor")
        && !response.contains("potion") && !response.contains("axe"));

        //5
        sendCommandToServer("shrirang: drop potion");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(!response.contains("potion") && response.contains("axe")
                && !response.contains("trapdoor"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("trapdoor")
                && response.contains("potion") && !response.contains("axe"));

        //dropping potion again
        sendCommandToServer("shrirang: drop potion");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(!response.contains("potion") && response.contains("axe")
                && !response.contains("trapdoor"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("trapdoor")
                && response.contains("potion") && !response.contains("axe"));

        //6
        sendCommandToServer("shrirang: drop axe");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(!response.contains("potion") && !response.contains("axe")
                && !response.contains("trapdoor"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("trapdoor")
                && response.contains("potion") && response.contains("axe"));

        //7
        sendCommandToServer("shrirang: goto forest");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(!response.contains("potion") && !response.contains("axe")
                && !response.contains("trapdoor"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && response.contains("key") && response.contains("tree"));
        assertFalse(response.contains("trapdoor") || response.contains("potion") || response.contains("axe"));

        //8
        sendCommandToServer("shrirang: get key");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("key") && !response.contains("tree"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && response.contains("tree") && !response.contains("key"));

        //9
        sendCommandToServer("shrirang: get tree");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("key") && !response.contains("tree"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && response.contains("tree") && !response.contains("key"));

        //10
        sendCommandToServer("shrirang: drop key");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(!response.contains("key") && !response.contains("tree"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && response.contains("tree") && response.contains("key"));

        //11
        sendCommandToServer("shrirang: get key");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("key") && !response.contains("tree"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && response.contains("tree") && !response.contains("key"));

        //12
        sendCommandToServer("shrirang: goto cabin");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("key") && !response.contains("tree"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("axe") && response.contains("potion") && response.contains("trapdoor"));
        assertFalse(response.contains("tree") || response.contains("key"));

        //13
        sendCommandToServer("shrirang: get axe");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("axe") && response.contains("key") && !response.contains("potion") && !response.contains("trapdoor"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("potion") && response.contains("trapdoor") && !response.contains("axe"));

        //14
        sendCommandToServer("shrirang: goto cellar");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("axe") && response.contains("key") && !response.contains("potion") && !response.contains("trapdoor"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("potion") && response.contains("trapdoor") && !response.contains("axe"));

        //15
        sendCommandToServer("shrirang: could you please open or unlock trapdoor with the key");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(!response.contains("key") && response.contains("axe"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("potion") && response.contains("trapdoor")
                && !response.contains("axe") && !response.contains("key"));

        //16
        sendCommandToServer("shrirang: goto cellar");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("axe"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cellar") && response.contains("elf") && !response.contains("trapdoor")
                && !response.contains("axe") && !response.contains("key"));

        //17
        sendCommandToServer("shrirang: goto cabin");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("axe"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("potion") && response.contains("trapdoor")
                && !response.contains("axe") && !response.contains("key"));

        //18
        sendCommandToServer("shrirang: goto forest");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("axe"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && response.contains("tree") && !response.contains("trapdoor"));

        //19
        sendCommandToServer("shrirang: could you please chop, cut or cutdown tree with the axe");
        response = sendCommandToServer("shrirang: inv");
        assertTrue(response.contains("axe"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("forest") && response.contains("log") && !response.contains("tree"));

        //storeroom has log and key;
    }
}