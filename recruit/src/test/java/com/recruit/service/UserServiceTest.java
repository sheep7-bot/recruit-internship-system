package com.recruit.service;

import com.recruit.common.PasswordUtil;
import com.recruit.common.TokenStore;
import com.recruit.entity.Company;
import com.recruit.entity.User;
import com.recruit.mapper.CompanyMapper;
import com.recruit.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 思考：UserService 单元测试——只测业务逻辑，不启动 Spring、不连数据库/Redis（用 Mockito 造假的 Mapper）
// 思考：为什么能这么测？UserService 用的是"构造器注入"，测试里 new UserService(假Mapper, 假Mapper, 假TokenStore) 就能测
// 思考：Mock 的含义：UserMapper 是假的，selectOne/selectCount 返回什么由测试自己规定，跑不到真数据库
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    // 思考：三个假依赖——Mockito 自动生成空实现，测试里用 when(...).thenReturn(...) 规定行为
    @Mock
    private UserMapper userMapper;
    @Mock
    private CompanyMapper companyMapper;
    @Mock
    private TokenStore tokenStore;

    // 思考：把三个 mock 塞进 UserServiceImpl 构造器，得到一个"被测试的真实实现类实例"
    // 思考：注：@InjectMocks 需要具体实现类（接口不能实例化），所以这里写 Impl 而不是 UserService
    @InjectMocks
    private UserServiceImpl userService;

    // 思考：工具方法——造一个密码是 BCrypt("1234") 的普通学生用户
    private User studentWithBcryptPassword() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student");
        user.setPassword(PasswordUtil.encode("1234"));   // 真实 BCrypt 加密
        user.setRole("student");
        user.setStatus(1);
        return user;
    }

    /**
     * 思考：用例1 登录成功——密码正确时返回 token 和用户，且密码必须被抹掉
     * 思考：为什么断言密码为 null？安全红线：响应 JSON 里绝不能出现密码
     */
    @Test
    void login_success_returnsTokenAndClearsPassword() {
        // 思考：安排（Given）——按用户名查返回这个用户；发 token 返回 "test-token"
        when(userMapper.selectOne(any())).thenReturn(studentWithBcryptPassword());
        when(tokenStore.create(any())).thenReturn("test-token");

        // 思考：执行（When）——真实调用 login 方法
        Map<String, Object> result = userService.login("student", "1234");

        // 思考：断言（Then）——token 正确、用户信息在、密码已抹掉
        assertEquals("test-token", result.get("token"));
        User returned = (User) result.get("user");
        assertEquals("student", returned.getUsername());
        assertNull(returned.getPassword(), "返回给前端的用户绝不能带密码");
    }

    /**
     * 思考：用例2 密码错误——抛"用户名或密码错误"
     */
    @Test
    void login_wrongPassword_throws() {
        // 思考：库里存的是 BCrypt("1234")，但用户输入 "wrong"
        when(userMapper.selectOne(any())).thenReturn(studentWithBcryptPassword());

        // 思考：断言抛异常，且异常消息正确
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login("student", "wrong"));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    /**
     * 思考：用例3 旧明文自动升级——库里还是明文时登录成功，且密码被加密写回数据库
     * 思考：这是"兼容老数据"的回归测试：保证以后删掉兼容代码也不会悄悄破坏这个行为
     * 思考：注意：updateById(user) 和 login 末尾 setPassword(null) 用的是同一个对象引用，
     * 思考：所以不能事后用 ArgumentCaptor 读（读到的是被抹过的 null）——要用 doAnswer 在调用瞬间取值
     */
    @Test
    void login_legacyPlainPassword_upgradesToBcrypt() {
        // 思考：模拟早期版本：库里密码就是明文 "1234"
        User legacy = studentWithBcryptPassword();
        legacy.setPassword("1234");
        when(userMapper.selectOne(any())).thenReturn(legacy);
        when(tokenStore.create(any())).thenReturn("test-token");

        // 思考：doAnswer：updateById 被调用的瞬间，把传入对象的密码立即取出来（此时还没被抹掉）
        final String[] savedPassword = new String[1];
        doAnswer(inv -> {
            savedPassword[0] = ((User) inv.getArgument(0)).getPassword();
            return 0;
        }).when(userMapper).updateById(any(User.class));

        // 思考：执行登录
        Map<String, Object> result = userService.login("student", "1234");
        assertNotNull(result.get("token"));

        // 思考：断言升级写回：updateById 调用过一次，且写回的是 BCrypt 密文
        verify(userMapper, times(1)).updateById(any(User.class));
        String saved = savedPassword[0];
        assertNotNull(saved);
        assertTrue(saved.startsWith("$2"), "升级后密码必须是 BCrypt 格式");
        assertTrue(PasswordUtil.matches("1234", saved), "升级后的密文必须能通过校验");
    }

    /**
     * 思考：用例4 注册查重——用户名已存在时拒绝注册
     */
    @Test
    void register_duplicateUsername_throws() {
        // 思考：selectCount 返回 1，表示用户名已被占用
        when(userMapper.selectCount(any())).thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register(Map.of("username", "student", "password", "1234", "role", "student")));
        assertEquals("用户名已被注册", ex.getMessage());
    }

    /**
     * 思考：用例5 注册成功——密码以 BCrypt 形式落库，绝不明文
     */
    @Test
    void register_success_passwordStoredAsBcrypt() {
        // 思考：selectCount 返回 0，用户名没被占用
        when(userMapper.selectCount(any())).thenReturn(0L);

        // 思考：执行注册
        userService.register(Map.of("username", "student", "password", "1234", "role", "student"));

        // 思考：捕获 insert 时传入的 User 对象，验证密码
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).insert(captor.capture());
        User saved = captor.getValue();
        assertNotEquals("1234", saved.getPassword(), "数据库里绝不能存明文密码");
        assertTrue(saved.getPassword().startsWith("$2"), "必须是 BCrypt 密文");
        assertTrue(PasswordUtil.matches("1234", saved.getPassword()), "密文必须能校验回明文 1234");
        // 思考：学生注册不碰企业表
        verify(companyMapper, never()).insert(any(Company.class));
    }
}