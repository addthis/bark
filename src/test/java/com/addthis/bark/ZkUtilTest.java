package com.addthis.bark;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ZkUtilTest {

    @Test
    public void testStripTrailingSlash() {
        assertEquals("", ZkUtil.stripTrailingSlash(""));
        assertEquals("", ZkUtil.stripTrailingSlash("/"));
        assertEquals("/hello/world", ZkUtil.stripTrailingSlash("/hello/world"));
        assertEquals("/hello/world", ZkUtil.stripTrailingSlash("/hello/world/"));
    }

}
