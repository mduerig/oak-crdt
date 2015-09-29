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

import static com.google.common.collect.Sets.newHashSet;
import static michid.crdt.plugins.MVConflictHandler.MIX_MV_REGISTER;
import static michid.crdt.plugins.MVConflictHandler.MIX_MV_REGISTER_CND;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import com.google.common.collect.ImmutableSet;
import michid.crdt.plugins.MVConflictHandler;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.junit.Before;
import org.junit.Test;

public class MVTest extends TestBase {

    @Before
    public void setup() throws RepositoryException, IOException, ParseException {
        Session session = createAdminSession();
        try {
            registerNodeType(session, MIX_MV_REGISTER_CND);
            Node root = session.getRootNode();
            Node mv = root.addNode("mv");
            mv.addMixin(MIX_MV_REGISTER);
            session.save();
        } finally {
            session.logout();
        }
    }

    @Override
    protected Jcr initJcr(Jcr jcr) {
        return jcr.with(new MVConflictHandler());
    }

    @Test
    public void noConflict() throws RepositoryException, ExecutionException, InterruptedException {
        final Session s1 = newSession();

        run(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    ValueFactory vf = s1.getValueFactory();
                    s1.getNode("/mv").setProperty("value", new Value[]{vf.createValue(1)});
                    s1.save();
                } finally {
                    s1.logout();
                }
                return null;
            }
        });

        final Session s2 = newSession();
        run(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    ValueFactory vf = s2.getValueFactory();
                    s2.getNode("/mv").setProperty("value", new Value[]{vf.createValue(2)});
                    s2.save();
                } finally {
                    s2.logout();
                }
                return null;
            }
        });

        Session session = createAdminSession();
        try {
            HashSet<Long> values = newHashSet();
            for (Value value : session.getProperty("/mv/value").getValues()) {
                values.add(value.getLong());
            }
            assertEquals(ImmutableSet.of(2L), values);
        } finally {
            session.logout();
        }
    }

    @Test
    public void conflict() throws RepositoryException, ExecutionException, InterruptedException {
        final Session s1 = newSession();
        final Session s2 = newSession();
        run(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    ValueFactory vf = s1.getValueFactory();
                    s1.getNode("/mv").setProperty("value", new Value[] {vf.createValue(1)});
                    s1.save();
                } finally {
                    s1.logout();
                }
                return null;
            }
        });

        run(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    ValueFactory vf = s2.getValueFactory();
                    s2.getNode("/mv").setProperty("value", new Value[]{vf.createValue(2)});
                    s2.save();
                } finally {
                    s2.logout();
                }
                return null;
            }
        });

        Session session = createAdminSession();
        try {
            HashSet<Long> values = newHashSet();
            for (Value value : session.getProperty("/mv/value").getValues()) {
                values.add(value.getLong());
            }
            assertEquals(ImmutableSet.of(1L, 2L), values);
        } finally {
            session.logout();
        }

        final Session s3 = newSession();
        run(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    ValueFactory vf = s3.getValueFactory();
                    s3.getNode("/mv").setProperty("value", new Value[]{vf.createValue(3)});
                    s3.save();
                } finally {
                    s3.save();
                }
                return null;
            }
        });

        session = createAdminSession();
        try {
            HashSet<Long> values = newHashSet();
            for (Value value : session.getProperty("/mv/value").getValues()) {
                values.add(value.getLong());
            }
            assertEquals(ImmutableSet.of(3L), values);
        } finally {
            session.logout();
        }
    }

    private Session newSession() throws ExecutionException, InterruptedException {
        return run(new Callable<Session>() {
            @Override
            public Session call() throws Exception {
                return createAdminSession();
            }
        });
    }

    private static <T> T run(Callable<T> callable) throws InterruptedException, ExecutionException {
        FutureTask<T> task = new FutureTask<T>(callable);
        new Thread(task).start();
        return task.get();
    }

}
