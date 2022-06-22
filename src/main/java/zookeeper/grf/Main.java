package zookeeper.grf;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import zookeeper.grf.app.TaskManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final ArrayList<TaskManager> tasksMan = new ArrayList<>();

    private static String host_port;
    private static ZooKeeper mainZk;
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String cmd = "";
        while (!cmd.equals("exit")) {
            System.out.print("==> ");
            cmd = scanner.nextLine().trim();
            String[] cmdAndParams = cmd.split(" +");


            try {
                if ("conn".equals(cmdAndParams[0])) {
                    setupConnection(cmd, cmdAndParams);
                } else if ("lst".equals(cmd)) {
                    listTasks(cmd, cmdAndParams);
                } else if ("kt".equals(cmdAndParams[0])) {
                    killTask(cmd, cmdAndParams);
                } else if ("lsz".equals(cmdAndParams[0])) {
                    listNodes(cmd, cmdAndParams);
                } else if ("desc".equals(cmdAndParams[0])) {
                    descendantsInsight(cmd, cmdAndParams);
                } else if ("new".equals(cmdAndParams[0])) {
                    newTask(cmd, cmdAndParams);
                } else if (!"".equals(cmd)){
                    throw new RuntimeException("incorrect command");
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



    private static void setupConnection(String cmd, String[] cmdAndParams) throws InterruptedException {
        if (mainZk != null) {
            mainZk.close();
        }
        boolean connected = false;
        while (!connected) {
            try {
                if (cmdAndParams.length == 1) {
                    host_port = "172.200.202.2:2181";
                } else {
                    host_port = cmdAndParams[1];
                }
                mainZk = new ZooKeeper(host_port, 3000, System.out::println);
                connected = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void listTasks(String cmd, String[] cmdAndParams) {
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
    private static void descendantsInsight(String cmd, String[] cmdAndParams) {
        throw new RuntimeException("not implemented");
    }

    private static void newTask(String cmd, String[] cmdAndParams) throws IOException {
        String[] exec = new String[cmdAndParams.length - 2];
        System.arraycopy(cmdAndParams, 2, exec, 0, exec.length);
        TaskManager tm = new TaskManager(host_port, cmdAndParams[1], String.join(" ", exec));
        tasksMan.add(tm);
    }
}


//        if (args.length < 4) {
//            System.err
//                    .println("USAGE: Executor hostPort znode program [args ...]");
//            System.exit(2);
//        }
//        String hostPort = args[0];
//        String znode = args[1];
//        String filename = args[2];
//        String[] exec = new String[args.length - 3];
//        System.arraycopy(args, 3, exec, 0, exec.length);
//        try {
//            System.out.println(Arrays.toString(args));
//            new Executor(hostPort, znode, filename, exec).run();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }