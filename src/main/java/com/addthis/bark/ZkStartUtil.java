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

import org.I0Itec.zkclient.ZkClient;

import org.junit.After;
import org.junit.Before;

@SuppressWarnings("unused")
public class ZkStartUtil {

    protected EmbeddedZookeeper myKeeper;
    protected ZkClient myZkClient;

    @Before
    public void startKeepers() throws Exception {
        myKeeper = new EmbeddedZookeeper(17023);
        String keeperPort = String.valueOf(myKeeper.getPort());
        System.setProperty("zk.servers", "localhost:" + keeperPort);
        myZkClient = new ZkClient("localhost:" + keeperPort, 10000, 60000, new StringSerializer());
        onAfterZKStart();
    }

    @After
    public void stopKeepers() {
        myZkClient.close();
        myKeeper.shutdown();
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
