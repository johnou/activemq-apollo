/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.apollo.broker.wildcard;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.activemq.apollo.broker.Destination;

/**
 * A Map-like data structure allowing values to be indexed by
 * {@link Destination} and retrieved by destination - supporting both *
 * and &gt; style of wildcard as well as composite destinations. <br>
 * This class assumes that the index changes rarely but that fast lookup into
 * the index is required. So this class maintains a pre-calculated index for
 * destination steps. So looking up the values for "TEST.*" or "*.TEST" will be
 * pretty fast. <br>
 * Looking up of a value could return a single value or a List of matching
 * values if a wildcard or composite destination is used.
 * 
 * @version $Revision: 1.3 $
 */
public class DestinationMap<Value> {
    protected static final String ANY_DESCENDENT = DestinationFilter.ANY_DESCENDENT;
    protected static final String ANY_CHILD = DestinationFilter.ANY_CHILD;

    private DestinationMapNode<Value> root = new DestinationMapNode<Value>(null);

    /**
     * Looks up the value(s) matching the given Destination key. For simple
     * destinations this is typically a List of one single value, for wild cards
     * or composite destinations this will typically be a List of matching
     * values.
     * 
     * @param key the destination to lookup
     * @return a List of matching values or an empty list if there are no
     *         matching values.
     */
    public synchronized Set<Value> get(Destination key) {
    	Collection<Destination> destinations = key.getDestinations();
        if (destinations!=null) {
        	HashSet<Value> answer = new HashSet<Value>(destinations.size());
        	for (Destination childDestination : destinations) {
                answer.addAll(get(childDestination));
            }
            return answer;
        }
        return findWildcardMatches(key);
    }

    public synchronized void put(Destination key, Value value) {
    	Collection<Destination> destinations = key.getDestinations();
        if (destinations!=null) {
        	for (Destination childDestination : destinations) {
                put(childDestination, value);
            }
            return;
        }
        String[] paths = DestinationPath.getDestinationPaths(key);
        getRootNode(key).add(paths, 0, value);
    }

    /**
     * Removes the value from the associated destination
     */
    public synchronized void remove(Destination key, Value value) {
    	Collection<Destination> destinations = key.getDestinations();
        if (destinations!=null) {
        	for (Destination childDestination : destinations) {
                remove(childDestination, value);
            }
            return;
        }
        String[] paths = DestinationPath.getDestinationPaths(key);
        getRootNode(key).remove(paths, 0, value);

    }

    public DestinationMapNode<Value> getRootNode() {
        return root;
    }

    // Implementation methods
    // -------------------------------------------------------------------------

    /**
     * A helper method to allow the destination map to be populated from a
     * dependency injection framework such as Spring
     */
    @SuppressWarnings("unchecked")
	protected void setEntries(List<DestinationMapEntry> entries) {
    	for (DestinationMapEntry entry : entries) {
            put(entry.getDestination(), (Value) entry);
        }
    }

    protected Set<Value> findWildcardMatches(Destination key) {
        String[] paths = DestinationPath.getDestinationPaths(key);
        HashSet<Value> answer = new HashSet<Value>();
        getRootNode(key).appendMatchingValues(answer, paths, 0);
        return answer;
    }

    /**
     * @param key
     * @return
     */
    public Set<Value> removeAll(Destination key) {
    	HashSet<Value> rc = new HashSet<Value>();
    	Collection<Destination> destinations = key.getDestinations();
        if (destinations!=null) {
        	for (Destination childDestination : destinations) {
                rc.addAll(removeAll(childDestination));
            }
            return rc;
        }
        String[] paths = DestinationPath.getDestinationPaths(key);
        getRootNode(key).removeAll(rc, paths, 0);
        return rc;
    }

    /**
     * Returns the value which matches the given destination or null if there is
     * no matching value. If there are multiple values, the results are sorted
     * and the last item (the biggest) is returned.
     * 
     * @param destination the destination to find the value for
     * @return the largest matching value or null if no value matches
     */
    public Value chooseValue(Destination destination) {
        Set<Value> set = get(destination);
        if (set == null || set.isEmpty()) {
            return null;
        }
        SortedSet<Value> sortedSet = new TreeSet<Value>(set);
        return sortedSet.last();
    }

    /**
     * Returns the root node for the given destination type
     */
    protected DestinationMapNode<Value> getRootNode(Destination key) {
        return root;
    }
}