package zookeeper.grf.app;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import zookeeper.grf.Main;

import java.io.IOException;

public class TaskManager implements Runnable, Watcher, AsyncCallback.StatCallback, AsyncCallback.VoidCallback {

    private String hostPort;
    private Process child;
    private ZooKeeper zk;
    private String exec;
    private String znode;
    private boolean end;

    private Thread runTh;


    public TaskManager(String hostPort, String znode, String exec) throws IOException, InterruptedException, KeeperException {
        this.exec = exec;
        this.hostPort = hostPort;
        this.znode = znode;
        this.end = false;
        this.zk = new ZooKeeper(hostPort, 3000, this);
        zk.addWatch(znode, this, AddWatchMode.PERSISTENT_RECURSIVE);
        runTh = new Thread(this);
        runTh.start();
    }

    @Override
    public void run() {
        zk.exists(znode, true, this, null);
        try {
            while (!end) {
                synchronized (this) {
                    wait();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        boolean exists = false;
        switch (rc) {
            case KeeperException.Code.Ok:
                exists = true;
                break;
            case KeeperException.Code.NoNode:
                break;
            case KeeperException.Code.SessionExpired:
            case KeeperException.Code.NoAuth:
                end = true;
                return;
            default:
                zk.exists(znode, true, this, null);
                return;
        }
        manageProcess(exists);
    }


    @Override
    public void process(WatchedEvent event) {
        String path = event.getPath();
        System.out.println(event);
        if (event.getType() == Event.EventType.None) {
            switch (event.getState()) {
                case SyncConnected:
                    break;
                case Expired:
                    end = true;
                    break;
            }
        } else {
            if (path != null && path.equals(znode)) {
                zk.exists(znode, true, this, null);
            }
        }
    }


    public void manageProcess(boolean ex) {
        if (!ex) {
            if (child != null) {
                System.out.println("Killing child");
                child.destroy();
                try {
                    child.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                child = null;
            }
        } else {
            if (child != null) {
                System.out.println("Rerun child");

                child.destroy();
                try {
                    child.waitFor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                child = Runtime.getRuntime().exec(exec);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        try {
            System.out.println("stopping: " + this);
            end = true;
            synchronized (this) {
                notifyAll();
            }
            Main.rmTask(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "TaskManager{" +
                "hostPort='" + hostPort + '\'' +
                ", child=" + child +
//                ", zk=" + zk +
                ", exec='" + exec + '\'' +
                ", znode='" + znode + '\'' +
                ", end=" + end +
                '}';
    }

    @Override
    public void processResult(int rc, String path, Object ctx) {
        System.out.println("task manages void callback");
        System.out.println(rc);
        System.out.println(path);
        System.out.println(ctx);
    }
}
