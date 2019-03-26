import org.jgroups.*;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.Util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class DistributedMap extends ReceiverAdapter implements SimpleStringMap {
    private JChannel channel;


    private final HashMap<String, Integer> map = new HashMap<>();

    @Override
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    @Override
    public Integer get(String key) {
        return map.getOrDefault(key, null);
    }

    @Override
    public void put(String key, Integer value) throws Exception {
        map.put(key, value);
        Map<String, Integer> tmp = new HashMap<>();
        tmp.put(key, value);
        channel.send(new Message(null, tmp));
    }

    @Override
    public Integer remove(String key) {
        return map.remove(key);
    }


    public void getState(OutputStream output) throws Exception {
        synchronized (map) {
            Util.objectToStream(map, new DataOutputStream(output));
        }
    }

    @SuppressWarnings("unchecked")
    public void setState(InputStream input) throws Exception {
        Map<String, Integer> mmap = (Map<String, Integer>) Util.objectFromStream(new DataInputStream(input));
        synchronized (map) {
            map.clear();
            map.putAll(mmap);
        }
        System.out.println("received state size: " + mmap.values().size());
    }

    public void viewAccepted(View new_view) {
        if (new_view instanceof MergeView) {
            ViewHandler handler = new ViewHandler(channel, (MergeView) new_view);
            handler.start();
        }
        System.out.println("** received view: " + new_view);
    }

    @SuppressWarnings("unchecked")
    public void receive(Message msg) {
        Map<String, Integer> line = (Map<String, Integer>) msg.getObject();
        synchronized (map) {
            map.putAll(line);
        }
    }

    private void start(String cluster_name) throws Exception {
        channel = new JChannel(false);
        ProtocolStack stack = new ProtocolStack();
        channel.setProtocolStack(stack);

        stack.addProtocol(new UDP().setValue("mcast_group_addr", InetAddress.getByName("230.100.213.55")))
                .addProtocol(new PING())
                .addProtocol(new MERGE3())
                .addProtocol(new FD_SOCK())
                .addProtocol(new FD_ALL()
                        .setValue("timeout", 12000)
                        .setValue("interval", 3000))
                .addProtocol(new VERIFY_SUSPECT())
                .addProtocol(new BARRIER())
                .addProtocol(new NAKACK2())
                .addProtocol(new UNICAST3())
                .addProtocol(new STABLE())
                .addProtocol(new GMS())
                .addProtocol(new UFC())
                .addProtocol(new MFC())
                .addProtocol(new FRAG2())
                .addProtocol(new STATE())
                .addProtocol(new SEQUENCER())
                .addProtocol(new FLUSH());

        stack.init();
        channel.setReceiver(this);
        channel.connect(cluster_name);
        channel.getState(null, 10000);
    }

    DistributedMap(String cluster_name) throws Exception {
        this.start(cluster_name);
    }

    private static class ViewHandler extends Thread {
        JChannel ch;
        MergeView view;

        private ViewHandler(JChannel ch, MergeView view) {
            this.ch = ch;
            this.view = view;
        }

        public void run() {
            Vector<View> subgroups = (Vector<View>) view.getSubgroups();
            View tmp_view = subgroups.firstElement();
            Address local_addr = ch.getAddress();
            if (!tmp_view.getMembers().contains(local_addr)) {
                System.out.println("Not member of the new primary partition ("
                        + tmp_view + "), will re-acquire the state");
                try {
                    ch.getState(null, 30000);
                } catch (Exception ignored) {}
            } else {
                System.out.println("Not member of the new primary partition ("
                        + tmp_view + "), will do nothing");
            }
        }
    }

}
