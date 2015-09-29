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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.Futures.allAsList;
import static michid.crdt.plugins.AtomicSetEditor.ATOMIC_SET_ADD;
import static michid.crdt.plugins.AtomicSetEditor.ATOMIC_SET_REMOVE;
import static michid.crdt.plugins.AtomicSetEditor.ATOMIC_SET_VALUES;
import static michid.crdt.plugins.AtomicSetEditor.MIX_ATOMIC_SET;
import static michid.crdt.plugins.AtomicSetEditor.MIX_ATOMIC_SET_CND;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import com.google.common.util.concurrent.ListenableFutureTask;
import michid.crdt.plugins.AtomicSetEditorProvider;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.junit.Before;
import org.junit.Test;

public class AtomicSetTest extends TestBase {
    private static final Random RND = new Random();

    @Override
    protected Jcr initJcr(Jcr jcr) {
        return jcr.with(new AtomicSetEditorProvider());
    }

    @Before
    public void setup() throws RepositoryException, IOException, ParseException {
        Session session = createAdminSession();
        try {
            registerNodeType(session, MIX_ATOMIC_SET_CND);
            Node root = session.getRootNode();
            Node set = root.addNode("set");
            set.addMixin(MIX_ATOMIC_SET);
            session.save();
        } finally {
            session.logout();
        }
    }

    @Test
    public void set() throws RepositoryException, ExecutionException, InterruptedException {
        Set<Long> expectedSet = newHashSet();
        List<ListenableFutureTask<Void>> tasks = newArrayList();
        for (int k = 0; k < 100; k ++) {
            tasks.add(updateSet(expectedSet, k, 1 + RND.nextInt(20), RND.nextBoolean()));
        }
        allAsList(tasks).get();

        Session session = createAdminSession();
        try {
            assertEquals(expectedSet, toLongs(session.getProperty("/set/" + ATOMIC_SET_VALUES).getValues()));
        } finally {
            session.logout();
        }
    }

    private static Set<Long> toLongs(Value[] values) throws RepositoryException {
        Set<Long> longs = newHashSet();
        for (Value value : values) {
            longs.add(value.getLong());
        }
        return longs;
    }

    private ListenableFutureTask<Void> updateSet(final Set<Long> expectedSet, final int id,
            final long value, final boolean remove) {
        ListenableFutureTask<Void> task = ListenableFutureTask.create(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Session session = createAdminSession();
                try {
                    Node set = session.getNode("/set");
                    if (remove) {
                        set.setProperty(ATOMIC_SET_REMOVE + id, value);
                        synchronized (expectedSet) {
                            session.save();
                            expectedSet.remove(value);
                        }
                    } else {
                        set.setProperty(ATOMIC_SET_ADD + id, value);
                        synchronized (expectedSet) {
                            session.save();
                            expectedSet.add(value);
                        }
                    }
                    return null;
                } finally {
                    session.logout();
                }
            }
        });
        new Thread(task).start();
        return task;
    }

}
