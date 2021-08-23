/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shulie.takin.parent.exception.holder;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import io.shulie.takin.parent.exception.entity.ExceptionReadable;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.boot.autoconfigure.takin.properties.BaseExceptionProperties;
import org.springframework.http.HttpStatus;

/**
* @Package io.shulie.takin.exception.holder
* @author 无涯
* @description:
* @date 2021/7/2 14:03
*/
@Slf4j
public class ExceptionMessageHolder {

    /**
     * 默认异常模块
     */
    public static final String DEFAULT_EXCEPTION_CODE = "takin_0000_S0000";
    public static final String DEFAULT_EXCEPTION_DEBUG_INFO = "系统发生未知异常";
    public static final String DEFAULT_EXCEPTION_MESSAGE_INFO = "服务器出了点小问题";
    public static final String DEFAULT_EXCEPTION_SOLUTION_INFO = "请联系管理员处理";

    private final Map<String, ExceptionPatternEntity> holder = new ConcurrentHashMap<>();

    private final BaseExceptionProperties properties;

    public ExceptionMessageHolder(BaseExceptionProperties baseExceptionProperties) {
        this.properties = baseExceptionProperties;
        init();
    }

    private void init() {
        setDefault();
        try (InputStream messageResource = getInputStream(
            properties.getMessageFilesPath() + File.separator + properties.getMessageFileName());
             InputStream debugResource = getInputStream(
                 properties.getMessageFilesPath() + File.separator + properties.getDebugFileName());
             InputStream solutionResource = getInputStream(
                 properties.getMessageFilesPath() + File.separator + properties.getSolutionFileName());
             InputStream httpCodeResource = getInputStream(
                 properties.getMessageFilesPath() + File.separator + properties.getHttpStatusFileName());) {
            mergerProperties(messageResource, "message");
            mergerProperties(debugResource, "debug");
            mergerProperties(solutionResource, "solution");
            mergerProperties(httpCodeResource, "http_code");
        } catch (Exception e) {
            log.error("读取异常错误信息文件失败",e);
        }
    }

    private void setDefault() {
        ExceptionPatternEntity patternEntity = new ExceptionPatternEntity();
        patternEntity.setCode(DEFAULT_EXCEPTION_CODE);
        patternEntity.setMessagePattern(DEFAULT_EXCEPTION_MESSAGE_INFO);
        patternEntity.setSolutionPattern(DEFAULT_EXCEPTION_SOLUTION_INFO);
        patternEntity.setDebugPattern(DEFAULT_EXCEPTION_DEBUG_INFO);
        patternEntity.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        holder.put(DEFAULT_EXCEPTION_CODE, patternEntity);
    }

    public ExceptionEntity defaultException() {
        ExceptionEntity exceptionEntity = new ExceptionEntity();
        ExceptionPatternEntity patternEntity = holder.get(DEFAULT_EXCEPTION_CODE);
        exceptionEntity.setCode(DEFAULT_EXCEPTION_CODE);
        exceptionEntity.setDebug(patternEntity.getDebugPattern() == null ?
            DEFAULT_EXCEPTION_DEBUG_INFO : patternEntity.getDebugPattern());
        exceptionEntity.setMessage(patternEntity.getMessagePattern() == null ?
            DEFAULT_EXCEPTION_MESSAGE_INFO : patternEntity.getMessagePattern());
        exceptionEntity.setSolution(patternEntity.getSolutionPattern() == null ?
            DEFAULT_EXCEPTION_SOLUTION_INFO : patternEntity.getSolutionPattern());
        exceptionEntity.setHttpStatus(patternEntity.getHttpStatus() == null ?
            HttpStatus.INTERNAL_SERVER_ERROR : patternEntity.getHttpStatus());
        return exceptionEntity;
    }

    public ExceptionEntity get(ExceptionReadable exception, Object o) {
        ExceptionEntity exceptionEntity = new ExceptionEntity();

        if(holder.containsKey(exception.getErrorCode())) {
            // 从配置文件寻找表达式 配置模板
            ExceptionPatternEntity patternEntity = holder.get(exception.getErrorCode());
            if (patternEntity == null && o instanceof String) {
                ExceptionEntity defaultException = defaultException();
                String message = (String)o;
                if (StringUtils.isBlank(message)) {
                    message = exception.getDefaultValue();
                }
                if (StringUtils.isNoneBlank(message)) {
                    defaultException.setMessage(message);
                }
                defaultException.setCode(exception.getErrorCode());
                return defaultException;
            }
            Map vars = getPatternVars(o);
            exceptionEntity.setCode(exception.getErrorCode());
            exceptionEntity.setDebug((patternEntity.getDebugPattern() == null) ? exception.getDefaultValue()
                : new StringSubstitutor(vars).replace(patternEntity.getDebugPattern()));
            exceptionEntity.setMessage((patternEntity.getMessagePattern() == null) ? exception.getDefaultValue()
                : new StringSubstitutor(vars).replace(patternEntity.getMessagePattern()));
            exceptionEntity.setSolution((patternEntity.getSolutionPattern() == null) ? DEFAULT_EXCEPTION_SOLUTION_INFO
                : new StringSubstitutor(vars).replace(patternEntity.getSolutionPattern()));
            exceptionEntity.setHttpStatus(patternEntity.getHttpStatus() == null ? HttpStatus.OK
                : patternEntity.getHttpStatus());
        }else {
            // 未配置模板
            exceptionEntity.setCode(exception.getErrorCode());
            exceptionEntity.setHttpStatus(HttpStatus.OK);
        }
        return exceptionEntity;
    }

    private InputStream getInputStream(String path) {
        return this.getClass().getClassLoader().getResourceAsStream(path);
    }

    private void mergerProperties(InputStream resource, String fieldName) {
        if (resource == null) {
            return;
        }
        try {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(resource, StandardCharsets.UTF_8));
            properties.forEach((key, value) -> {
                ExceptionPatternEntity patternEntity = holder.get((String)key);
                if (patternEntity == null) {
                    patternEntity = new ExceptionPatternEntity();
                    patternEntity.setCode((String)key);
                    holder.put((String)key, patternEntity);
                }
                if ("message".equals(fieldName)) {
                    patternEntity.setMessagePattern((String)value);
                }
                if ("debug".equals(fieldName)) {
                    patternEntity.setDebugPattern((String)value);
                }
                if ("solution".equals(fieldName)) {
                    patternEntity.setSolutionPattern((String)value);
                }
                if ("http_code".equals(fieldName)) {
                    patternEntity.setHttpStatus(HttpStatus.valueOf(Integer.parseInt((String)value)));
                }
            });
        } catch (Exception e) {
            // ignore
        }
    }

    private Map getPatternVars(Object o) {
        Map vars;
        if (o instanceof Map) {
            vars = (Map)o;
        } else if (o == null) {
            vars = new HashMap();
        } else if (o instanceof String) {
            vars = new HashMap();
            vars.put("text", o);
        } else {
            vars = objectToMap(o);
        }
        return vars;
    }

    @Data
    public static class ExceptionEntity {

        private String code;

        private String message;

        private String solution;

        private String debug;

        private HttpStatus httpStatus;
    }

    @Data
    private static class ExceptionPatternEntity {

        private String code;

        private String messagePattern;

        private String solutionPattern;

        private String debugPattern;

        private HttpStatus httpStatus;
    }

    public static Map<String, String> objectToMap(Object obj) {
        Map<String, String> map = new HashMap<>();
        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                String fieldName = field.getName();
                String value = String.valueOf(field.get(obj));
                map.put(fieldName, value);
            } catch (Exception e) {
                // ignore
            }
        }
        return map;
    }
}
