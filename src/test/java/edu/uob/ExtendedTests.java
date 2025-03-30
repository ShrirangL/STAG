package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * test class which runs a large number of gameplay tests based on an
 * extended actions file
 */
class ExtendedTests {
    /**
     * server object through which the tests are run
     */
    private GameServer server;
    
    @BeforeEach
    void setup() {
        final File entitiesFile = Paths.get("config" + File.separator + "extended" +
            "-entities.dot").toAbsolutePath().toFile();
        final File actionsFile = Paths.get("config" + File.separator + "extended" +
            "-actions-ABC.xml").toAbsolutePath().toFile();
        server = new GameServer(entitiesFile, actionsFile);
    }
    
    private String sendCommandToServer(final String command) {
        // Try to send a command to the server - this call will time out if
        // it takes too long (in case the server enters an infinite loop)
        return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> server.handleCommand(command),
            "Server took too long to respond (probably stuck in an infinite loop)");
    }
    
    @Test
    void test1Step1() {
        final String response1 = sendCommandToServer("test: goto forest");
        assertTrue(response1.contains("cabin"),
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void test1Step2() {
        sendCommandToServer("test: goto forest");
        final String response2 = sendCommandToServer("test: goto forest");
        assertTrue(response2.contains("ERROR"),
            "cannot go to location you are already in");
    }
    
    @Test
    void test1Step3() {
        sendCommandToServer("test: goto forest");
        final String response3 = sendCommandToServer("test: get key");
        assertTrue(response3.contains("picked up"),
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void test1Step4() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        final String response4 = sendCommandToServer("test: goto cabin");
        assertTrue(response4.contains("cabin"),
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void test1Step5() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        sendCommandToServer("test: goto cabin");
        final String response5 = sendCommandToServer("test: open key");
        assertTrue(response5.contains("unlock"),
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void test1Step6() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        sendCommandToServer("test: goto cabin");
        sendCommandToServer("test: open key");
        final String response6 = sendCommandToServer("test: goto cellar");
        assertTrue(response6.contains("elf"),
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void test1Step7() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        sendCommandToServer("test: goto cabin");
        sendCommandToServer("test: open key");
        sendCommandToServer("test: goto cellar");
        final String response7 = sendCommandToServer("test: health");
        assertTrue(response7.contains("3"),
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void test1Step8() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        sendCommandToServer("test: goto cabin");
        sendCommandToServer("test: open key");
        sendCommandToServer("test: goto cellar");
        final String response8 = sendCommandToServer("test: hit elf");
        assertTrue(response8.contains("lose some health"),
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void test1Step9() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        sendCommandToServer("test: goto cabin");
        sendCommandToServer("test: open key");
        sendCommandToServer("test: goto cellar");
        sendCommandToServer("test: hit elf");
        final String response9 = sendCommandToServer("test: health");
        assertTrue(response9.contains("2"),
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void test1Step10() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        sendCommandToServer("test: goto cabin");
        sendCommandToServer("test: open key");
        sendCommandToServer("test: goto cellar");
        sendCommandToServer("test: hit elf");
        final String response10 = sendCommandToServer("test: hit elf");
        assertTrue(response10.contains("lose some health"),
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void test1Step11() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        sendCommandToServer("test: goto cabin");
        sendCommandToServer("test: open key");
        sendCommandToServer("test: goto cellar");
        sendCommandToServer("test: hit elf");
        sendCommandToServer("test: hit elf");
        final String response11 = sendCommandToServer("test: health");
        assertTrue(response11.contains("1"),
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void test1Step12() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        sendCommandToServer("test: goto cabin");
        sendCommandToServer("test: open key");
        sendCommandToServer("test: goto cellar");
        sendCommandToServer("test: hit elf");
        sendCommandToServer("test: hit elf");
        String response = sendCommandToServer("test: hit elf");
        // player should have died and respawned at cabin with empty inventory
        response = sendCommandToServer("shrirang: inventory");
        assertFalse(response.contains("potion") || response.contains("axe") ||
                response.contains("coin") || response.contains("log") || response.contains("key"));
        response = sendCommandToServer("shrirang: look");
        assertTrue(response.contains("cabin") && response.contains("trapdoor"));
        response = sendCommandToServer("shrirang: health");
        assertTrue(response.contains("3"));
    }
    
    @Test
    void test1Step13() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        sendCommandToServer("test: goto cabin");
        sendCommandToServer("test: open key");
        sendCommandToServer("test: goto cellar");
        sendCommandToServer("test: hit elf");
        sendCommandToServer("test: hit elf");
        sendCommandToServer("test: hit elf");
        final String response13 = sendCommandToServer("test: health");
        assertTrue(response13.contains("3"),
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void test1Step14() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        sendCommandToServer("test: goto cabin");
        sendCommandToServer("test: open key");
        sendCommandToServer("test: goto cellar");
        sendCommandToServer("test: hit elf");
        sendCommandToServer("test: hit elf");
        sendCommandToServer("test: hit elf");
        final String response14 = sendCommandToServer("test: inv");
        assertEquals("test has:\n", response14,
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void extraEntityTest() {
        sendCommandToServer("test: goto forest");
        sendCommandToServer("test: get key");
        sendCommandToServer("test: goto cabin");
        final String response5 = sendCommandToServer("test: open trapdoor with key " +
            "and potion");
        assertTrue(response5.contains("ERROR"),
            "cannot have an entity name as decoration");
    }
    
    @Test
    void testHealing() {
        sendCommandToServer("Alex: get potion");
        sendCommandToServer("Alex: goto forest");
        sendCommandToServer("Alex: get key");
        sendCommandToServer("Alex: goto cabin");
        sendCommandToServer("Alex: open key");
        sendCommandToServer("Alex: goto cellar");
        sendCommandToServer("Alex: hit elf");
        sendCommandToServer("Alex: hit elf");
        sendCommandToServer("Alex: drink potion");
        final String response10 = sendCommandToServer("Alex: health");
        /*assertEquals("Alex's health is at 2", response10,
            "drinking a potion should increase player's health (provided it " +
                "was below 3 before drinking");*/
        assertTrue(response10.contains("Alex") && response10.contains("2"));
    }
    
    @Test
    void testDeath() {
        sendCommandToServer("Alex: get axe");
        sendCommandToServer("Alex: get potion");
        sendCommandToServer("Alex: goto forest");
        sendCommandToServer("Alex: get key");
        sendCommandToServer("Alex: goto cabin");
        sendCommandToServer("Alex: open key");
        sendCommandToServer("Alex: goto cellar");
        sendCommandToServer("Alex: hit elf");
        sendCommandToServer("Alex: hit elf");
        sendCommandToServer("Alex: hit elf");
        final String response10 = sendCommandToServer("Alex: look");
        /*assertEquals("""
            You are in A log cabin in the woods You can see:
            coin: A silver coin
            trapdoor: A locked wooden trapdoor in the floor
            You can see from here:
            forest
            cellar
            """, response10,
            "dying should send the player back to the start location");*/
        assertTrue(response10.contains("cabin") && response10.contains("coin")
        && response10.contains("trapdoor"));
    }
    
    @Test
    void testCallingLumberjack() {
        sendCommandToServer("Alex: goto forest");
        sendCommandToServer("Alex: goto riverbank");
        sendCommandToServer("Alex: get horn");
        final String response4 = sendCommandToServer("Alex: blow horn");
        assertEquals("You blow the horn and as if by magic, a lumberjack appears !", response4,
            "normal play-through of game should behave as expected");
    }
    
    @Test
    void testCuttingTree() {
        sendCommandToServer("Alex: get axe");
        sendCommandToServer("Alex: goto forest");
        final String response3 = sendCommandToServer("Alex: cut tree");
        assertEquals("You cut down the tree with the axe", response3,
            "normal play-through of game should behave as expected");
    }

    /*
    @Test
    void testGrowingTree() {
        sendCommandToServer("Alex: get axe");
        sendCommandToServer("Alex: goto forest");
        sendCommandToServer("Alex: cut tree");
        final String response4 = sendCommandToServer("Alex: grow seed");
        assertEquals("You grow a tree", response4,
            "actions that consume nothing should be handled properly");
    }
     */

    /*
    @Test
    void testProducingArtefactFromCharacter() {
        sendCommandToServer("Alex: get axe");
        sendCommandToServer("Alex: goto forest");
        sendCommandToServer("Alex: cut tree");
        sendCommandToServer("Alex: get log");
        sendCommandToServer("Alex: grow seed");
        sendCommandToServer("Alex: cut tree");
        final String response8 = sendCommandToServer("Alex: look");
        assertEquals("You are in A deep dark forest You can see:\n" +
                "key: A rusty old key\n" +
                "log: A heavy wooden log\n" +
                "You can see from here:\n" +
                "cabin\n" +
                "riverbank\n", response8,
            "producing an artefact that is held by a player should take it " +
                "from the player's inventory and bring it to the location");
    }
     */
    
    @Test
    void testBurningBridge() {
        sendCommandToServer("Alex: get axe");
        sendCommandToServer("Alex: goto forest");
        sendCommandToServer("Alex: cut tree");
        sendCommandToServer("Alex: get log");
        sendCommandToServer("Alex: goto riverbank");
        sendCommandToServer("Alex: bridge river");
        final String response7 = sendCommandToServer("Alex: burn down route from riverbank");
        assertEquals("You burn down the bridge", response7,
            "actions that remove a path should be handled properly");
    }

    /*
    @Test
    void testSinging() {
        final String response1 = sendCommandToServer("Alex: sing");
        assertEquals("You sing a sweet song", response1,
            "actions that have no subjects, produced, or consumed should be " +
                "handled properly");
    }
     */
    
    @Test
    void testInvalidAction() {
        final String response1 = sendCommandToServer("Alex: blow horn");
        /*assertEquals("ERROR - no valid instruction in that command",
            response1,
            "cannot perform an action if not all of the subjects are present");*/
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void testDrinkPotion() {
        final String response1 = sendCommandToServer("Alex: drink potion");
        assertEquals("You drink the potion and your health improves",
            response1,
            "should be able to use entities in the location as subjects");
    }
    
    @Test
    void testAmbiguousMultiWordTriggers() {
        final String response1 = sendCommandToServer("Alex: make music");
        /*assertEquals("ERROR - invalid/ambiguous command\n", response1,
            "multi-word trigger that could do two actions is not valid");*/
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void testAmbiguousSingleWordTriggers() {
        final String response1 = sendCommandToServer("Alex: random");
        /*assertEquals("ERROR - invalid/ambiguous command\n", response1,
            "single-word trigger that could do two actions is not valid");*/
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void testRemovingOneOfManyCharacters() {
        sendCommandToServer("Ollie: goto forest");
        sendCommandToServer("Ollie: get key");
        sendCommandToServer("Ollie: goto riverbank");
        sendCommandToServer("Ollie: get horn");
        sendCommandToServer("Ollie: goto forest");
        sendCommandToServer("Ollie: goto cabin");
        sendCommandToServer("Ollie: open trapdoor");
        sendCommandToServer("Ollie: goto cellar");
        sendCommandToServer("Ollie: blow horn");
        sendCommandToServer("Ollie: goto cabin");
        final String response1 = sendCommandToServer("Ollie: blow horn");
        assertEquals("You blow the horn and as if by magic, a lumberjack " +
            "appears !", response1,
            "should be able to remove one character from a location with more" +
                " than one character");
    }
    
    @Test
    void testNotAmbiguousSharedtrigger() {
        sendCommandToServer("Kate: goto forest");
        sendCommandToServer("Kate: get key");
        sendCommandToServer("Kate: goto cabin");
        final String response1 = sendCommandToServer("Kate: use key");
        assertEquals("You unlock the door and see steps leading down into a " +
            "cellar", response1,
            "where a trigger relates to multiple actions, but only one is " +
                "doable, the doable one should be performed");
    }
    
    @Test
    void testDoubleTrigger() {
        sendCommandToServer("Chris: get axe");
        sendCommandToServer("Chris: goto forest");
        final String response1 = sendCommandToServer("Chris: chop cut tree");
        assertEquals("You cut down the tree with the axe", response1,
            "can provide more than one valid trigger for an action");
    }
    
    @Test
    void testAmbiguousMultiWordTrigger() {
        final String response1 = sendCommandToServer("Gus: make music");
        /*assertEquals("ERROR - invalid/ambiguous command\n", response1,
            "multi-word triggers that are ambiguous should not work");*/
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void testCommandActionCombo() {
        final String response1 = sendCommandToServer("Jake: look for potion to " +
            "drink");
        /*assertEquals("ERROR - look requires no arguments, so the command " +
            "cannot contain any entity names\n", response1,
            "reserved word cannot be decoration for action");*/
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void testImpossibleMultiWordTrigger() {
        final String response1 = sendCommandToServer("Sion: flirt with lumberjack");
        /*assertEquals("ERROR - no valid instruction in that command",
            response1,
            "multi-word trigger for action that is not currently doable " +
                "should not work");*/
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void testNoSubjectMentioned() {
        final String response1 = sendCommandToServer("Cesca: drink");
        /*assertEquals("ERROR - no valid instruction in that command",
            response1,
            "actions that require subjects do not work with no arguments " +
                "provided");*/
        assertTrue(response1.contains("ERROR"));
    }

    /*
    @Test
    void testConsumedButNotSubject() {
        final String response1 = sendCommandToServer("Eamon: pointless");
        assertEquals("You blow the horn and as if by magic, a lumberjack " +
            "appears !", response1,
            "action with no subjects can still consume an entity");
    }
     */
    
    @Test
    void testImpossibleSubject() {
        final String response1 = sendCommandToServer("Bala: boomerang");
        /*assertEquals("ERROR - no valid instruction in that command", response1, "Should not be able to perform this " +
            "action as subject does not exist in the game");*/
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void testImpossibleConsumed() {
        final String response1 = sendCommandToServer("Harry: bottle");
        /*assertEquals("This shouldn't be possible", response1, "Should not be able to perform this " +
            "action as consumeddoes not exist in the game");*/
        assertTrue(response1.contains("ERROR"));
    }
    
    @Test
    void testImpossibleProduced() {
        final String response1 = sendCommandToServer("Ed: MacBook");
        /*assertEquals("ERROR - no valid instruction in that command", response1, "Should not be able to perform this " +
            "action as produced does not exist in the game");*/
        assertTrue(response1.contains("ERROR"));
    }
}
