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

import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterators.contains;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.oak.api.Type.NAMES;

import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.commit.PartialConflictHandler;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;

/**
 * Implementation of a {@link PartialConflictHandler} such that nodes of type
 * {@code mix:mvRegister} behave like a 'multi value' register.
 */
public class MVConflictHandler implements PartialConflictHandler {
    public static final String MIX_MV_REGISTER = "mix:mvRegister";
    public static final String MIX_MV_REGISTER_CND = '[' + MIX_MV_REGISTER + "]  mixin";

    @Override
    public Resolution addExistingProperty(NodeBuilder parent, PropertyState ours, PropertyState theirs) {
        if (hasMixin(parent, MIX_MV_REGISTER)) {
            return mergeValues(parent, ours.getName(), ours.getType(), ours, theirs);
        } else {
            return null;
        }
    }

    @Override
    public Resolution changeDeletedProperty(NodeBuilder parent, PropertyState ours) {
        if (hasMixin(parent, MIX_MV_REGISTER)) {
            return Resolution.OURS;
        } else {
            return null;
        }
    }

    @Override
    public Resolution changeChangedProperty(NodeBuilder parent, PropertyState ours, PropertyState theirs) {
        if (hasMixin(parent, MIX_MV_REGISTER)) {
            return mergeValues(parent, ours.getName(), ours.getType(), ours, theirs);
        } else {
            return null;
        }
    }

    @Override
    public Resolution deleteDeletedProperty(NodeBuilder parent, PropertyState ours) {
        if (hasMixin(parent, MIX_MV_REGISTER)) {
            return Resolution.MERGED;
        } else {
            return null;
        }
    }

    @Override
    public Resolution deleteChangedProperty(NodeBuilder parent, PropertyState theirs) {
        if (hasMixin(parent, MIX_MV_REGISTER)) {
            return Resolution.THEIRS;
        } else {
            return null;
        }
    }

    @Override
    public Resolution addExistingNode(NodeBuilder parent, String name, NodeState ours, NodeState theirs) {
        return null;
    }

    @Override
    public Resolution changeDeletedNode(NodeBuilder parent, String name, NodeState ours) {
        return null;
    }

    @Override
    public Resolution deleteChangedNode(NodeBuilder parent, String name, NodeState theirs) {
        return null;
    }

    @Override
    public Resolution deleteDeletedNode(NodeBuilder parent, String name) {
        return null;
    }

    private static boolean hasMixin(NodeBuilder builder, String name) {
        PropertyState mixin = builder.getProperty(JCR_MIXINTYPES);
        return mixin != null && contains(mixin.getValue(NAMES).iterator(), name);
    }

    private static <T> Resolution mergeValues(NodeBuilder parent, String name, Type<T> type, PropertyState... ps) {
        parent.setProperty(name, getValues(type, ps), arrayType(type));
        return Resolution.MERGED;
    }

    private static <T> Iterable<T> getValues(Type<T> type, PropertyState... ps) {
        Set<T> values = Sets.newHashSet();
        for (PropertyState p : ps) {
            if (p.isArray()) {
                addAll(values, p.getValue(arrayType(type)));
            } else {
                values.add(p.getValue(type));
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private static <T> Type<Iterable<T>> arrayType(Type<T> type) {
        if (type.isArray()) {
            return (Type<Iterable<T>>) type;
        } else {
            return (Type<Iterable<T>>) type.getArrayType();
        }
    }

}
