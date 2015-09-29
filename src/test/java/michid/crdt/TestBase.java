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

import static org.apache.jackrabbit.commons.cnd.CndImporter.registerNodeTypes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.plugins.segment.memory.MemoryStore;
import org.junit.After;
import org.junit.Ignore;

@Ignore("This abstract base class does not have any tests")
public abstract class TestBase {
    private volatile Repository repository;

    @After
    public void tearDown() {
        if (repository instanceof JackrabbitRepository) {
            ((JackrabbitRepository) repository).shutdown();
        }
        repository = null;
    }

    protected Jcr initJcr(Jcr jcr) {
        return jcr;
    }

    protected final Repository getRepository() {
        if (repository == null) {
            repository = initJcr(new Jcr(SegmentNodeStore.newSegmentNodeStore(
                new MemoryStore()).create()))
                    .createRepository();
        }
        return repository;
    }

    protected final Session createAdminSession() throws RepositoryException {
        return getRepository().login(getAdminCredentials());
    }

    protected static SimpleCredentials getAdminCredentials() {
        return new SimpleCredentials("admin", "admin".toCharArray());
    }

    protected static void registerNodeType(Session session, String cnd) throws RepositoryException, ParseException, IOException {
        registerNodeTypes(new InputStreamReader(new ByteArrayInputStream(cnd.getBytes())), session);
    }

}