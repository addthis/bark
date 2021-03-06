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

import javax.annotation.Nonnull;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

@SuppressWarnings("unused")
public class ZkUtil {
    private static final String zkHosts = System.getProperty("zk.servers", "127.0.0.1:2181");
    private static final String zkChroot = System.getProperty("zk.chroot", "");
    private static final int zkSessionTimeout = Integer.parseInt(System.getProperty("zk.sessionTimeout", "600000"));
    private static final int zkConnectionTimeout = Integer.parseInt(System.getProperty("zk.connectionTimeout", "180000"));
    private static final int baseZkRetryTimeMs = Integer.parseInt(System.getProperty("zk.zkRetryTimeMs", "1000"));
    private static final int zkRetryMaxAttempts = Integer.parseInt(System.getProperty("zk.baseZkRetryMaxAttempts", "25"));


    // if the caller wants to close this later, that's their buisness.
    public static CuratorFramework makeStandardClient() {
        return makeCustomChrootClient(zkHosts, zkChroot);
    }

    public static CuratorFramework makeStandardClient(String zkHosts, boolean useChroot) {
        String chroot = (useChroot) ? "/" + zkChroot : "";
        return makeCustomChrootClient(zkHosts, chroot);
    }

    /**
     * The chroot znode *must* already exist.
     */

    public static CuratorFramework makeCustomChrootClient(String chroot) {
        return makeCustomChrootClient(zkHosts, chroot);
    }

    static String stripTrailingSlash(@Nonnull String input) {
        if (input.endsWith("/")) {
            return input.substring(0, input.length() - 1);
        } else {
            return input;
        }
    }


    public static CuratorFramework makeCustomChrootClient(String zkHosts, String chroot) {
        CuratorFramework framework = CuratorFrameworkFactory.builder()
                .sessionTimeoutMs(zkSessionTimeout)
                .connectionTimeoutMs(zkConnectionTimeout)
                .connectString(stripTrailingSlash(zkHosts + "/" + chroot))
                .retryPolicy(new ExponentialBackoffRetry(baseZkRetryTimeMs, zkRetryMaxAttempts))
                .defaultData(null)
                .build();
        framework.start();
        return framework;
    }
}
