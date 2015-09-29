/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package michid.crdt;

import static michid.crdt.plugins.LWWEditor.LWW_UPDATE;
import static michid.crdt.plugins.LWWEditor.LWW_VALUE;
import static michid.crdt.plugins.LWWEditor.MIX_LWW_REGISTER;
import static michid.crdt.plugins.LWWEditor.MIX_LWW_REGISTER_CND;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import michid.crdt.plugins.LWWEditorProvider;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.junit.Before;
import org.junit.Test;

public class LWWTest extends TestBase {

    @Before
    public void setup() throws RepositoryException, IOException, ParseException {
        Session session = createAdminSession();
        try {
            registerNodeType(session, MIX_LWW_REGISTER_CND);
            Node root = session.getRootNode();
            Node lww = root.addNode("lww");
            lww.addMixin(MIX_LWW_REGISTER);
            session.save();
        } finally {
            session.logout();
        }
    }

    @Override
    protected Jcr initJcr(Jcr jcr) {
        return jcr.with(new LWWEditorProvider());
    }

    @Test
    public void noConflict() throws RepositoryException {
        Session s1 = createAdminSession();
        try {
            s1.getNode("/lww").setProperty(LWW_UPDATE + '1', 1);
            s1.save();
        } finally {
            s1.logout();
        }

        Session s2 = createAdminSession();
        try {
            s2.getNode("/lww").setProperty(LWW_UPDATE + '2', 2);
            s2.save();
        } finally {
            s2.logout();
        }

        Session session = createAdminSession();
        try {
            assertEquals(2L, session.getProperty("/lww/" + LWW_VALUE).getLong());
        } finally {
            session.logout();
        }
    }

    @Test
    public void conflict() throws RepositoryException {
        Session s1 = createAdminSession();
        Session s2 = createAdminSession();
        try {
            s1.getNode("/lww").setProperty(LWW_UPDATE + '1', "one");
            s1.save();

            s2.getNode("/lww").setProperty(LWW_UPDATE + '2', "two");
            s2.save();
        } finally {
            s1.logout();
            s2.logout();
        }

        Session session = createAdminSession();
        try {
            assertEquals("two", session.getProperty("/lww/" + LWW_VALUE).getString());
        } finally {
            session.logout();
        }
    }

}
