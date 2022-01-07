package minesweeper.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class ClientRequest {
    private final Socket socket; 
    private final String message; 
    
    
    public ClientRequest ( Socket socket, String message ) {
        this.socket = socket;
        this.message = message; 
    }
    
    public String  getMessage () {
        return message; 
    }
    
    public Socket getSocket () {
        return socket; 
    }
    
    public static void main (String [] args) {
        int num = 10;
        List<ClientRequest> requests = new ArrayList<>();
        
        
        PriorityQueue <ClientRequest> pq = new PriorityQueue <> (new Comparator <ClientRequest>() {
            @Override
            public int compare(ClientRequest o1, ClientRequest o2) {
                // TODO Auto-generated method stub
                return - o1.getMessage().compareTo(o2.getMessage());
            }
        });
        
        for (int i = 0; i < num ; i ++) {
            requests.add( new ClientRequest ( new Socket ( ), String.valueOf(i) ) );
        }
        pq.addAll(requests);
        
        while (!pq.isEmpty()) {
            System.out.println( pq.remove().getMessage());
        }
        
    }
    
}
