package com.recruit.controller;

import com.recruit.common.Result;
import com.recruit.entity.User;
import com.recruit.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 思考：认证接口——登录 / 注册 / 退出（无需 token，因为登录注册本身就是"拿 token"的过程）
// 思考：/api/auth/** 在拦截器里被放行（见 WebConfig 的 excludePathPatterns）——如果这里也要 token 就是死循环
//
// 思考：Spring MVC 注解速查（看懂所有 Controller 的钥匙）：
// 思考：@RestController        类上：声明这个类的所有方法返回值都自动转 JSON（= @Controller + @ResponseBody 合体）
// 思考：@RequestMapping("/api/auth")  类上：给这个类所有接口加统一路径前缀
// 思考：@PostMapping("/login")   方法上：POST 请求 + 路径，组合起来就是 POST /api/auth/login
// 思考：@RequestBody             参数上：把请求体里的 JSON 自动转成 Java 对象/Map
// 思考：@RequestParam            参数上：取 URL 问号后面的参数，如 ?keyword=Java&city=昆明
// 思考：@PathVariable            参数上：取路径里的变量，如 /jobs/{id} 中的 id
// 思考：@RequestHeader           参数上：取请求头（我们自定义的 token 就放在请求头里）
// 思考：@RequestAttribute        参数上：取拦截器放进 request 的属性（登录用户）
@Tag(name = "01-认证", description = "登录、注册、退出（无需 token）")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 思考：认证业务都封装在 UserService，Controller 只负责接参数、调服务、包返回
    private final UserService userService;

    // 思考：构造器注入（官方推荐方式，比 @Autowired 字段注入好：字段可声明 final、依赖一目了然、方便写单测时传 mock）
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "登录", description = "返回 token 和用户信息；企业账号需审核通过")
    // 思考：登录接口：POST /api/auth/login，请求体：{"username":"student","password":"1234"}
    // 思考：@RequestBody 把这段 JSON 自动转成 Map<String,String>（key 是字段名，value 是值）
    // 思考：返回值 Result<Map> 被 @RestController 自动转成 JSON：
    // 思考：{"code":0,"msg":"success","data":{"token":"xxx...","user":{...用户信息...}}}
    // 思考：前端登录成功后把 token 存 sessionStorage，之后每次请求都带上它
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> req) {
        // 思考：req.get(key) 取 JSON 字段：req.get("username") 就是用户填的用户名
        // 思考：Result.ok() 包装成功响应；业务失败由 Service 抛异常、GlobalExceptionHandler 统一转错误 JSON
        return Result.ok(userService.login(req.get("username"), req.get("password")));
    }

    @Operation(summary = "注册", description = "role=student 直接可用；role=company 需带 companyName 等待审核")
    // 思考：注册接口
    // 思考：学生注册只传基础字段：{"role":"student","username":"..","password":"..","name":"..","phone":".."}
    // 思考：企业注册额外传公司字段：companyName/industry/scale，
    // 思考：注册后企业档案为"待审核"状态，管理员审核通过才能登录（见 UserService.register）
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> req) {
        // 思考：校验、查重、落库都在 Service 里做，Controller 一行调用
        userService.register(req);
        // 思考：Result.ok("注册成功")：字符串作为 data 返回，前端弹提示用
        return Result.ok("注册成功");
    }

    @Operation(summary = "退出登录", description = "作废服务端保存的 token")
    // 思考：退出登录
    // 思考：@RequestHeader：从请求头里取值。前端 axios 每次请求都会带 token 请求头，
    // 思考：退出时把服务端保存的这个 token 作废即可。required=false 表示没带也不报错（容错）
    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader(value = "token", required = false) String token) {
        // 思考：tokenStore.remove 删掉 Redis 里的 token，这个 token 立即失效
        userService.logout(token);
        return Result.ok("已退出登录");
    }
}