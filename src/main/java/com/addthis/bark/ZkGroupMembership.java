/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.bark;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZkGroupMembership {

    private static final Logger log = LoggerFactory.getLogger(ZkGroupMembership.class);

    private static final int zkConnectionTimeout = Integer.parseInt(System.getProperty("zk.connectionTimeout", "60000"));
    private static final int RETRYS = 5;

    private CuratorFramework zkClient;
    private final boolean ephemeral;

    public ZkGroupMembership(CuratorFramework zkClient) {
        this(zkClient, true);
    }

    public ZkGroupMembership(CuratorFramework zkClient, boolean ephemeral) {
        this.zkClient = zkClient;
        this.ephemeral = ephemeral;
    }

    @SuppressWarnings("unused")
    public void addToGroup(String parentPath, String member) {
        addToGroup(parentPath, member, "", null); // todo: null?
    }

    @SuppressWarnings("unused")
    public void addToGroup(String parentPath, String member, AtomicBoolean shutdown) {
        addToGroup(parentPath, member, "", shutdown); // todo: null?
    }

    @SuppressWarnings("unused")
    public void addToGroup(String parentPath, String member, String data) {
        addToGroup(parentPath, member, data, null);
    }

    public void addToGroup(String parentPath, String member, String data, AtomicBoolean shutdown) {
        String path = parentPath + "/" + member;
        int remaining = zkConnectionTimeout;

        // try at most RETRYS times to replace existing node
        try {
            while (zkClient.checkExists().forPath(path) != null && (shutdown == null || !shutdown.get())) {
                if (remaining <= 0) {
                    throw new RuntimeException("cannot overwrite existing path: " + path);
                }
                log.info("[group.add] path already exists, retrying: {}", path);
                remaining -= zkConnectionTimeout / RETRYS;
                try {
                    Thread.sleep(zkConnectionTimeout / RETRYS);
                } catch (Exception ex) {
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("[group.add] unexpected exception adding to group: " + path, e);
        }

        if (shutdown != null && shutdown.get()) {
            return;
        }
        try {
            try {
                if (ephemeral) {
                    zkClient.create().withMode(CreateMode.EPHEMERAL).forPath(path, StringSerializer.serialize(data));
                } else {
                    zkClient.create().forPath(path, StringSerializer.serialize(data));
                }
            } catch (KeeperException.NodeExistsException e) {
                zkClient.setData().forPath(path, StringSerializer.serialize(data));
            }
        } catch (Exception e) {
            throw new RuntimeException("[group.add] unable to create node", e);
        }

    }


    public void removeFromGroup(String parentPath, String member) {
        try {
            zkClient.delete().deletingChildrenIfNeeded().forPath(parentPath + "/" + member);
        } catch (Exception e) {
            throw new RuntimeException("unable to remove: " + member + " from group", e);
        }
    }

    public List<String> listenToGroup(String parentPath, PathChildrenCacheListener listener) throws Exception {
        return listenToGroup(parentPath, listener, false);
    }

    public List<String> listenToGroup(String parentPath, PathChildrenCacheListener listener, boolean cacheData) throws Exception {
        PathChildrenCache cache = new PathChildrenCache(zkClient, parentPath, cacheData);
        cache.getListenable().addListener(listener);
        cache.start();
        return zkClient.getChildren().forPath(parentPath);
    }
}
