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

import java.util.List;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ZkGroupMembershipTest {

    private static final Logger logger = LoggerFactory.getLogger(ZkGroupMembershipTest.class);

    private EmbeddedZookeeper myKeeper;
    private ZkClient myZkClient;

    @Before
    public void startKeepers() throws Exception {
        System.setProperty("zk.servers", "localhost:17022");

        // todo: random port
        myKeeper = new EmbeddedZookeeper(17022);
        // todo: use standard
        myZkClient = new ZkClient("localhost:" + String.valueOf(myKeeper.getPort()), 10000, 60000, new StringSerializer());
    }

    @After
    public void stopKeepers() {
        myZkClient.close();
        myKeeper.shutdown();
    }


    public class NoOpListner implements IZkChildListener {

        public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
            logger.debug("Listener parent path: {} currentChilds {}", new Object[]{parentPath, currentChilds});
        }
    }

    private List<String> testGroupMembers;

    public class SingleVarListener implements IZkChildListener {

        public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
            logger.debug("Listener parent path: {} currentChilds {}", new Object[]{parentPath, currentChilds});
            synchronized (this) {
                testGroupMembers = currentChilds;
            }
        }
    }


    @Test
    public void testNoCurrentMembers() throws Exception {
        String testPath = "/foo";
        ZkGroupMembership group = new ZkGroupMembership(myZkClient);
        ZkHelpers.makeSurePersistentPathExists(myZkClient, testPath);
        List<String> currentMembers = group.listenToGroup(testPath, new NoOpListner());
        assertEquals(ImmutableSet.of(), ImmutableSet.copyOf(currentMembers));
    }


    @Test
    public void testSomeCurrent() throws Exception {
        String testPath = "/foo";
        ZkGroupMembership group = new ZkGroupMembership(myZkClient);
        ZkHelpers.makeSurePersistentPathExists(myZkClient, testPath);
        ZkHelpers.makeSurePersistentPathExists(myZkClient, testPath + "/a");
        ZkHelpers.makeSurePersistentPathExists(myZkClient, testPath + "/b");
        List<String> currentMembers = group.listenToGroup(testPath, new NoOpListner());
        assertEquals(ImmutableSet.of("a", "b"), ImmutableSet.copyOf(currentMembers));
    }


    @Test
    public void testGroupUpdates() throws Exception {
        String testPath = "/foo";
        ZkGroupMembership group = new ZkGroupMembership(myZkClient);
        ZkHelpers.makeSurePersistentPathExists(myZkClient, testPath);
        ZkHelpers.makeSurePersistentPathExists(myZkClient, testPath + "/a");

        List<String> currentMembers = group.listenToGroup(testPath, new SingleVarListener());
        assertEquals(ImmutableSet.of("a"), ImmutableSet.copyOf(currentMembers));
        ZkHelpers.makeSurePersistentPathExists(myZkClient, testPath + "/b");
        Thread.sleep(250);
        assertEquals(ImmutableSet.of("a", "b"), ImmutableSet.copyOf(testGroupMembers));
    }


    @Test
    public void testGroupRemove() throws Exception {
        String testPath = "/foo";
        ZkGroupMembership group = new ZkGroupMembership(myZkClient);
        ZkHelpers.makeSurePersistentPathExists(myZkClient, testPath);
        ZkHelpers.makeSurePersistentPathExists(myZkClient, testPath + "/a");

        List<String> currentMembers = group.listenToGroup(testPath, new SingleVarListener());
        assertEquals(ImmutableSet.of("a"), ImmutableSet.copyOf(currentMembers));
        ZkHelpers.makeSurePersistentPathExists(myZkClient, testPath + "/b");
        Thread.sleep(250);
        assertEquals(ImmutableSet.of("a", "b"), ImmutableSet.copyOf(testGroupMembers));
        group.removeFromGroup(testPath, "a");
        Thread.sleep(250);
        assertEquals(ImmutableSet.of("b"), ImmutableSet.copyOf(testGroupMembers));
    }
}
