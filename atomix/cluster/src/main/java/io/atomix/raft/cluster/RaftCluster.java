/*
 * Copyright 2016-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package io.atomix.raft.cluster;

import io.atomix.cluster.MemberId;
import io.atomix.raft.RaftServer;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Raft server cluster API.
 *
 * <p>This class provides the view of the Raft cluster from the perspective of a single server. When
 * a {@link RaftServer RaftServer} is started, the server will form a cluster with other servers.
 * Each Raft cluster consists of some set of {@link #getMembers() members}, and each {@link
 * RaftMember} represents a single server in the cluster. Users can use the {@code Cluster} to react
 * to state changes in the underlying Raft algorithm via the various listeners.
 *
 * <p>
 *
 * <pre>{@code
 * server.cluster().onJoin(member -> {
 *   System.out.println(member.address() + " joined the cluster!");
 * });
 *
 * }</pre>
 *
 * Membership exposed via this interface is provided from the perspective of the local server and
 * may not necessarily be consistent with cluster membership from the perspective of other nodes.
 * The only consistent membership list is on the leader node.
 *
 * <h2>Cluster management</h2>
 *
 * Users can use the {@code Cluster} to manage the Raft cluster membership. Typically, servers join
 * the cluster by calling {@link RaftServer#bootstrap(MemberId...)}.
 *
 * <p>
 *
 * <pre>{@code
 * server.cluster().onJoin(member -> {
 *   member.remove().thenRun(() -> System.out.println("Removed " + member.address() + " from the cluster!"));
 * });
 *
 * }</pre>
 *
 * When a member is removed from the cluster, the configuration change removing the member will be
 * replicated to all the servers in the cluster and persisted to disk. Once a member has been
 * removed, for that member to rejoin the cluster it must fully restart and request to rejoin the
 * cluster. Cluster configurations are stored on disk.
 *
 * <p>Additionally, members can be {@link RaftMember#promote() promoted} and {@link
 * RaftMember#demote() demoted} by any other member of the cluster. When a member state is changed,
 * a cluster configuration change request is sent to the cluster leader where it's logged and
 * replicated through the Raft consensus algorithm. <em>During</em> the configuration change,
 * servers' cluster configurations will be updated. Once the configuration change is complete, it
 * will be persisted to disk on all servers and is guaranteed not to be lost even in the event of a
 * full cluster shutdown.
 */
public interface RaftCluster {

  /**
   * Bootstraps the cluster.
   *
   * <p>Bootstrapping the cluster results in a new cluster being formed with the provided
   * configuration. The initial nodes in a cluster must always be bootstrapped. This is necessary to
   * prevent split brain. If the provided configuration is empty, the local server will form a
   * single-node cluster.
   *
   * <p>Only {@link RaftMember.Type#ACTIVE} members can be included in a bootstrap configuration. If
   * the local server is not initialized as an active member, it cannot be part of the bootstrap
   * configuration for the cluster.
   *
   * <p>When the cluster is bootstrapped, the local server will be transitioned into the active
   * state and begin participating in the Raft consensus algorithm. When the cluster is first
   * bootstrapped, no leader will exist. The bootstrapped members will elect a leader amongst
   * themselves.
   *
   * <p>It is critical that all servers in a bootstrap configuration be started with the same exact
   * set of members. Bootstrapping multiple servers with different configurations may result in
   * split brain.
   *
   * <p>The {@link CompletableFuture} returned by this method will be completed once the cluster has
   * been bootstrapped, a leader has been elected, and the leader has been notified of the local
   * server's client configurations.
   *
   * @param cluster The bootstrap cluster configuration.
   * @return A completable future to be completed once the cluster has been bootstrapped.
   */
  default CompletableFuture<Void> bootstrap(final MemberId... cluster) {
    return bootstrap(Arrays.asList(cluster));
  }

  /**
   * Bootstraps the cluster.
   *
   * <p>Bootstrapping the cluster results in a new cluster being formed with the provided
   * configuration. The initial nodes in a cluster must always be bootstrapped. This is necessary to
   * prevent split brain. If the provided configuration is empty, the local server will form a
   * single-node cluster.
   *
   * <p>Only {@link RaftMember.Type#ACTIVE} members can be included in a bootstrap configuration. If
   * the local server is not initialized as an active member, it cannot be part of the bootstrap
   * configuration for the cluster.
   *
   * <p>When the cluster is bootstrapped, the local server will be transitioned into the active
   * state and begin participating in the Raft consensus algorithm. When the cluster is first
   * bootstrapped, no leader will exist. The bootstrapped members will elect a leader amongst
   * themselves.
   *
   * <p>It is critical that all servers in a bootstrap configuration be started with the same exact
   * set of members. Bootstrapping multiple servers with different configurations may result in
   * split brain.
   *
   * <p>The {@link CompletableFuture} returned by this method will be completed once the cluster has
   * been bootstrapped, a leader has been elected, and the leader has been notified of the local
   * server's client configurations.
   *
   * @param cluster The bootstrap cluster configuration.
   * @return A completable future to be completed once the cluster has been bootstrapped.
   */
  CompletableFuture<Void> bootstrap(Collection<MemberId> cluster);

  /**
   * Returns a member by ID.
   *
   * <p>The returned {@link RaftMember} is referenced by the unique {@link RaftMember#memberId()}.
   *
   * @param id The member ID.
   * @return The member or {@code null} if no member with the given {@code id} exists.
   */
  RaftMember getMember(MemberId id);

  /**
   * Returns the local cluster member.
   *
   * @return The local cluster member.
   */
  RaftMember getLocalMember();

  /**
   * Returns a collection of all cluster members.
   *
   * <p>The returned members are representative of the last configuration known to the local server.
   * Over time, the cluster configuration may change. In the event of a membership change, the
   * returned {@link Collection} will not be modified, but instead a new collection will be created.
   * Similarly, modifying the returned collection will have no impact on the cluster membership.
   *
   * @return A collection of all cluster members.
   */
  Collection<RaftMember> getMembers();
}
