package minesweeper;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * TODO: Description
 */
public class BoardTest {

    // TODO: Testing strategy
    /*
     * Testing strategy for the input:
     * 
     * Partition the constructor: The board has all bombs, no bombs, and a mix of no
     * bombs and some bombs.
     * 
     * Partition getwidth and getheight: width/height is =1 or > 1, width = height
     * and width != height
     * 
     * Partition for flag: Try to flag locations that were previously untouched,
     * touched, or dug up.
     * 
     * Partition for unflag: Same partition as flag
     * 
     * Partition for status: Get the status of a flagged, unflagged, dug up spot (
     * both cases of spots that have adjacent neighbors with bombs and those that do
     * not), and untouched spot.
     * 
     * Partition for dig: Dig at a spot that is flagged, unflagged, previously dug
     * up, and untouched. Make sure we recursively dig more spots if we dig a spot
     * w/ neighbors w/out bombs.
     * 
     * Test each case at least once.
     *
     */

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // TODO: Tests
    final static int[][] nobombloc = { { 0 } };
    final static int[][] allbombloc = { { 1, 1, 1, 1 } };

    final static int[][] mix = { { 0, 0, 1 }, { 0, 0, 0 }, { 1, 0, 1 } };

    final static char untouched = '-';
    final static char flagged = 'F';

    @Test
    public void testNoBombs() {
        Board board = new Board(nobombloc);
        assertTrue(board.getWidth() == 1 && 1 == board.getHeight());
        assertEquals(board.status(0, 0), untouched);
        assertEquals(board.flag(0, 0), true);
        assertEquals(board.flag(0, 0), false);
        assertEquals(board.status(0, 0), flagged);

        assertEquals(board.dig(0, 0), false);

        assertEquals(board.deflag(0, 0), true);
        assertEquals(board.deflag(0, 0), false);
        assertEquals(board.status(0, 0), untouched);
        assertEquals(board.dig(0, 0), false);

        assertEquals(board.dig(0, 0), false);

        assertEquals('0', board.status(0, 0));

    }

    @Test
    public void testSomeBombs() {
        Board board = new Board(mix);
        assertEquals(board.deflag(0, 0), false);
        assertEquals(board.flag(0, 0), true);
        assertEquals(board.dig(0, 0), false);
        assertEquals(flagged, board.status(0, 0));

        assertEquals(true, board.deflag(0, 0));
        assertEquals(false, board.dig(0, 0));
        assertEquals(board.status(0, 0), '0');
        assertEquals(board.status(1, 0), '1');
        assertEquals(board.status(1, 1), '3');
        assertEquals(board.status(0, 1), '1');
        assertEquals(untouched, board.status(2, 1));

        assertEquals(true, board.dig(2, 2));

        assertEquals('0', board.status(0, 0));
        assertEquals('0', board.status(2, 2));
        assertEquals('1', board.status(2, 1));
        assertEquals('1', board.status(1, 2));

        char c = board.status(1, 1);
        System.out.println("this " + c);
        assertEquals('2', board.status(1, 1));   
        
        System.out.println ( Integer.valueOf(String.valueOf('2')) );
        
    }
}
