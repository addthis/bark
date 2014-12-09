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
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

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

    private CuratorFramework myZkClient;

    private TestingServer myKeeper;

    @Before
    public void startKeepers() throws Exception {
        InstanceSpec spec = new InstanceSpec(null, -1, -1, -1, true, -1, 2000, 10);
        System.setProperty("zk.servers", "localhost:" + spec.getPort());
        System.setProperty("zookeeper.serverCnxnFactory", "org.apache.zookeeper.server.NettyServerCnxnFactory");
        myKeeper = new TestingServer(spec, true);
        myZkClient = CuratorFrameworkFactory.newClient("localhost:" + spec.getPort(), new RetryOneTime(1000));
        myZkClient.start();
    }

    @After
    public void stopKeepers() throws IOException {
        myKeeper.stop();
    }


    public class NoOpListner implements PathChildrenCacheListener {
        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            logger.debug("Listener parent path: {} currentData {}",
                    new Object[]{event.getData().getPath(), StringSerializer.deserialize(event.getData().getData())});
        }
    }

    private CopyOnWriteArraySet<String> testGroupMembers = new CopyOnWriteArraySet<>();

    public class SingleVarListener implements PathChildrenCacheListener {

        private final CyclicBarrier barrier;

        public SingleVarListener(CyclicBarrier barrier) {
            this.barrier = barrier;
        }

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            logger.debug("Listener parent path: {} currentData {}",
                    new Object[]{event.getData().getPath(), StringSerializer.deserialize(event.getData().getData())});
            switch (event.getType()) {
                case CHILD_ADDED:
                    synchronized (this) {
                        testGroupMembers.add(event.getData().getPath().substring(
                                event.getData().getPath().lastIndexOf("/")+1));
                    }
                    barrier.await(10, TimeUnit.SECONDS);
                    break;
                case CHILD_REMOVED:
                    synchronized (this) {
                        testGroupMembers.remove(event.getData().getPath().substring(
                                event.getData().getPath().lastIndexOf("/")+1));
                    }
                    barrier.await(10, TimeUnit.SECONDS);
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
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<String> currentMembers = group.listenToGroup(testPath, new SingleVarListener(barrier));
        assertEquals(ImmutableSet.of("a"), ImmutableSet.copyOf(currentMembers));
        myZkClient.create().creatingParentsIfNeeded().forPath(testPath + "/b");
        barrier.await(10, TimeUnit.SECONDS);
        barrier.await(10, TimeUnit.SECONDS);
        assertEquals(ImmutableSet.of("a", "b"), ImmutableSet.copyOf(testGroupMembers));
    }


    @Test
    public void testGroupRemove() throws Exception {
        String testPath = "/foo";
        ZkGroupMembership group = new ZkGroupMembership(myZkClient);
        myZkClient.create().creatingParentsIfNeeded().forPath(testPath + "/a");
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<String> currentMembers = group.listenToGroup(testPath, new SingleVarListener(barrier));
        assertEquals(ImmutableSet.of("a"), ImmutableSet.copyOf(currentMembers));
        myZkClient.create().creatingParentsIfNeeded().forPath(testPath + "/b");
        barrier.await(10, TimeUnit.SECONDS);
        barrier.await(10, TimeUnit.SECONDS);
        assertEquals(ImmutableSet.of("a", "b"), ImmutableSet.copyOf(testGroupMembers));
        group.removeFromGroup(testPath, "a");
        barrier.await(10, TimeUnit.SECONDS);
        assertEquals(ImmutableSet.of("b"), ImmutableSet.copyOf(testGroupMembers));
    }
}
