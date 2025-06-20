package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

class BasicTestsAya {

  private GameServer server;

  // Create a new server _before_ every @Test
  @BeforeEach
  void setup() {
      File entitiesFile = Paths.get("config" + File.separator + "basic-entities.dot").toAbsolutePath().toFile();
      File actionsFile = Paths.get("config" + File.separator + "basic-actions.xml").toAbsolutePath().toFile();
      server = new GameServer(entitiesFile, actionsFile);
  }

  String sendCommandToServer(String command) {
      // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
      return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
      "Server took too long to respond (probably stuck in an infinite loop)");
  }

  // A lot of tests will probably check the game state using 'look' - so we better make sure 'look' works well !
  @Test
  void testLook() {
    String response = sendCommandToServer("simon: look");
    response = response.toLowerCase();
    assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
    assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
    assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
    assertTrue(response.contains("trapdoor"), "Did not see description of furniture in response to look");
    assertTrue(response.contains("forest"), "Did not see available paths in response to look");
  }

  // Test that we can goto a different location (we won't get very far if we can't move around the game !)
  @Test
  void testGoto()
  {
      sendCommandToServer("simon: goto forest");
      String response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
  }

    // Test that we can pick something up and that it appears in our inventory
    @Test
    void testGet()
    {
        String response;
        sendCommandToServer("simon: get potion");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertFalse(response.contains("potion"), "Potion is still present in the room after an attempt was made to get it");
    }

    @Test
    void testDrop()
    {
        String response;
        sendCommandToServer("simon: get potion");
        sendCommandToServer("simon: drop potion");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertFalse(response.contains("potion"), "Potion is still present in the room after an attempt was made to get it");
    }

    @Test
    void testUnlock()
    {
        String response;
        response = sendCommandToServer("simon: goto forest");
        assertTrue(response.contains("cabin"), "Did not see the cabin path from forest");
        sendCommandToServer("simon: get key");
        response = sendCommandToServer("simon: inv");
        assertTrue(response.contains("key"), "Did not see the key in the inventory");
        response = sendCommandToServer("simon: goto cabin");
        assertTrue(response.contains("log cabin"), "Did not return to cabin");
        response = sendCommandToServer("simon: unlock trapdoor");
        assertTrue(response.contains("trapdoor"), "Did not unlock trapdoor");
        response = sendCommandToServer("simon: look");
        assertTrue(response.contains("forest"), "Did not see the forest path from cabin");
        assertTrue(response.contains("cellar"), "Cellar path was not produced");
        response = sendCommandToServer("simon: inv");
        assertFalse(response.contains("key"), "Key still in inventory");
        response = sendCommandToServer("simon: goto forest");
        assertTrue(response.contains("forest"), "Could not go to forest after unlocking door to cellar");
        response = sendCommandToServer("simon: goto cabin");
        assertTrue(response.contains("cabin"), "Could not go to cabin");
        response = sendCommandToServer("simon: goto cellar");
        assertTrue(response.contains("cellar"), "Could not go to cellar");
    }

    @Test
    void testChop()
    {
        String response;
        sendCommandToServer("simon: get axe");
        response = sendCommandToServer("simon: inv");
        assertTrue(response.contains("axe"), "Did not see the axe in the inventory");
        response = sendCommandToServer("simon: goto forest");
        assertTrue(response.contains("cabin"), "Did not see the cabin path from forest");
        response = sendCommandToServer("simon: chop tree");
        assertTrue(response.contains("tree"), "Could not chop tree");
        response = sendCommandToServer("simon: inv");
        assertTrue(response.contains("axe"), "Did not see the axe in the inventory");
        response = sendCommandToServer("simon: look");
        assertFalse(response.contains("tree"), "Tree was not consumed");
        assertTrue(response.contains("log"), "Log was not produced");
    }
}
