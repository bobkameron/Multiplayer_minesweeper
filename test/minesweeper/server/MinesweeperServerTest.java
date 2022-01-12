package minesweeper.server;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Test;

/**
 * TODO
 */
public class MinesweeperServerTest {

    // TODO
    private static final String LOCALHOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 4444;

    /*
     * Test cases for Server:
     * 
     * 1) Test with 1 or multiple clients connecting to the same server.
     * 
     * 2) Test with a client disconnecting with the server to see if the game still
     * goes on.
     * 
     */

    private static MinesweeperServer getServer(boolean debug) {
        try {
            return new MinesweeperServer(debug);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assert false;
        }
        return null;
    }

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // Test for Minesweeper Server
    // Test multiple clients connecting to same server
    @Test
    public void testMultipleConnections() {
        // inesweeperServer server = new MinesweeperServer
        // (MinesweeperServer.DEFAULT_PORT , false );
        // MinesweeperServer.main(new String[] {});
        MinesweeperServer server = getServer(true);

        Thread serverThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                runServer(server);

            }
        });
        serverThread.start();

        Socket client = null;
        Socket client2 = null;
        try {
            client = new Socket(LOCALHOST, DEFAULT_PORT);
            client2 = new Socket(LOCALHOST, DEFAULT_PORT);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assert false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assert false;
        }

        BufferedReader read1 = null;
        BufferedReader read2 = null;
        PrintWriter writer1 = null;
        PrintWriter writer2 = null;

        try {
            read1 = new BufferedReader(new InputStreamReader(client.getInputStream()));
            read2 = new BufferedReader(new InputStreamReader(client2.getInputStream()));
            writer1 = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
            writer2 = new PrintWriter(new OutputStreamWriter(client2.getOutputStream()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            Thread.sleep(250);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            String hello2 = read2.readLine();
            String hello1 = read1.readLine();

            assertTrue(hello1.startsWith("Welcome to Minesweeper. Players: "));
            assertTrue(hello2.startsWith("Welcome to Minesweeper. Players: "));
            if (hello2.contains("Players: 1")) {
                assert hello1.contains("Players: 2");
            } else {
                assert hello2.contains("Players: 2");
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        writer1.println("dig 0 0");
        writer1.flush();
        writer2.println("look");
        writer2.flush();
        try {

            String read = read2.readLine();
            assertTrue(!read.substring(0, 1).equals("-"));
            System.out.println(read);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static void runServer(MinesweeperServer server) {
        try {
            server.serve();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assert false;
        }
    }

}
