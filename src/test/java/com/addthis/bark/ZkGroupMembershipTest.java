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


import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.junit.After;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ZkGroupMembershipTest {

    private static final Logger logger = LoggerFactory.getLogger(ZkGroupMembershipTest.class);

    private TestingServer myKeeper;
    private CuratorFramework myZkClient;

    @Before
    public void startKeepers() throws Exception {
        InstanceSpec spec = new InstanceSpec(null, -1, -1, -1, true, -1, 2000, 10);
        System.setProperty("zk.servers", "localhost:" + spec.getPort());
        System.setProperty("zookeeper.serverCnxnFactory", "org.apache.zookeeper.server.NettyServerCnxnFactory");
        myKeeper = new TestingServer(spec);
        myZkClient = CuratorFrameworkFactory.newClient("localhost:" + spec.getPort(), new RetryOneTime(1000));
        myZkClient.start();
    }

    @After
    public void stopKeepers() throws IOException {
//        myZkClient.close();
//        myKeeper.stop();
    }


    public class NoOpListner implements PathChildrenCacheListener {
        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            logger.debug("Listener parent path: {} currentData {}", new Object[]{event.getData().getPath(), StringSerializer.deserialize(event.getData().getData())});
        }
    }

    private List<String> testGroupMembers;

    public class SingleVarListener implements PathChildrenCacheListener {

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            logger.debug("Listener parent path: {} currentData {}", new Object[]{event.getData().getPath(), StringSerializer.deserialize(event.getData().getData())});
            switch (event.getType()) {
                case CHILD_ADDED:
                    synchronized (this) {
                        if (testGroupMembers == null) {
                            testGroupMembers = new ArrayList<>();
                        }
                        testGroupMembers.add(event.getData().getPath().substring(event.getData().getPath().lastIndexOf("/")+1));
                    }
                    break;
                case CHILD_REMOVED:
                    synchronized (this) {
                        if (testGroupMembers != null) {
                            testGroupMembers.remove(event.getData().getPath().substring(event.getData().getPath().lastIndexOf("/")+1));
                        }
                    }
                    break;
            }

        }
    }


    @Test
    public void testNoCurrentMembers() throws Exception {
        String testPath = "/foo";
        ZkGroupMembership group = new ZkGroupMembership(myZkClient);
        myZkClient.create().creatingParentsIfNeeded().forPath(testPath);
        List<String> currentMembers = group.listenToGroup(testPath, new NoOpListner());
        assertEquals(ImmutableSet.of(), ImmutableSet.copyOf(currentMembers));
    }


    @Test
    public void testSomeCurrent() throws Exception {
        String testPath = "/foo";
        ZkGroupMembership group = new ZkGroupMembership(myZkClient);
        myZkClient.create().creatingParentsIfNeeded().forPath(testPath + "/a");
        myZkClient.create().creatingParentsIfNeeded().forPath(testPath + "/b");
        List<String> currentMembers = group.listenToGroup(testPath, new NoOpListner());
        assertEquals(ImmutableSet.of("a", "b"), ImmutableSet.copyOf(currentMembers));
    }


    @Test
    public void testGroupUpdates() throws Exception {
        String testPath = "/foo";
        ZkGroupMembership group = new ZkGroupMembership(myZkClient);
        myZkClient.create().creatingParentsIfNeeded().forPath(testPath + "/a");
        List<String> currentMembers = group.listenToGroup(testPath, new SingleVarListener());
        assertEquals(ImmutableSet.of("a"), ImmutableSet.copyOf(currentMembers));
        myZkClient.create().creatingParentsIfNeeded().forPath(testPath + "/b");
        Thread.sleep(250);
        assertEquals(ImmutableSet.of("a", "b"), ImmutableSet.copyOf(testGroupMembers));
    }


    @Test
    public void testGroupRemove() throws Exception {
        String testPath = "/foo";
        ZkGroupMembership group = new ZkGroupMembership(myZkClient);
        myZkClient.create().creatingParentsIfNeeded().forPath(testPath + "/a");

        List<String> currentMembers = group.listenToGroup(testPath, new SingleVarListener());
        assertEquals(ImmutableSet.of("a"), ImmutableSet.copyOf(currentMembers));
        myZkClient.create().creatingParentsIfNeeded().forPath(testPath + "/b");
        Thread.sleep(250);
        assertEquals(ImmutableSet.of("a", "b"), ImmutableSet.copyOf(testGroupMembers));
        group.removeFromGroup(testPath, "a");
        Thread.sleep(250);
        assertEquals(ImmutableSet.of("b"), ImmutableSet.copyOf(testGroupMembers));
    }
}
