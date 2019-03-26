import org.jgroups.protocols.UDP;

import java.io.*;
import java.net.InetAddress;

public class Client {

    private SimpleStringMap map;

    public Client() throws Exception {
        map = new DistributedMap("ala_cluster");
    }

    public void run() throws Exception {
        System.out.println("Simple application using distributed map");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String key;
        String value;
        while (true) {
            String command = in.readLine();
            switch (command) {
                case "get":
                    System.out.print("key? ");
                    key = in.readLine().toLowerCase();
                    System.out.flush();
                    System.out.println(map.get(key));
                    break;
                case "put":
                    System.out.print("key> ");
                    key = in.readLine().toLowerCase();
                    System.out.print("value> ");
                    value = in.readLine().toLowerCase();
                    System.out.flush();
                    map.put(key, Integer.parseInt(value));
                    break;
                case "remove":
                    System.out.print("key? ");
                    key = in.readLine().toLowerCase();
                    System.out.flush();
                    map.remove(key);
                    break;
                case "contains":
                    System.out.print("key? ");
                    key = in.readLine().toLowerCase();
                    System.out.flush();
                    if (map.containsKey(key)) {System.out.println("да");} else {System.out.println("нет");}
                    break;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Client().run();
    }
}
