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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkGroupMembership {

    private static final Logger log = LoggerFactory.getLogger(ZkGroupMembership.class);

    private static final int zkConnectionTimeout = Integer.parseInt(System.getProperty("zk.connectionTimeout", "60000"));
    private static final int RETRYS = 5;

    private ZkClient zkClient;
    private final boolean ephemeral;

    public ZkGroupMembership(ZkClient zkClient) {
        this(zkClient, true);
    }

    public ZkGroupMembership(ZkClient zkClient, boolean ephemeral) {
        this.zkClient = zkClient;
        this.ephemeral = ephemeral;
    }

    public void addToGroup(String parentPath, String member) {
        addToGroup(parentPath, member, "", null); // todo: null?
    }

    public void addToGroup(String parentPath, String member, AtomicBoolean shutdown) {
        addToGroup(parentPath, member, "", shutdown); // todo: null?
    }

    public void addToGroup(String parentPath, String member, String data) {
        addToGroup(parentPath, member, data, null);
    }

    public void addToGroup(String parentPath, String member, String data, AtomicBoolean shutdown) {
        String path = parentPath + "/" + member;
        int remaining = zkConnectionTimeout;

        // try at most RETRYS times to replace existing node
        while (zkClient.exists(path) && (shutdown == null || !shutdown.get())) {
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

        if (shutdown != null && shutdown.get()) {
            return;
        }

        if (ephemeral) {
            zkClient.createEphemeral(path, data);
        } else {
            zkClient.createPersistent(path, data);
        }
    }


    public void removeFromGroup(String parentPath, String member) {
        zkClient.deleteRecursive(parentPath + "/" + member);
    }


    // Note that the caller is responsible for what to do about
    // listener's after a reconnect.  ZkGroupMembership can not solve
    // that for you.
    public List<String> listenToGroup(String parentPath, IZkChildListener listener) {
        return zkClient.subscribeChildChanges(parentPath, listener);
    }
}
