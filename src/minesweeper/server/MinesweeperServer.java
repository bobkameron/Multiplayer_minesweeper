package minesweeper.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import minesweeper.Board;

/**
 * Multiplayer Minesweeper server.
 */
public class MinesweeperServer {

    // System thread safety argument
    // TODO Problem 5

    /** Default server port. */
    private static final int DEFAULT_PORT = 4444;
    /** Maximum port number as defined by ServerSocket. */
    private static final int MAXIMUM_PORT = 65535;
    /** Default square board size. */
    private static final int DEFAULT_SIZE = 10;
    /** Default probability of a mine in any given space */
    private static final double probabilityMine = 0.25;
    // Number of threads needed to respond
    private static final int NUMBER_RESPONSE_THREADS = 10; 
    
    BlockingQueue <ClientRequest> clientRequests;
    Map <Socket, PrintWriter> clients; 
    
    private static final String TERMINATION_MSG = "terminate";

    private static final String[] HELLO_MSG = { "Welcome to Minesweeper. Players: ", " including you. Board: ",
            " columns by ", " rows. Type 'help' for help.\n" };

    // { "Welcome to Minesweeper. Board: ", " columns by ", " rows. Players: ",
    // " including you. Type 'help' for help.\n" };

    private static final String BOOM_MSG = "BOOM!\n";

    private static final String HELP_MSG = "Following commands in this game are allowed: "
            + "look(this returns a message showing the current state of the board), "
            + "dig x y(this digs at location x,y in the minefield), "
            + "flag x y(this flags the location x,y in minefield), "
            + "deflag x y(this deflags the location x,y in minefield), " + "help(this returns this same help message), "
            + "bye(this terminates the connection).\n";

    /** Socket for receiving incoming connections. */
    private final ServerSocket serverSocket;
    /** True if the server should *not* disconnect a client after a BOOM message. */
    private final boolean debug;

    /** Minesweeper board instance */
    private final Board board;

    // TODO: Abstraction function, rep invariant, rep exposure
    /*
     * Abstraction function: AF(board) = represents a minesweeper board, where
     * board.status(x,y) represents the status at that location in the board.
     * AF(clients.size()) = number of connected clients to the server. AF(debug)
     * = true if we are playing a minesweeper game where client is disconnected if
     * they dig a bomb, false otherwise (game continues one).
     * AF(clientRequests) = all of the client requests that have been made to the server. 
     * 
     * Rep invariant: serverSocket, board, clientRequests, and clients are all non-null.
     * 
     * Safety from rep exposure argument: numberConnections and debug are immutable
     * references and ADTs. serverSocket can't be reassigned and we never change the
     * socket in any method, and we never leak a reference to the socket in any
     * method. We never leak a reference to board nor to any data that is used to
     * construct board, and it is an immutable reference.
     * 
     * Thread safety argument: serverSocket, debug, and numberConnections are never
     * changed in any method and are thus immutable so thread-safe, while all
     * concurrent accesses to board are synchronized.
     * 
     * 
     */

    synchronized private void checkRep() {
        assert serverSocket != null && board != null && clientRequests != null && clients != null;
        // assert numberConnections >= 0;
    }

    public MinesweeperServer(boolean debug ) throws IOException {
        this(DEFAULT_PORT, true, Optional.empty(), 10, 10);
    }
    
    /**
     * Make a MinesweeperServer that listens for connections on port.
     * 
     * @param port  port number, requires 0 <= port <= 65535
     * @param debug debug mode flag
     * @throws IOException if an error occurs opening the server socket
     */
    public MinesweeperServer(int port, boolean debug, Optional<File> file, int sizeX, int sizeY) throws IOException {

        if (file.isPresent()) {
            BufferedReader reader = new BufferedReader(new FileReader(file.get()));
            board = new Board(reader);
        }

        else if (sizeX > 0 && sizeY > 0) {
            board = new Board(sizeX, sizeY, probabilityMine);

        } else {
            board = new Board(DEFAULT_SIZE, DEFAULT_SIZE, probabilityMine);
        }

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw ioe;
        }
        this.debug = debug;
        
        clientRequests = new LinkedBlockingQueue<>();
        clients = new HashMap<>();
        checkRep();
    }

    synchronized private String printHello() {
        return HELLO_MSG[0] + String.valueOf(clients.size()) + HELLO_MSG[1] + String.valueOf(board.getWidth())
                + HELLO_MSG[2] + String.valueOf(board.getHeight()) + HELLO_MSG[3];
    }

    synchronized private void addClientSocket(Socket socket) throws IOException {
        clients.put(socket, new PrintWriter(
                 new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)));
    }
    
    synchronized private void removeClientSocket(Socket socket) {
        clients.remove(socket);
    }
    
    private void initClientHandlerThreads() {
        for (int i = 0; i < NUMBER_RESPONSE_THREADS; i ++) {
            new Thread (new Runnable() {
                @Override 
                public void run() {
                    handleClientRequests();
                }
            }).start();
        }
    }
    
    
    /**
     * Run the server, listening for client connections and handling them. Never
     * returns unless an exception is thrown.
     * 
     * @throws IOException if the main server socket is broken (IOExceptions from
     *                     individual clients do *not* terminate serve())
     */
    public void serve() throws IOException {
        initClientHandlerThreads();
        
        while (true) {
            // handle the client
            try {
                // block until a client connects
                final Socket socket = serverSocket.accept();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            processClientRequests(socket);
                        } catch (IOException | InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();

                        }
                    }
                }).start();

            } catch (IOException ioe) {
                ioe.printStackTrace();
                throw ioe;
            }
            checkRep();
        }
    }
    
    private void handleClientRequests() {        
        while( true) {
            ClientRequest request = null; 
            try {
                request = clientRequests.take();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                continue;
            }
            
            Socket socket = request.getSocket();
            if (!clients.containsKey(socket)) continue;
            
            String requestMsg = request.getMessage();
            String response = handleRequest(requestMsg);
            PrintWriter writer = clients.get(socket);
            
            if (response.equals(TERMINATION_MSG)) {
                closeSocket (socket);
                continue;
            } else {
                writer.print(response);
                writer.flush();
            }
            if (response.equals(BOOM_MSG) && !debug) {
                closeSocket(socket);
            } 
        }
    }
    
    private synchronized void closeSocket( Socket socket) {
        try {
            socket.close();
            removeClientSocket(socket);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Handle a single client connection. Returns when client disconnects.
     * 
     * @param socket socket where the client is connected
     * @throws IOException if the connection encounters an error or terminates
     *                     unexpectedly
     * @throws InterruptedException 
     */
    private void processClientRequests(Socket socket) throws IOException, InterruptedException {
        
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
           // PrintWriter out = new PrintWriter(
             //    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                )
        {
            addClientSocket(socket);   
            PrintWriter out = clients.get(socket);
            out.print(printHello());
            out.flush();
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                if (! clients.containsKey(socket)) break;
                ClientRequest request = new ClientRequest(socket,line);
                clientRequests.put(request);
                
                /*
                String output = handleRequest(line);

                
                if (output.equals(TERMINATION_MSG)) {
                    break;
                } else {
                    out.print(output);
                    out.flush();
                }
                if (output.equals(BOOM_MSG) && !debug) {
                    break;
                }  */
                checkRep();
            }
            socket.close();
        } catch (InterruptedException | IOException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            removeClientSocket(socket);
            throw e;
        }
        checkRep();
    }

    /**
     * Handler for client input, performing requested operations and returning an
     * output message.
     * 
     * @param input message from client
     * @return message to client
     */
    synchronized private String handleRequest(String input) {
        String regex = "(look)|(help)|(bye)|" + "(dig -?\\d+ -?\\d+)|(flag -?\\d+ -?\\d+)|(deflag -?\\d+ -?\\d+)";
        if (!input.matches(regex)) {
            // invalid input
            // TODO Problem 5
            return HELP_MSG;
        }
        String[] tokens = input.split(" ");
        if (tokens[0].equals("look")) {
            return board.toString();
            // 'look' request
            // TODO Problem 5
        } else if (tokens[0].equals("help")) {
            return HELP_MSG;
            // 'help' request
            // TODO Problem 5
        } else if (tokens[0].equals("bye")) {
            return TERMINATION_MSG;
            // 'bye' request
            // TODO Problem 5
        } else {
            int x = Integer.parseInt(tokens[1]);
            int y = Integer.parseInt(tokens[2]);
            if (!(board.inbounds(x, y))) {
                return board.toString();
            } else if (tokens[0].equals("dig")) {

                boolean result = board.dig(x, y);
                if (result) {
                    return BOOM_MSG;
                } else {
                    return board.toString();
                }
                // 'dig x y' request
                // TODO Problem 5
            } else if (tokens[0].equals("flag")) {
                board.flag(x, y);
                return board.toString();
                // 'flag x y' request
                // TODO Problem 5
            } else if (tokens[0].equals("deflag")) {
                board.deflag(x, y);
                return board.toString();
                // 'deflag x y' request
                // TODO Problem 5
            }
        }
        // TODO: Should never get here, make sure to return in each of the cases above
        throw new UnsupportedOperationException();
    }

    /**
     * Start a MinesweeperServer using the given arguments.
     * 
     * <br>
     * Usage: MinesweeperServer [--debug | --no-debug] [--port PORT] [--size
     * SIZE_X,SIZE_Y | --file FILE]
     * 
     * <br>
     * The --debug argument means the server should run in debug mode. The server
     * should disconnect a client after a BOOM message if and only if the --debug
     * flag was NOT given. Using --no-debug is the same as using no flag at all.
     * <br>
     * E.g. "MinesweeperServer --debug" starts the server in debug mode.
     * 
     * <br>
     * PORT is an optional integer in the range 0 to 65535 inclusive, specifying the
     * port the server should be listening on for incoming connections. <br>
     * E.g. "MinesweeperServer --port 1234" starts the server listening on port
     * 1234.
     * 
     * <br>
     * SIZE_X and SIZE_Y are optional positive integer arguments, specifying that a
     * random board of size SIZE_X*SIZE_Y should be generated. <br>
     * E.g. "MinesweeperServer --size 42,58" starts the server initialized with a
     * random board of size 42*58.
     * 
     * <br>
     * FILE is an optional argument specifying a file pathname where a board has
     * been stored. If this argument is given, the stored board should be loaded as
     * the starting board. <br>
     * E.g. "MinesweeperServer --file boardfile.txt" starts the server initialized
     * with the board stored in boardfile.txt.
     * 
     * <br>
     * The board file format, for use with the "--file" option, is specified by the
     * following grammar:
     * 
     * <pre>
     *   FILE ::= BOARD LINE+
     *   BOARD ::= X SPACE Y NEWLINE
     *   LINE ::= (VAL SPACE)* VAL NEWLINE
     *   VAL ::= 0 | 1
     *   X ::= INT
     *   Y ::= INT
     *   SPACE ::= " "
     *   NEWLINE ::= "\n" | "\r" "\n"?
     *   INT ::= [0-9]+
     * </pre>
     * 
     * <br>
     * If neither --file nor --size is given, generate a random board of size 10x10.
     * 
     * <br>
     * Note that --file and --size may not be specified simultaneously.
     * 
     * @param args arguments as described
     */
    public static void main(String[] args) {
        // Command-line argument parsing is provided. Do not change this method.
        boolean debug = false;
        int port = DEFAULT_PORT;
        int sizeX = DEFAULT_SIZE;
        int sizeY = DEFAULT_SIZE;
        Optional<File> file = Optional.empty();

        Queue<String> arguments = new LinkedList<String>(Arrays.asList(args));
        try {
            while (!arguments.isEmpty()) {
                String flag = arguments.remove();
                try {
                    if (flag.equals("--debug")) {
                        debug = true;
                    } else if (flag.equals("--no-debug")) {
                        debug = false;
                    } else if (flag.equals("--port")) {
                        port = Integer.parseInt(arguments.remove());
                        if (port < 0 || port > MAXIMUM_PORT) {
                            throw new IllegalArgumentException("port " + port + " out of range");
                        }
                    } else if (flag.equals("--size")) {
                        String[] sizes = arguments.remove().split(",");
                        sizeX = Integer.parseInt(sizes[0]);
                        sizeY = Integer.parseInt(sizes[1]);
                        file = Optional.empty();
                    } else if (flag.equals("--file")) {
                        sizeX = -1;
                        sizeY = -1;
                        file = Optional.of(new File(arguments.remove()));
                        if (!file.get().isFile()) {
                            throw new IllegalArgumentException("file not found: \"" + file.get() + "\"");
                        }
                    } else {
                        throw new IllegalArgumentException("unknown option: \"" + flag + "\"");
                    }
                } catch (NoSuchElementException nsee) {
                    throw new IllegalArgumentException("missing argument for " + flag);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("unable to parse number for " + flag);
                }
            }
        } catch (IllegalArgumentException iae) {
            System.err.println(iae.getMessage());
            System.err.println(
                    "usage: MinesweeperServer [--debug | --no-debug] [--port PORT] [--size SIZE_X,SIZE_Y | --file FILE]");
            return;
        }

        try {
            runMinesweeperServer(debug, file, sizeX, sizeY, port);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Start a MinesweeperServer running on the specified port, with either a random
     * new board or a board loaded from a file.
     * 
     * @param debug The server will disconnect a client after a BOOM message if and
     *              only if debug is false.
     * @param file  If file.isPresent(), start with a board loaded from the
     *              specified file, according to the input file format defined in
     *              the documentation for main(..).
     * @param sizeX If (!file.isPresent()), start with a random board with width
     *              sizeX (and require sizeX > 0).
     * @param sizeY If (!file.isPresent()), start with a random board with height
     *              sizeY (and require sizeY > 0).
     * @param port  The network port on which the server should listen, requires 0
     *              <= port <= 65535.
     * @throws IOException if a network error occurs
     */
    public static void runMinesweeperServer(boolean debug, Optional<File> file, int sizeX, int sizeY, int port)
            throws IOException {

        MinesweeperServer server = new MinesweeperServer(port, debug, file, sizeX, sizeY);
        server.serve();
    }
}
