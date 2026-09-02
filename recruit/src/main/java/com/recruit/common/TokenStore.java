package com.recruit.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.recruit.entity.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

// 思考：登录会话存储（Redis 版）。核心职责：登录时发 token、每次请求用 token 反查用户、退出时作废
// 思考：最早版本用内存 Map 存 token，三个致命问题：重启全员掉线、无法多实例部署、没有过期机制
// 思考：换 Redis 后：TTL 自动过期（天然解决"登录有效期"）、重启不掉线、多机共享
// 思考：面试高频——"为什么不用 JWT？"：Redis 方案删 key 即可随时踢人下线，JWT 签发后无法撤销
@Component
public class TokenStore {

    // 思考：key 统一前缀，一是防止和其他业务的 key 冲突，二是方便按前缀批量排查/清理
    private static final String KEY_PREFIX = "login:token:";

    // 思考：登录有效期 24 小时，到点 Redis 自动删除该 key = 天然的登录过期，不需要定时任务
    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    // 思考：StringRedisTemplate 是 Spring Data Redis 提供的 Redis 操作模板（key/value 都是字符串）
    // 思考：自动装配——application.yml 配了 Redis 地址，启动时就创建好了，这里直接注入用
    private final StringRedisTemplate redis;

    // 思考：User 对象要序列化成 JSON 字符串存进 Redis，取出时再反序列化回来
    // 思考：User 里有 LocalDateTime 字段，必须注册 JavaTimeModule 才能正确序列化，否则直接报错
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // 思考：构造器注入，让 Spring 把 Redis 操作模板传进来
    public TokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 思考：登录成功后调用——生成随机 token，把用户信息存进 Redis 并设置 24 小时过期
     * 思考：UUID 是全球唯一的随机字符串，当作"临时通行证"发给前端
     */
    public String create(User user) {
        // 思考：UUID 去掉中划线变成纯字母数字串，作为 token 更整洁
        String token = UUID.randomUUID().toString().replace("-", "");
        try {
            // 思考：opsForValue() 是操作字符串类型的入口，set(key, value, 过期时间) 三参数版自动带 TTL
            // 思考：value 存的是整个 User 的 JSON——以后每次请求凭 token 就能反查出完整用户信息，不用再查库
            redis.opsForValue().set(KEY_PREFIX + token, mapper.writeValueAsString(user), TOKEN_TTL);
        } catch (Exception e) {
            // 思考：Redis 挂了登录就发不出 token，直接抛业务异常让用户重试，不能静默失败
            throw new RuntimeException("登录会话写入失败", e);
        }
        return token;
    }

    /**
     * 思考：每次请求时调用——拿 token 换回用户信息
     * 思考：返回 null 的三种可能：前端没带 token / token 已过期被 Redis 删除 / token 本来就是伪造的
     */
    public User get(String token) {
        // 思考：先判空，防止调用方传 null 时 NPE
        if (token == null) return null;
        try {
            // 思考：get 返回 null 说明 Redis 里没有这个 key（未登录或已过期）
            String json = redis.opsForValue().get(KEY_PREFIX + token);
            return json == null ? null : mapper.readValue(json, User.class);
        } catch (Exception e) {
            // 思考：反序列化失败按"未登录"处理，宁可让用户重新登录也不能让请求带着坏数据往下走
            return null;
        }
    }

    /**
     * 思考：退出登录时调用——直接删除 Redis 里的 key，这个 token 立即作废
     * 思考：这就是 Redis 会话方案的核心优势：服务端可以随时主动踢人下线
     */
    public void remove(String token) {
        // 思考：判空容错，避免传 null 时 Redis 抛异常
        if (token != null) redis.delete(KEY_PREFIX + token);
    }
}
