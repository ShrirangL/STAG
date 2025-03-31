package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

class ExtendedTestsAya {
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
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> {
                    return server.handleCommand(command);
                },
                "Server took too long to respond (probably stuck in an infinite loop)");
    }

    @Test
    void testGold() {
        String response;
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: goto forest");
        response = sendCommandToServer("simon: chop tree");
        assertTrue(response.contains("cut"), "Did not cut down tree");
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("log"), "Did not find log");
        response = sendCommandToServer("simon: get log");
        assertTrue(response.contains("picked"), "Did not get log");
        response = sendCommandToServer("simon: goto riverbank");
        assertTrue(response.contains("grassy"), "Did not goto riverbank");
        response = sendCommandToServer("simon: bridge river");
        assertTrue(response.contains("bridge"), "Did not bridge river");
        response = sendCommandToServer("simon: goto clearing");
        assertTrue(response.contains("clearing"), "Did not goto clearing");
        sendCommandToServer("simon: goto riverbank");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: get coin");
        sendCommandToServer("simon: unlock trapdoor");
        sendCommandToServer("simon: goto cellar");
        sendCommandToServer("simon: pay elf");
        sendCommandToServer("simon: get shovel");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: goto riverbank");
        sendCommandToServer("simon: goto clearing");
        response = sendCommandToServer("simon: dig ground");
        assertTrue(response.contains("gold"), "Did not find gold");
        sendCommandToServer("simon: get gold");
        response = sendCommandToServer("simon: inv");
        assertTrue(response.contains("gold"), "No gold in inv");
    }
}
