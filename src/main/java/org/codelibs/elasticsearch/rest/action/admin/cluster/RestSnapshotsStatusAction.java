/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codelibs.elasticsearch.rest.action.admin.cluster;

import org.codelibs.elasticsearch.action.admin.cluster.snapshots.status.SnapshotsStatusRequest;
import org.codelibs.elasticsearch.client.node.NodeClient;
import org.codelibs.elasticsearch.common.Strings;
import org.codelibs.elasticsearch.common.inject.Inject;
import org.codelibs.elasticsearch.common.settings.Settings;
import org.codelibs.elasticsearch.rest.BaseRestHandler;
import org.codelibs.elasticsearch.rest.RestController;
import org.codelibs.elasticsearch.rest.RestRequest;
import org.codelibs.elasticsearch.rest.action.RestToXContentListener;

import java.io.IOException;

import static org.codelibs.elasticsearch.client.Requests.snapshotsStatusRequest;
import static org.codelibs.elasticsearch.rest.RestRequest.Method.GET;

/**
 * Returns status of currently running snapshot
 */
public class RestSnapshotsStatusAction extends BaseRestHandler {

    @Inject
    public RestSnapshotsStatusAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(GET, "/_snapshot/{repository}/{snapshot}/_status", this);
        controller.registerHandler(GET, "/_snapshot/{repository}/_status", this);
        controller.registerHandler(GET, "/_snapshot/_status", this);
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        String repository = request.param("repository", "_all");
        String[] snapshots = request.paramAsStringArray("snapshot", Strings.EMPTY_ARRAY);
        if (snapshots.length == 1 && "_all".equalsIgnoreCase(snapshots[0])) {
            snapshots = Strings.EMPTY_ARRAY;
        }
        SnapshotsStatusRequest snapshotsStatusRequest = snapshotsStatusRequest(repository).snapshots(snapshots);
        snapshotsStatusRequest.ignoreUnavailable(request.paramAsBoolean("ignore_unavailable", snapshotsStatusRequest.ignoreUnavailable()));

        snapshotsStatusRequest.masterNodeTimeout(request.paramAsTime("master_timeout", snapshotsStatusRequest.masterNodeTimeout()));
        return channel -> client.admin().cluster().snapshotsStatus(snapshotsStatusRequest, new RestToXContentListener<>(channel));
    }
}