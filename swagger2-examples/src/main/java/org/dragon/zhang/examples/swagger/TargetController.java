package org.dragon.zhang.examples.swagger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.AuthorizationScope;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.ResponseHeader;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类上的描述2
 *
 * @author zhangzicheng
 * @version 1.0.0
 * @date 2021/03/01
 * @exception Exception
 * @throws Exception
 * @link Exception
 * @see Exception
 * @since 1.0.0
 */
@Api(
        consumes = "",
        produces = "",
        authorizations = {@Authorization(
                scopes = {@AuthorizationScope(
                        scope = "",
                        description = ""
                )},
                value = ""
        )},
        value = "",
        position = 0,
        basePath = "",
        tags = {"类上的描述2"},
        description = "",
        protocols = "",
        hidden = false
)
@RestController
public class TargetController implements BeanNameAware {

    /**
     * beanName
     */
    private String name;

    /**
     * 方法上的描述2
     *
     * @param param 参数
     * @return 返回值2
     */
    @ApiResponse(
            responseContainer = "",
            examples = @Example({@ExampleProperty(
                    value = "",
                    mediaType = ""
            )}),
            message = "返回值2",
            response = Void.class,
            responseHeaders = {@ResponseHeader(
                    responseContainer = "",
                    name = "",
                    response = Void.class,
                    description = ""
            )},
            code = 200,
            reference = ""
    )
    @ApiOperation(
            consumes = "",
            produces = "",
            authorizations = {@Authorization(
                    scopes = {@AuthorizationScope(
                            scope = "",
                            description = ""
                    )},
                    value = ""
            )},
            responseReference = "",
            nickname = "",
            responseContainer = "",
            ignoreJsonView = false,
            value = "方法上的描述2",
            position = 0,
            extensions = {@Extension(
                    name = "",
                    properties = {@ExtensionProperty(
                            name = "",
                            value = ""
                    )}
            )},
            response = Void.class,
            tags = {""},
            httpMethod = "",
            responseHeaders = {@ResponseHeader(
                    responseContainer = "",
                    name = "",
                    response = Void.class,
                    description = ""
            )},
            code = 200,
            notes = "",
            protocols = "http,https",
            hidden = false
    )
    @RequestMapping(path = "/test2", method = RequestMethod.GET)
    public String test(@RequestParam String param) {
        return name + " say hello, " + param;
    }

    @Override
    public void setBeanName(String name) {
        this.name = name;
    }
}
