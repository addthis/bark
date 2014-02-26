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
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.QuorumConfigBuilder;
import org.apache.curator.test.TestingServer;
import org.apache.curator.test.TestingZooKeeperServer;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;

@SuppressWarnings("unused")
public class ZkStartUtil {

    protected TestingServer myKeeper;
    protected CuratorFramework zkClient;

    @Before
    public void startKeepers() throws Exception {
        InstanceSpec spec = new InstanceSpec(null, -1, -1, -1, true, -1, 2000, 10);
        System.setProperty("zookeeper.serverCnxnFactory", "org.apache.zookeeper.server.NettyServerCnxnFactory");
        myKeeper = new TestingServer(spec);
        String keeperPort = String.valueOf(spec.getPort());
        System.setProperty("zk.servers", "localhost:" + keeperPort);
        zkClient = CuratorFrameworkFactory.builder()
                .sessionTimeoutMs(60000)
                .connectionTimeoutMs(10000)
                .connectString("localhost:" + keeperPort)
                .retryPolicy(new RetryOneTime(1000))
                .defaultData(null)
                .build();
        zkClient.start();
        onAfterZKStart();
    }

    @After
    public void stopKeepers() throws IOException {
        zkClient.close();
        myKeeper.stop();
        onAfterZKStop();
    }

    /**
     * subclasses can override as needed
     */
    protected void onAfterZKStart() {

    }

    /**
     * subclasses can override as needed
     */
    protected void onAfterZKStop() {

    }
}
