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

import static org.apache.jackrabbit.oak.plugins.atomic.AtomicCounterEditor.PROP_COUNTER;
import static org.apache.jackrabbit.oak.plugins.atomic.AtomicCounterEditor.PROP_INCREMENT;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFutureTask;
import org.junit.Before;
import org.junit.Test;

public class AtomicCounterTest extends TestBase {
    private static final Random RND = new Random();

    @Before
    public void setup() throws RepositoryException {
        Session session = createAdminSession();
        try {
            Node root = session.getRootNode();
            Node counter = root.addNode("counter");
            counter.addMixin("mix:atomicCounter");
            session.save();
        } finally {
            session.logout();
        }
    }

    @Test
    public void counter() throws RepositoryException, ExecutionException, InterruptedException {
        List<ListenableFutureTask<Long>> tasks = Lists.newArrayList();
        for (int k = 0; k < 100; k ++) {
            tasks.add(updateCounter(RND.nextInt(21 - 10)));
        }

        long expectedCount = 0;
        for (ListenableFutureTask<Long> task : tasks) {
            expectedCount += task.get();
        }

        Session session = createAdminSession();
        try {
            assertEquals(expectedCount, session.getProperty("/counter/" + PROP_COUNTER).getLong());
        } finally {
            session.logout();
        }
    }

    private ListenableFutureTask<Long> updateCounter(final long delta) {
        ListenableFutureTask<Long> task = ListenableFutureTask.create(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                Session session = createAdminSession();
                try {
                    session.getNode("/counter").setProperty(PROP_INCREMENT, delta);
                    session.save();
                    return delta;
                } finally {
                    session.logout();
                }
            }
        });
        new Thread(task).start();
        return task;
    }

}
