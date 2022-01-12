package minesweeper;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.spi.DirStateFactory.Result;

/**
 * TODO: Specification: Represents a minesweeper playing board, where each
 * unique position on the board starting from the top-left hand corner is
 * defined by the coordinates x,y where 0<= x< board width and 0<= y < board
 * height.
 * 
 * Any position on the board can be either flagged as containing a bomb, dug, or
 * untouched (meaning neither flagged nor dug up).
 * 
 */
public class Board {

    private final int[][] bombMap;

    private final char[][] statusMap;
    private final int width;
    private final int height;

    private final static char untouched = '-';
    private final static char flagged = 'F';

    private final static String title = "[0-9]+ [0-9]+";

    // TODO: Abstraction function, rep invariant, rep exposure, thread safety

    /*
     * Abstraction function: AF(bombMap) = the locations of all bombs in the current
     * map, where the value 0 at bombMap[x][y] indicates that there is no bomb, and
     * 1 at bombMap[x][y] indicates there is a bomb. AF(statusMap) = the status of
     * the current gameplay board, where statusMap[x][y] = '-' indicates that the
     * position is untouched, 'F' indicates flagged, and 'integer' indicates the
     * position has been dug up and indicates the # of adjacent bombs. width = width
     * of the map height = height of the map
     * 
     * Rep invariant: bombMap and statusMap are both not null and have same
     * dimensions of width * height. Every value in bombMap is either 0 or 1. For
     * every value of statusMap[x][y] that can be cast into an integer, (int)
     * statusMap[x][y] = the number of adjacent neighboring squares that have a bomb
     * in bombMap.
     * 
     * Safety from rep exposure argument: width and height are all immutable data
     * types and references.
     * 
     * bombMap and statusMap are immutable references, and they are never exposed in
     * any public methods to clients. bombMap and statusMap are created from copies
     * from the initial map input passed into the constructor, so no rep exposure
     * here.
     * 
     * Thread Safety argument: width, height, untouched, and height are all
     * immutable references and datatypes, while bombMap and statusMap are only
     * accessed from synchronized methods.
     * 
     */

    /**
     * Creates a new minesweeper board from a map of the locations of bombs.
     * 
     * @param map: a 2-d integer array where the # of columns and # of rows are
     *             greater than 0, and there are the same number of columns in each
     *             row. Each entry in map must be either 0 (no bomb) or 1 (there is
     *             a bomb).
     * 
     *             Returns a new board as described in Board spec, with location 0,0
     *             at the top-left hand corner.
     */
    public Board(int[][] map) {
        bombMap = clone(map);
        height = map.length;
        width = map[0].length;
        statusMap = initStatus();
        checkRep();
    }

    /**
     * Creates a new board with dimensions of wide by high.
     * 
     * @param wide,           must be greater than 0
     * @param high,           must be greater than 0
     * @param probabilityMine the probability of a bomb at each location in the
     *                        board.
     */
    public Board(int wide, int high, double probabilityMine) {

        height = high;
        width = wide;
        bombMap = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double result = Math.random();
                if (result < probabilityMine)
                    bombMap[y][x] = 1;
                else
                    bombMap[y][x] = 0;
            }
        }

        statusMap = initStatus();

    }

    private static boolean matchesTitle(String s) {
        return s.matches(title);
    }

    private int lineWidthLimit() {
        return (width - 1) * 2 + 1;
    }

    private boolean matchesBombMap(String line) {
        int limit = lineWidthLimit();
        if (line.length() != limit) {
            return false;
        }
        for (int i = 0; i < limit; i++) {
            char c = line.charAt(i);
            if (i % 2 == 0) {
                if (!(c == '0' || c == '1')) {
                    return false;
                }
            } else if (c != ' ') {
                return false;
            }
        }
        return true;
    }

    synchronized private void setBombMap(String line, int heightIndex) {
        for (int i = 0; i < width; i++) {
            bombMap[heightIndex][i] = Integer.valueOf(line.substring(2 * i, 2 * i + 1));
        }
    }

    /**
     * Creates a board from the reader.
     * 
     * @param reader Creates a board from the reader, following the problem set
     *               instructions. If the file is not in the right format then the
     *               method throws a runtime exception.
     */
    public Board(BufferedReader reader) {

        String titleString = null;

        try {
            titleString = reader.readLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException();
        }

        if (!matchesTitle(titleString)) {
            throw new RuntimeException();
        }

        String[] result = titleString.split(" ");

        width = Integer.valueOf(result[0]); // Integer.valueOf(titleString.substring(widthIndex, widthIndex + 1));
        height = Integer.valueOf(result[1]);

        bombMap = new int[height][width];

        for (int i = 0; i < height; i++) {
            String nextline = null;
            try {
                nextline = reader.readLine();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new RuntimeException();
            }
            if (!matchesBombMap(nextline))
                throw new RuntimeException();
            setBombMap(nextline, i);
        }

        // last line should be null
        try {
            String endline = reader.readLine();
            // System.out.println(endline + " last");
            // endline = reader.readLine();
            if (endline != null) {
                throw new RuntimeException();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        statusMap = initStatus();
        // printArray(bombMap);
        // printArray(statusMap);

    }

    synchronized private void checkRep() {
        assert bombMap.length == height && statusMap.length == height;
        for (int y = 0; y < height; y++) {
            assert bombMap[y].length == width && statusMap[y].length == width;
            for (int x = 0; x < width; x++) {
                assert bombMap[y][x] == 1 || bombMap[y][x] == 0;

                char c = statusMap[y][x];
                if (c != untouched && c != flagged) {
                    int expected = Integer.valueOf(String.valueOf(c));
                    assert expected == countSurroundingBombs(x, y);
                }
            }
        }

    }

    private char[][] initStatus() {
        char[][] result = new char[height][width];

        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)

                result[y][x] = untouched;

        return result;
    }

    private static int[][] clone(int[][] array) {
        int rows = array.length;
        int cols = array[0].length;
        int[][] result = new int[rows][cols];

        for (int x = 0; x < rows; x++) {

            for (int y = 0; y < cols; y++) {
                result[x][y] = array[x][y];
            }

        }
        return result;
    }

    /**
     * Digs at the specified x,y location of the board only if location is
     * untouched.
     * 
     * @param x : must be within board's width bounds, so 0 <= x < board width, as
     *          indicated in Board ADT spec
     * @param y : must be within board's height bounds.
     * @return true if the location at x,y was untouched and the user dug a bomb
     *         with the current method call; otherwise, return false.
     * 
     *         If the current spot has a bomb that is dug up, we get rid of the bomb
     *         at the current location and we update the neighboring counts to
     *         reflect this update. If one digs the location and no neighboring
     *         squares have bombs, recursively dig on untouched neighbors.
     * 
     */
    synchronized public boolean dig(int x, int y) {
        assert inbounds(x, y);

        if (status(x, y) != untouched)
            return false;

        boolean result = false;
        if (hasBomb(x, y)) {
            setNoBomb(x, y);
            result = true;
        }
        int bombNeighbors = countSurroundingBombs(x, y);
        setStatus(x, y, Character.forDigit(bombNeighbors, 10));

        if (bombNeighbors == 0) {
            for (int[] adj : getAdjacent(x, y)) {
                dig(adj[0], adj[1]);
            }
        }
        checkRep();
        return result;
    }

    synchronized private int countSurroundingBombs(int x, int y) {
        int counter = 0;
        for (int[] coords : getAdjacent(x, y)) {
            int wide = coords[0];
            int high = coords[1];

            if (hasBomb(wide, high))
                counter++;

        }
        return counter;
    }

    private List<int[]> getAdjacent(int x, int y) {
        int maxWide = x + 1 < width ? x + 2 : width;
        int maxHigh = y + 1 < height ? y + 2 : height;

        List<int[]> result = new ArrayList<>();
        for (int i = x - 1 >= 0 ? x - 1 : 0; i < maxWide; i++) {
            for (int j = y - 1 >= 0 ? y - 1 : 0; j < maxHigh; j++) {
                if (i != x || j != y)
                    result.add(new int[] { i, j });
            }
        }

        return result;
    }

    synchronized private boolean hasBomb(int x, int y) {
        return bombMap[y][x] == 1;
    }

    synchronized private void setNoBomb(int x, int y) {
        bombMap[y][x] = 0;
        for (int[] coords : getAdjacent(x, y)) {
            char current = status(coords[0], coords[1]);
            if (!(current == untouched || current == flagged))

                setStatus(coords[0], coords[1], (char) (current - 1));
        }
    }

    synchronized private void setStatus(int x, int y, char c) {
        statusMap[y][x] = c;
    }

    /**
     * Flag the location in the board indicated by x,y.
     * 
     * @param x must be within board width bounds
     * @param y must be within board height bounds
     * @return true if location was successfully marked with a flag, false
     *         otherwise.
     * 
     *         Only marks the location as flagged if the x,y location was untouched
     *         before calling this method.
     */
    synchronized public boolean flag(int x, int y) {
        assert inbounds(x, y);
        if (status(x, y) != untouched)
            return false;

        setStatus(x, y, flagged);
        checkRep();
        return true;
    }

    /**
     * Removes the flag from the location in the board indicated by x,y.
     * 
     * @param x must be within board width bounds
     * @param y must be within board height bounds
     * @return true if location was successfully deflagged, false otherwise.
     * 
     *         Only removes the flag from the location if the location was in a
     *         flagged state, upon which the location turns into an untouched state.
     */
    synchronized public boolean deflag(int x, int y) {
        assert inbounds(x, y);
        if (status(x, y) != flagged)
            return false;
        setStatus(x, y, untouched);

        checkRep();
        return true;
    }

    synchronized public String toString() {
        StringBuilder result = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char c = status(x, y);
                if (c != '0') {
                    result.append(c);
                } else {
                    result.append(' ');
                }
                if (x < width - 1) {
                    result.append(' ');
                }
            }
            result.append('\n');
        }
        return result.toString();
    }

    /**
     * 
     * @return the width of this board
     */
    synchronized public int getWidth() {
        return width;
    }

    /**
     * 
     * @return the height of this board
     */
    synchronized public int getHeight() {
        return height;
    }

    /**
     * 
     * @param x
     * @param y
     * @return true if x and y are valid coordinates in the board, false otherwise
     */
    synchronized public boolean inbounds(int x, int y) {
        if (0 <= x && x < getWidth() && y >= 0 && y < getHeight())
            return true;
        return false;
    }

    /**
     * Checks the status of the given position in the board
     * 
     * @param x coordinate, must be within board width bounds
     * @param y coordinate, must be within board height bounds
     * @return '-' if the square is untouched, 'F' if the state is flagged, ' ' if
     *         state is dug w/ 0 neighbors that have a bomb, otherwise 'integer' for
     *         state w/
     */
    synchronized public char status(int x, int y) {
        return statusMap[y][x];
    }

    static private void printArray(int[][] array) {
        for (int x = 0; x < array.length; x++) {
            for (int y = 0; y < array[x].length; y++) {
                System.out.print(array[x][y] + " ");
            }
            System.out.println(" ");
        }
    }

    static private void printArray(char[][] array) {
        for (int x = 0; x < array.length; x++) {
            for (int y = 0; y < array[x].length; y++) {
                System.out.print(array[x][y] + " ");
            }
            System.out.println(" ");
        }
    }

    static public void main(String args[]) {
        int[][] toCopy = { { 1, 0, 1 }, { 0, 0, 1 } };

        int[][] copied = clone(toCopy);

        toCopy[0][0] = 100;
        printArray(toCopy);
        printArray(copied);

        assert toCopy[0][0] == 100;
        assert copied[0][0] != 100;

        Board board = new Board(copied);

        printArray(board.bombMap);
        printArray(board.statusMap);

    }

}
