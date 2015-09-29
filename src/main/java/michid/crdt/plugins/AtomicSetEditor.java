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
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterators.contains;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.oak.api.Type.NAMES;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.PropertyState;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.spi.commit.DefaultEditor;
import org.apache.jackrabbit.oak.spi.commit.Editor;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;

/**
 * Implementation of a {@link Editor} such that nodes of type
 * {@code mix:atomicSet} behave like an atomic set.
 * <p>
 * The current values of the set are available via the {@code values}
 * property. Additions and removals are recorded via the {@code add-}
 * and {@code remove-} properties, respectively.
 */
public class AtomicSetEditor extends DefaultEditor {
    public static final String MIX_ATOMIC_SET = "mix:atomicSet";
    public static final String MIX_ATOMIC_SET_CND = '[' + MIX_ATOMIC_SET + "]  mixin";
    public static final String ATOMIC_SET_ADD = "add-";
    public static final String ATOMIC_SET_REMOVE = "remove-";
    public static final String ATOMIC_SET_VALUES = "values";

    private final List<PropertyState> additions = newArrayList();
    private final List<PropertyState> deletions = newArrayList();

    private final NodeBuilder builder;
    private final boolean isAtomicSet;

    public AtomicSetEditor(NodeBuilder builder) {
        this.builder = builder;
        isAtomicSet = hasMixin(builder, MIX_ATOMIC_SET);
        if (isAtomicSet) {
            PropertyState initial = builder.getProperty(ATOMIC_SET_VALUES);
            if (initial != null) {
                additions.add(initial);
            }
        }
    }

    @Override
    public void leave(NodeState before, NodeState after) throws CommitFailedException {
        if (isAtomicSet) {
            PropertyState initial = getFirst(concat(additions, deletions), null);
            if (initial != null) {
                applyChanges(builder, ATOMIC_SET_VALUES, initial.getType(), additions, deletions);
            }
        }
    }

    @Override
    public void propertyAdded(PropertyState after) throws CommitFailedException {
        if (isAtomicSet && !after.isArray()) {
            String name = after.getName();
            if (name.startsWith(ATOMIC_SET_ADD)) {
                additions.add(after);
                builder.removeProperty(name);
            } else if (name.startsWith(ATOMIC_SET_REMOVE)) {
                deletions.add(after);
                builder.removeProperty(name);
            }
        }
    }

    @Override
    public Editor childNodeAdded(String name, NodeState after) throws CommitFailedException {
        return new AtomicSetEditor(builder.getChildNode(name));
    }

    @Override
    public Editor childNodeChanged(String name, NodeState before, NodeState after) throws CommitFailedException {
        return new AtomicSetEditor(builder.getChildNode(name));
    }

    private static <T> void applyChanges(NodeBuilder parent, String name, Type<T> type,
            List<PropertyState> additions, List<PropertyState> deletions) {
        parent.setProperty(name, getValues(type, additions, deletions), arrayType(type));
    }

    private static <T> Iterable<T> getValues(Type<T> type,
            List<PropertyState> additions, List<PropertyState> deletions) {
        Set<T> values = Sets.newHashSet();
        for (PropertyState addition : additions) {
            if (addition.isArray()) {
                addAll(values, addition.getValue(arrayType(type)));
            } else {
                values.add(addition.getValue(scalarType(type)));
            }
        }
        for (PropertyState deletion : deletions) {
            if (deletion.isArray()) {
                removeAll(values, deletion.getValue(arrayType(type)));
            } else {
                values.remove(deletion.getValue(scalarType(type)));
            }
        }
        return values;
    }

    private static <T> void removeAll(Set<T> values, Iterable<T> remove) {
        for (T r : remove) {
            values.remove(r);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Type<T> scalarType(Type<T> type) {
        if (type.isArray()) {
            return (Type<T>) type.getBaseType();
        } else {
            return type;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Type<Iterable<T>> arrayType(Type<T> type) {
        if (type.isArray()) {
            return (Type<Iterable<T>>) type;
        } else {
            return (Type<Iterable<T>>) type.getArrayType();
        }
    }

    private static boolean hasMixin(NodeBuilder builder, String name) {
        PropertyState mixin = builder.getProperty(JCR_MIXINTYPES);
        return mixin != null && contains(mixin.getValue(NAMES).iterator(), name);
    }

}
