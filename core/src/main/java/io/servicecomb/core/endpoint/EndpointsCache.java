/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.servicecomb.core.endpoint;

import io.servicecomb.core.Endpoint;
import io.servicecomb.core.Transport;
import io.servicecomb.serviceregistry.cache.CacheEndpoint;

/**
 * <一句话功能简述>
 * <功能详细描述>
 *
 * @version  [版本号, 2017年1月26日]
 * @see  [相关类/方法]
 * @since  [产品/模块版本]
 */
public class EndpointsCache extends AbstractEndpointsCache<Endpoint> {

    public EndpointsCache(String appId, String microserviceName, String microserviceVersionRule,
            String transportName) {
        super(appId, microserviceName, microserviceVersionRule, transportName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Endpoint createEndpoint(Transport transport, CacheEndpoint cacheEndpoint) {
        return new Endpoint(transport, cacheEndpoint.getEndpoint());
    }
}
