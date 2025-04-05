package edu.uob;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaheshMyTests {

  private GameServer server;

  // Create a new server _before_ every @Test
  @BeforeEach
  void setup() {
      File entitiesFile = Paths.get("config" + File.separator + "my-entities.dot").toAbsolutePath().toFile();
      File actionsFile = Paths.get("config" + File.separator + "my-actions.xml").toAbsolutePath().toFile();
      server = new GameServer(entitiesFile, actionsFile);
  }

  String sendCommandToServer(String command) {
      // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
      return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
      "Server took too long to respond (probably stuck in an infinite loop)");
  }

  // A lot of tests will probably check the game state using 'look' - so we better make sure 'look' works well !
  @Test
  void testActionsWithSameTriggerWords() {
      String response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
      sendCommandToServer("simon: get axe");
      sendCommandToServer("simon: goto forest");
      sendCommandToServer("simon: cut tree");
      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("log"), "Did not see log after cutting tree");
      sendCommandToServer("simon: goto cabin");
      sendCommandToServer("simon: cut apple");
      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("food"), "Did not see food after cutting apple");
  }

  @Test
  void testConsumedProducedLocation() {
      String response = sendCommandToServer("simon: look");
      assertTrue(response.contains("forest"), "Did not see path to forest from cabin");

      // Close gate and try to goto forest
      sendCommandToServer("simon: close gate");
      response = sendCommandToServer("simon: look");
      assertFalse(response.contains("forest"), "Should not see path to forest after closing gate");

      sendCommandToServer("simon: goto forest");
      response = sendCommandToServer("simon: look");
      assertFalse(response.contains("tree"), "Should not be able to goto forest when gate was closed");

      // Open gate and try to goto forest
      sendCommandToServer("simon: open gate");
      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("forest"), "Should see path to forest after opening gate");

      sendCommandToServer("simon: goto forest");
      response = sendCommandToServer("simon: look");
      assertTrue(response.contains("tree"), "Should be able to goto forest when gate was opened");

    // Open trapdoor (same trigger word but different subject)
    sendCommandToServer("simon: goto forest");
    sendCommandToServer("simon: get key");
    sendCommandToServer("simon: goto cabin");
    sendCommandToServer("simon: open trapdoor");
    response = sendCommandToServer("simon: look");
    assertTrue(response.contains("cellar"), "Should see path to cellar after opening trapdoor");
  }


}
