package zookeeper.grf.app;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ZnodeTreeTraverser {

    public static List<String> getChildrenWithPaths(ZooKeeper zk, String path) throws InterruptedException, KeeperException {
        List<String> children = zk.getChildren(path, false);
        List<String> childrenWithPath = new LinkedList<>();
        for (String c : children) {
            if (path.equals("/")) {
                childrenWithPath.add(String.format("/%s", c));
            } else {
                childrenWithPath.add(String.format("%s/%s", path, c));
            }
        }
        return childrenWithPath;
    }

    public static Object traversTree(ZooKeeper zk, String path, BiFunction<Object, List<String>, Object> f) throws InterruptedException, KeeperException {
        Object state = null;
        Queue<String> descendantToProcess = new LinkedList<>();
        descendantToProcess.add(path);
        while (!descendantToProcess.isEmpty()) {
            String d = descendantToProcess.poll();
            List<String> descendants = getChildrenWithPaths(zk, d);
            state = f.apply(state, descendants);
            descendantToProcess.addAll(descendants);
        }
        return state;
    }

    public static int countDescendants(ZooKeeper zk, String path) throws InterruptedException, KeeperException {
        return (int) traversTree(zk, path, (count_, descendants) -> {
            int count;
            if (count_ == null) {
                count = 0;
            } else {
                count = (int) count_;
            }
            count += descendants.size();
            return count;
        });
    }

    public static List<String> listTree(ZooKeeper zk, String path) throws InterruptedException, KeeperException {
        List<String> res__ =  (List<String>) traversTree(zk, path, (res_, descendants) -> {
            List<String> res;
            if (res_ != null) {
                res = (List<String>) res_;
            } else {
                res = new LinkedList<>();
            }
            res.addAll(descendants);
            return res;
        });
        res__.add(path);
        return res__;
    }


    public static List<String> listTreeCount(ZooKeeper zk, String path) throws InterruptedException, KeeperException {
        List<String> res__ =  (List<String>) traversTree(zk, path, (res_, descendants) -> {
            List<String> res;
            if (res_ != null) {
                res = (List<String>) res_;
            } else {
                res = new LinkedList<>();
            }
            res.addAll(descendants.stream().map((s) -> {
                try {
                    return String.format("%s :: %d", s, countDescendants(zk, s));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (KeeperException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList()));
            return res;
        });

        res__.add(path + " :: " + countDescendants(zk, path));
        return res__;
    }
}
