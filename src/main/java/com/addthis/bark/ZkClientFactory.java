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

import com.addthis.basis.util.Parameter;

import org.I0Itec.zkclient.ZkClient;

@SuppressWarnings("unused")
public class ZkClientFactory {

    private static final String zkHosts = Parameter.value("zk.servers", "127.0.0.1:2181");
    private static final String zkChroot = Parameter.value("zk.chroot", "");
    private static final int zkSessionTimeout = Integer.parseInt(System.getProperty("zk.sessionTimeout", "180000"));
    private static final int zkConnectionTimeout = Integer.parseInt(System.getProperty("zk.connectionTimeout", "600000"));


    // if the caller wants to close this later, that's their buisness.
    public static ZkClient makeStandardClient() {
        return new ZkClient(zkHosts + "/" + zkChroot, zkSessionTimeout, zkConnectionTimeout, new StringSerializer());
    }

    public static ZkClient makeStandardClient(String zkHosts, boolean useChroot) {
        String chroot = (useChroot) ? "/" + zkChroot : "";
        return new ZkClient(zkHosts + chroot, zkSessionTimeout, zkConnectionTimeout, new StringSerializer());
    }

    /**
     * The chroot znode *must* already exist.
     */

    public static ZkClient makeCustomChrootClient(String zkHosts, String chroot) {
        return new ZkClient(zkHosts + "/" + chroot, zkSessionTimeout, zkConnectionTimeout, new StringSerializer());
    }

    public static ZkClient makeCustomChrootClient(String chroot) {
        return new ZkClient(zkHosts + "/" + chroot, zkSessionTimeout, zkConnectionTimeout, new StringSerializer());
    }


}
