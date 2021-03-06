/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.controller.common;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An adapter for subscribing to updates to a config map that manages the watch.
 */
public class ConfigAdapter implements Watcher<ConfigMap> {
    private static final Logger log = LoggerFactory.getLogger(ConfigAdapter.class.getName());

    private Watch watch;
    private final OpenShiftClient openshiftClient;
    private final String configName;
    private final ConfigSubscriber configSubscriber;

    public ConfigAdapter(OpenShiftClient openshiftClient, String configName, ConfigSubscriber configSubscriber) {
        this.openshiftClient = openshiftClient;
        this.configName = configName;
        this.configSubscriber = configSubscriber;
    }

    private void configUpdated(ConfigMap configMap) {
        try {
            configSubscriber.configUpdated(configMap);
        } catch (Exception e) {
            log.warn("Error handling address config update", e);
        }

    }

    public void start() {
        ConfigMap initial = openshiftClient.configMaps().withName(configName).get();
        configUpdated(initial);
        watch = openshiftClient.configMaps().withName(configName).withResourceVersion(initial.getMetadata().getResourceVersion()).watch(this);
    }

    public void stop() {
        if (watch != null) {
            watch.close();
        }
    }

    @Override
    public void eventReceived(Action action, ConfigMap resource) {
        if (!action.equals(Action.ERROR)) {
            configUpdated(resource);
        }
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        log.info("Received onClose for watcher", cause);
    }
}
