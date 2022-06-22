package zookeeper.grf;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import zookeeper.grf.app.TaskManager;
import zookeeper.grf.app.ZnodeTreeTraverser;

import java.io.IOException;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    enum ZCMD {
        HELP("help"),
        CONN("conn"),
        CLOSE("close"),

        LS_TASKS("ls-tasks"),
        KILL_TASKS("kill-tasks"),
        NEW_TASK("new-t"),

        CREATE("create"),
        DELETE("delete"),
        COUNT("count"),
        LS_ZNODE("ls-z"),
        LS_TREE("ls-tree"),
        LS_TREE_count("ls-tree-count"),
        ZNODE_INSIGHT("z-in");

        public final String cmd;
        ZCMD(String cmd) {
            this.cmd = cmd;
        }
    }

    private static final ArrayList<TaskManager> tasksMan = new ArrayList<>();

    private static String host_port;
    private static ZooKeeper mainZk;

    private static Watcher defaultWatcher = System.out::println;
    private static AsyncCallback.Create2Callback defCb = (rc, path, ctx, name, stat) -> {
        System.out.println("callback: " + rc + " " + path + " " + ctx + " " + name + " " + stat);
        for (TaskManager tm : tasksMan) {
            if (path.startsWith(tm.getZnode())) {
                try {
                    for (String s : ZnodeTreeTraverser.listTreeCount(mainZk, tm.getZnode())) {
                        System.out.println(s);
                    }
                } catch (InterruptedException | KeeperException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    private static AsyncCallback.VoidCallback defVCb = (rc, path, ctx) -> {
        System.out.println("callback: " + rc + " " + path + " " + ctx);
    };

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String cmd = "";
        while (!cmd.equals("exit")) {
            System.out.print("==> ");
            cmd = scanner.nextLine().trim();
            String[] cmdAndParams = cmd.split(" +");

            try {
                if (ZCMD.HELP.cmd.equals(cmd)) {
                    for (ZCMD zcmd : ZCMD.values()) {
                        System.out.println(zcmd.cmd);
                    }
                }

                else if (ZCMD.CONN.cmd.equals(cmdAndParams[0])) {
                    setupConnection(cmd, cmdAndParams);
                } else if ("close".equals(cmd)) {
                    close();
                }

                else if (ZCMD.LS_TASKS.cmd.equals(cmd)) {
                    listTasks(cmd, cmdAndParams);
                } else if (ZCMD.KILL_TASKS.cmd.equals(cmdAndParams[0])) {
                    killTask(cmd, cmdAndParams);
                } else if (ZCMD.NEW_TASK.cmd.equals(cmdAndParams[0])) {
                    newTask(cmd, cmdAndParams);
                }

                else if (ZCMD.CREATE.cmd.equals(cmdAndParams[0])) {
                    create(cmd, cmdAndParams);
                } else if (ZCMD.DELETE.cmd.equals(cmdAndParams[0])) {
                    delete(cmd, cmdAndParams);
                } else if (ZCMD.COUNT.cmd.equals(cmdAndParams[0])) {
                    System.out.println(ZnodeTreeTraverser.countDescendants(mainZk, cmdAndParams[1]));
                }  else if (ZCMD.LS_ZNODE.cmd.equals(cmdAndParams[0])) {
                    for (String s : ZnodeTreeTraverser.getChildrenWithPaths(mainZk, cmdAndParams[1])) {
                        System.out.println(s);
                    }
                } else if (ZCMD.LS_TREE.cmd.equals(cmdAndParams[0])) {
                    for (String s : ZnodeTreeTraverser.listTree(mainZk, cmdAndParams[1])) {
                        System.out.println(s);
                    }
                } else if (ZCMD.LS_TREE_count.cmd.equals(cmdAndParams[0])) {
                    for (String s : ZnodeTreeTraverser.listTreeCount(mainZk, cmdAndParams[1])) {
                        System.out.println(s);
                    }
                }

                else if (ZCMD.ZNODE_INSIGHT.cmd.equals(cmdAndParams[0])) {
                    znodeInsight(cmd, cmdAndParams);
                } else if (!"".equals(cmd)){
                    throw new RuntimeException("incorrect command");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    private static void setupConnection(String cmd, String[] cmdAndParams) throws InterruptedException {
        close();
        boolean connected = false;
        while (!connected) {
            try {
                if (cmdAndParams.length == 1) {
                    host_port = "172.200.202.2:2181";
                } else {
                    host_port = cmdAndParams[1];
                }
                mainZk = new ZooKeeper(host_port, 3000, defaultWatcher);
                connected = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void close() throws InterruptedException {
        if (mainZk != null) {
            mainZk.close();
            killAllTasks();
        }
    }

    private static void create(String cmd, String[] cmdAndParams) throws InterruptedException, KeeperException {
        mainZk.create(cmdAndParams[1],
                new byte[0],
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT,
                defCb,
                null);
    }
    private static void delete(String cmd, String[] cmdAndParams) {
        mainZk.delete(cmdAndParams[1], -1, defVCb, null);
    }
    private static void listTasks(String cmd, String[] cmdAndParams) {
        if (tasksMan.isEmpty()) {
            System.out.println("no running tasks");
        }
        for (int i = 0; i < tasksMan.size(); i++) {
            System.out.println(i + " : " + tasksMan.get(i));
        }
    }
    private static void killTask(String cmd, String[] cmdAndParams) {
        int i = Integer.parseInt(cmdAndParams[1]);
        tasksMan.get(i).stop();
    }
    private static void killAllTasks() {
        for (int i=tasksMan.size() - 1; i >= 0; i--) {
            tasksMan.get(i).stop();
        }
    }
    public static void rmTask(TaskManager tm) {
        tasksMan.remove(tm);
    }
    private static void listNodes(String cmd, String[] cmdAndParams)
            throws InterruptedException, KeeperException {
        List<String> res = mainZk.getChildren(cmdAndParams[1], false);
        System.out.println(res);
    }
    private static void znodeInsight(String cmd, String[] cmdAndParams) {
        throw new RuntimeException("not implemented");
    }

    private static void newTask(String cmd, String[] cmdAndParams) throws Exception {
        String[] exec = new String[cmdAndParams.length - 2];
        System.arraycopy(cmdAndParams, 2, exec, 0, exec.length);
        TaskManager tm = new TaskManager(host_port, cmdAndParams[1], String.join(" ", exec));
        tasksMan.add(tm);
    }
}
