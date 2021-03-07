package org.dragon.zhang.swagger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Example;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ResponseHeader;
import org.dragon.zhang.JavadocAnnotator;
import org.dragon.zhang.JavadocMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author zhangzicheng
 * @date 2021/03/07
 */
public class SwaggerAnnotator extends JavadocAnnotator {

    @Override
    protected Set<Class<? extends Annotation>> buildTypeMarks() {
        Set<Class<? extends Annotation>> set = new HashSet<>();
        set.add(Controller.class);
        return set;
    }

    @Override
    protected Set<Class<? extends Annotation>> buildMethodMarks() {
        Set<Class<? extends Annotation>> set = new HashSet<>();
        set.add(RequestMapping.class);
        return set;
    }

    @Override
    protected Set<JavadocMapping<? extends Annotation>> buildTagParameter() {
        ApiParam apiParam = new ApiParam() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ApiParam.class;
            }

            @Override
            public String name() {
                return null;
            }

            @Override
            public String value() {
                return null;
            }

            @Override
            public String defaultValue() {
                return null;
            }

            @Override
            public String allowableValues() {
                return null;
            }

            @Override
            public boolean required() {
                return false;
            }

            @Override
            public String access() {
                return null;
            }

            @Override
            public boolean allowMultiple() {
                return false;
            }

            @Override
            public boolean hidden() {
                return false;
            }

            @Override
            public String example() {
                return null;
            }

            @Override
            public Example examples() {
                return null;
            }

            @Override
            public String type() {
                return null;
            }

            @Override
            public String format() {
                return null;
            }

            @Override
            public boolean allowEmptyValue() {
                return false;
            }

            @Override
            public boolean readOnly() {
                return false;
            }

            @Override
            public String collectionFormat() {
                return null;
            }
        };
        Map<String, String> apiParamMapping = new HashMap<>();
        apiParamMapping.put("name", "paramName");
        apiParamMapping.put("value", "paramDescription");

        Set<JavadocMapping<? extends Annotation>> set = new HashSet<>();
        set.add(new JavadocMapping<>(apiParam, apiParamMapping));
        return set;
    }

    @Override
    protected Set<JavadocMapping<? extends Annotation>> buildTagMethod() {
        ApiOperation apiOperation = new ApiOperation() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ApiOperation.class;
            }

            @Override
            public String value() {
                return null;
            }

            @Override
            public String notes() {
                return null;
            }

            @Override
            public String[] tags() {
                return null;
            }

            @Override
            public Class<?> response() {
                return null;
            }

            @Override
            public String responseContainer() {
                return null;
            }

            @Override
            public String responseReference() {
                return null;
            }

            @Override
            public String httpMethod() {
                return null;
            }

            @Override
            public int position() {
                return 0;
            }

            @Override
            public String nickname() {
                return null;
            }

            @Override
            public String produces() {
                return null;
            }

            @Override
            public String consumes() {
                return null;
            }

            @Override
            public String protocols() {
                return "http,https";
            }

            @Override
            public Authorization[] authorizations() {
                return null;
            }

            @Override
            public boolean hidden() {
                return false;
            }

            @Override
            public ResponseHeader[] responseHeaders() {
                return null;
            }

            @Override
            public int code() {
                return 200;
            }

            @Override
            public Extension[] extensions() {
                return null;
            }

            @Override
            public boolean ignoreJsonView() {
                return false;
            }
        };
        Map<String, String> apiOperationMapping = new HashMap<>();
        apiOperationMapping.put("value", "description");
        JavadocMapping<ApiOperation> mapping1 = new JavadocMapping<>(apiOperation, apiOperationMapping);

        ApiResponse apiResponse = new ApiResponse() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ApiResponse.class;
            }

            @Override
            public int code() {
                return 200;
            }

            @Override
            public String message() {
                return null;
            }

            @Override
            public Class<?> response() {
                return null;
            }

            @Override
            public String reference() {
                return null;
            }

            @Override
            public ResponseHeader[] responseHeaders() {
                return null;
            }

            @Override
            public String responseContainer() {
                return null;
            }

            @Override
            public Example examples() {
                return null;
            }
        };
        Map<String, String> apiResponseMapping = new HashMap<>();
        apiResponseMapping.put("message", "return");
        JavadocMapping<ApiResponse> mapping2 = new JavadocMapping<>(apiResponse, apiResponseMapping);

        Set<JavadocMapping<? extends Annotation>> set = new HashSet<>();
        set.add(mapping1);
        set.add(mapping2);
        return set;
    }

    @Override
    protected Set<JavadocMapping<? extends Annotation>> buildTagClass() {
        Api api = new Api() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Api.class;
            }

            @Override
            public String value() {
                return null;
            }

            @Override
            public String[] tags() {
                return null;
            }

            @Override
            public String description() {
                return null;
            }

            @Override
            public String basePath() {
                return null;
            }

            @Override
            public int position() {
                return 0;
            }

            @Override
            public String produces() {
                return null;
            }

            @Override
            public String consumes() {
                return null;
            }

            @Override
            public String protocols() {
                return null;
            }

            @Override
            public Authorization[] authorizations() {
                return null;
            }

            @Override
            public boolean hidden() {
                return false;
            }
        };
        Map<String, String> apiMapping = new HashMap<>();
        apiMapping.put("tags", "description");

        Set<JavadocMapping<? extends Annotation>> set = new HashSet<>();
        set.add(new JavadocMapping<>(api, apiMapping));
        return set;
    }

}
