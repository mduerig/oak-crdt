package michid.crdt;/*
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

import static michid.crdt.plugins.AtomicSetEditor.MIX_ATOMIC_SET;
import static michid.crdt.plugins.AtomicSetEditor.MIX_ATOMIC_SET_CND;
import static michid.crdt.plugins.LWWEditor.MIX_LWW_REGISTER;
import static michid.crdt.plugins.LWWEditor.MIX_LWW_REGISTER_CND;
import static michid.crdt.plugins.MVConflictHandler.MIX_MV_REGISTER;
import static michid.crdt.plugins.MVConflictHandler.MIX_MV_REGISTER_CND;
import static org.apache.jackrabbit.commons.cnd.CndImporter.registerNodeTypes;
import static org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore.newSegmentNodeStore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import ammonite.repl.Bind;
import ammonite.repl.Repl;
import michid.crdt.plugins.AtomicSetEditorProvider;
import michid.crdt.plugins.LWWEditorProvider;
import michid.crdt.plugins.MVConflictHandler;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.segment.memory.MemoryStore;
import scala.collection.mutable.ListBuffer;

/**
 * Main class for an interactive Scala shell. A transient JCR
 * {@link Demo#repository} is set up with {@link AtomicSetEditorProvider},
 * {@link LWWEditorProvider} and {@link MVConflictHandler}.
 */
public final class Demo {

    /**
     * Transient repository instance.
     */
    public static Repository repository = createRepository();

    private Demo() { }

    /**
     * Create a new session on {@link #repository}
     * @return  a new {@link Session} instance
     * @throws RepositoryException
     */
    public static Session newSession() throws RepositoryException {
        return newSession(repository);
    }

    private static Session newSession(Repository repository) throws RepositoryException {
        return repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    /**
     * Auxiliary function for printing the value of a {@link Property}.
     * @param property   the property to print
     * @return  a human readable string representation of {@code property}
     * @throws RepositoryException
     */
    public static String show(Property property) throws RepositoryException {
        if (property.isMultiple()) {
            return Arrays.toString(property.getValues());
        } else {
            return property.getValue().toString();
        }
    }

    private static Repository createRepository() {
        Repository repository = new Jcr(newSegmentNodeStore(new MemoryStore()).create())
                .with(new AtomicSetEditorProvider())
                .with(new LWWEditorProvider())
                .with(new MVConflictHandler())
                .createRepository();
        try {
            Session session = newSession(repository);
            Node root = session.getRootNode();
            try {
                root.addNode("count").addMixin("mix:atomicCounter");

                registerNodeType(session, MIX_ATOMIC_SET_CND);
                root.addNode("set").addMixin(MIX_ATOMIC_SET);

                registerNodeType(session, MIX_LWW_REGISTER_CND);
                root.addNode("lww").addMixin(MIX_LWW_REGISTER);

                registerNodeType(session, MIX_MV_REGISTER_CND);
                root.addNode("mv").addMixin(MIX_MV_REGISTER);

                session.save();
            } finally {
                session.logout();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return repository;
    }

    private static void registerNodeType(Session session, String cnd) throws RepositoryException, ParseException, IOException {
        registerNodeTypes(new InputStreamReader(new ByteArrayInputStream(cnd.getBytes())), session);
    }

    public static void main(String[] args) {
        Repl.debug(new ListBuffer<Bind<?>>());
    }
}
