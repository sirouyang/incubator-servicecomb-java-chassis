/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.servicecomb.authentication.provider;

import io.servicecomb.core.Const;
import io.servicecomb.core.Handler;
import io.servicecomb.core.Invocation;
import io.servicecomb.swagger.invocation.AsyncResponse;
import io.servicecomb.swagger.invocation.context.HttpStatus;
import io.servicecomb.swagger.invocation.exception.InvocationException;

public class ProviderAuthHanlder implements Handler {

  private RSAProviderTokenManager authenticationTokenManager = new RSAProviderTokenManager();

  @Override
  public void handle(Invocation invocation, AsyncResponse asyncResp) throws Exception {

    String token = invocation.getContext(Const.AUTH_TOKEN);
    if (null != token && authenticationTokenManager.valid(token)) {
      invocation.next(asyncResp);
    } else {
      asyncResp.producerFail(new InvocationException(new HttpStatus(401, "UNAUTHORIZED"), "UNAUTHORIZED"));
    }

  }

}