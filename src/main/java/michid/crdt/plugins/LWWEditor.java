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

package michid.crdt.plugins;

import static com.google.common.collect.Iterators.contains;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.oak.api.Type.NAMES;

import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.memory.PropertyBuilder;
import org.apache.jackrabbit.oak.spi.commit.DefaultEditor;
import org.apache.jackrabbit.oak.spi.commit.Editor;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;

/**
 * Implementation of a {@link Editor} such that nodes of type
 * {@code mix:lwwRegister} behave like a 'last writer wins' register.
 * <p>
 * The current value of the register is available via the {@code value}
 * property. Updates are recorded via the {@code update-} property.
 */
public class LWWEditor extends DefaultEditor {
    public static final String MIX_LWW_REGISTER = "mix:lwwRegister";
    public static final String MIX_LWW_REGISTER_CND = '[' + MIX_LWW_REGISTER + "]  mixin";
    public static final String LWW_UPDATE = "update-";
    public static final String LWW_VALUE = "value";

    private final NodeBuilder builder;

    private PropertyState value;

    public LWWEditor(NodeBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void leave(NodeState before, NodeState after) throws CommitFailedException {
        if (value != null) {
            builder.setProperty(value);
        }
    }

    @Override
    public void propertyAdded(PropertyState after) throws CommitFailedException {
        String name = after.getName();
        if (name.startsWith(LWW_UPDATE) && hasMixin(builder, MIX_LWW_REGISTER)) {
            Type<?> type = after.isArray() ? after.getType().getBaseType() : after.getType();
            value = PropertyBuilder.copy(type, after).setName(LWW_VALUE).getPropertyState();
            builder.removeProperty(name);
        }
    }

    @Override
    public Editor childNodeAdded(String name, NodeState after) throws CommitFailedException {
        return new LWWEditor(builder.getChildNode(name));
    }

    @Override
    public Editor childNodeChanged(String name, NodeState before, NodeState after) throws CommitFailedException {
        return new LWWEditor(builder.getChildNode(name));
    }

    private static boolean hasMixin(NodeBuilder builder, String name) {
        PropertyState mixin = builder.getProperty(JCR_MIXINTYPES);
        return mixin != null && contains(mixin.getValue(NAMES).iterator(), name);
    }
}
