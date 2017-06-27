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

package io.servicecomb.common.rest.definition;

import io.servicecomb.common.rest.codec.produce.ProduceProcessor;
import io.servicecomb.common.rest.codec.produce.ProduceProcessorManager;
import io.servicecomb.common.rest.definition.path.PathRegExp;
import io.servicecomb.common.rest.definition.path.URLPathBuilder;
import io.servicecomb.core.definition.OperationMeta;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestOperationMeta {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestOperationMeta.class);

    private static final String ACCEPT_TYPE_SEPARATER = ",";

    private static final String ACCEPT_TYPE_INNER_SEPARATER = ";";

    private static final String MEDIATYPE_INNER_SLASH = "/";

    protected OperationMeta operationMeta;

    protected List<String> produces;

    protected List<RestParam> paramList = new ArrayList<>();

    // key为参数名
    protected Map<String, RestParam> paramMap = new LinkedHashMap<>();

    // key为数据类型，比如json之类
    private Map<String, ProduceProcessor> produceProcessorMap = new HashMap<>();

    // 不一定等于mgr中的default，因为本operation可能不支持mgr中的default
    private ProduceProcessor defaultProcessor;

    protected String absolutePath;

    protected PathRegExp absolutePathRegExp;

    // 快速构建URL path
    private URLPathBuilder pathBuilder;

    public void init(OperationMeta operationMeta) {
        this.operationMeta = operationMeta;

        Swagger swagger = operationMeta.getSchemaMeta().getSwagger();
        Operation operation = operationMeta.getSwaggerOperation();
        this.produces = operation.getProduces();
        if (produces == null) {
            this.produces = swagger.getProduces();
        }

        setAbsolutePath(concatPath(swagger.getBasePath(), operationMeta.getOperationPath()));

        this.createProduceProcessors();

        Method method = operationMeta.getMethod();
        Type[] genericParamTypes = method.getGenericParameterTypes();
        if (genericParamTypes.length != operation.getParameters().size()) {
            throw new Error("Param count is not equal between swagger and method,  path=" + absolutePath);
        }

        // 初始化所有rest param
        for (int idx = 0; idx < genericParamTypes.length; idx++) {
            Parameter parameter = operation.getParameters().get(idx);
            Type genericParamType = genericParamTypes[idx];

            RestParam param = new RestParam(idx, parameter, genericParamType);
            addParam(param);
        }

        this.pathBuilder = new URLPathBuilder(absolutePath, paramMap);
    }

    public void setOperationMeta(OperationMeta operationMeta) {
        this.operationMeta = operationMeta;
    }

    // 输出b/c/形式的url
    private String concatPath(String basePath, String operationPath) {
      return ("/" + nonNullify(basePath) + "/" + nonNullify(operationPath) + "/")
            .replaceAll("/{2,}", "/");
    }

  private String nonNullify(String path) {
        return path == null ? "" : path;
    }

  public String getAbsolutePath() {
        return this.absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
        this.absolutePathRegExp = createPathRegExp(absolutePath);
    }

    public PathRegExp getAbsolutePathRegExp() {
        return this.absolutePathRegExp;
    }

    public boolean isAbsoluteStaticPath() {
        return this.absolutePathRegExp.isStaticPath();
    }

    protected PathRegExp createPathRegExp(String path) {
        if (path == null || path.equals("")) {
            throw new Error("null rest url is not supported");
        }
        try {
            return new PathRegExp(path);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    public RestParam getParamByName(String name) {
        return paramMap.get(name);
    }

    public RestParam getParamByIndex(int index) {
        return paramList.get(index);
    }

    public OperationMeta getOperationMeta() {
        return operationMeta;
    }

    // 为operation创建支持的多种produce processor
    protected void createProduceProcessors() {
        if (null == produces || produces.isEmpty()) {
            for (ProduceProcessor processor : ProduceProcessorManager.INSTANCE.values()) {
                this.produceProcessorMap.put(processor.getName(), processor);
            }
            return;
        }
        for (String produce : produces) {
            ProduceProcessor processor = ProduceProcessorManager.INSTANCE.findValue(produce);
            if (processor == null) {
                LOGGER.error("produce {} is not supported", produce);
                continue;
            }
            this.produceProcessorMap.put(produce, processor);
        }

        defaultProcessor = getDefaultOrFirstProcessor();
    }

    public URLPathBuilder getPathBuilder() {
        return this.pathBuilder;
    }

    public List<RestParam> getParamList() {
        return paramList;
    }

    private void addParam(RestParam param) {
        paramList.add(param);
        paramMap.put(param.getParamName(), param);
    }

    public ProduceProcessor findProduceProcessor(String type) {
        return this.produceProcessorMap.get(type);
    }

    // 选择与accept匹配的produce processor或者缺省的
    public ProduceProcessor ensureFindProduceProcessor(String types) {
        if (StringUtils.isEmpty(types)) {
            return defaultProcessor;
        }

        String[] typeArr = splitAcceptTypes(types);
        if (containSpecType(typeArr, MediaType.WILDCARD)) {
            return defaultProcessor;
        }
        if (containSpecType(typeArr, ProduceProcessorManager.DEFAULT_TYPE)) {
            return ProduceProcessorManager.DEFAULT_PROCESSOR;
        }

        for (String type : typeArr) {
            ProduceProcessor processor = this.produceProcessorMap.get(type);
            if (null != processor) {
                return processor;
            }
        }
        return null;
    }

    // 只提取出media type，忽略charset和q值等
    protected String[] splitAcceptTypes(String types) {
        String[] typeArr = types.split(ACCEPT_TYPE_SEPARATER);
        for (int idxX = 0; idxX < typeArr.length; idxX++) {
            String[] strItems = typeArr[idxX].split(ACCEPT_TYPE_INNER_SEPARATER);
            for (int idxY = 0; idxY < strItems.length; idxY++) {
                if (strItems[idxY].contains(MEDIATYPE_INNER_SLASH)) {
                    typeArr[idxX] = strItems[idxY].trim();
                    break;
                }
            }
        }
        return typeArr;
    }

    // 检查是否包含特定的类型
    protected boolean containSpecType(String[] typeArr, String specType) {
        for (String type : typeArr) {
            if (specType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    public ProduceProcessor getDefaultProcessor() {
        return this.defaultProcessor;
    }

    // 仅用于测试
    protected void setDefaultProcessor(ProduceProcessor defaultProcessor) {
        this.defaultProcessor = defaultProcessor;
    }

    // 获取缺省的或者第一个processor
    private ProduceProcessor getDefaultOrFirstProcessor() {
        ProduceProcessor processor = this.produceProcessorMap.get(ProduceProcessorManager.DEFAULT_TYPE);
        if (null == processor) {
            for (ProduceProcessor pp : this.produceProcessorMap.values()) {
                return pp;
            }
        }
        return processor;
    }

    public String getHttpMethod() {
        return operationMeta.getHttpMethod();
    }
}
