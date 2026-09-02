package com.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recruit.common.PasswordUtil;
import com.recruit.common.TokenStore;
import com.recruit.entity.Company;
import com.recruit.entity.User;
import com.recruit.mapper.CompanyMapper;
import com.recruit.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

// 思考：用户服务——注册、登录、退出三类用户相关业务
// 思考：@Service 标注这是业务层，Spring 启动时创建实例，供 Controller 构造器注入
// 思考：为什么登录校验放 Service 不放 Controller？——Controller 只管"接参数、调服务、包返回"，
// 思考：业务规则（审核状态判断、密码校验、查重）都在 Service，Controller 保持干净
@Service
public class UserServiceImpl implements UserService {

    // 思考：依赖的数据访问层对象，构造器注入（官方推荐方式，字段可声明 final）
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;

    // 思考：会话存储——登录成功后发 token 用（Redis 实现）
    private final TokenStore tokenStore;

    // 思考：构造器注入：Spring 启动时把三个依赖实例一次性传进来
    public UserServiceImpl(UserMapper userMapper, CompanyMapper companyMapper, TokenStore tokenStore) {
        this.userMapper = userMapper;
        this.companyMapper = companyMapper;
        this.tokenStore = tokenStore;
    }

    /**
     * 思考：登录主流程：按用户名查库 → BCrypt 校验密码 → 校验账号状态 → 企业额外校验审核状态 → 抹掉密码 → 发 token
     * 思考：任何一步失败直接 throw RuntimeException，由 GlobalExceptionHandler 统一转成错误 JSON
     * 思考：返回 Map 里装 token 和用户信息，前端把 token 存 sessionStorage、用户信息存响应式 store
     */
    public Map<String, Object> login(String username, String password) {
        // 思考：LambdaQueryWrapper 拼条件：WHERE username = ?
        // 思考：注意：不再用 username+password 两个条件查——密码是 BCrypt 密文，SQL 里没法直接等值匹配
        // 思考：先按用户名查出用户，再用 PasswordUtil 在 Java 里校验密码（密文比较必须在应用层做）
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        // 思考：用户不存在 或 密码不匹配 → 统一报"用户名或密码错误"，不区分具体哪个错（防止撞库试探）
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        // 思考：旧数据自动升级：早期版本密码是明文存的，登录命中后顺手加密写回，
        // 思考：从此这个账号就是 BCrypt 存储了（对用户无感，数据库里的明文密码自动清零）
        if (!user.getPassword().startsWith("$2")) {
            user.setPassword(PasswordUtil.encode(password));
            userMapper.updateById(user);
        }
        // 思考：status=0 表示被管理员禁用，禁用账号即使密码正确也拒绝登录
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        // 思考：企业账号额外校验入驻审核状态——只有 approved=1 的企业才能登录发岗位
        // 思考：这就是"企业入驻审核"闭环的最后一步：注册待审核 → 管理员通过 → 才能登录
        if ("company".equals(user.getRole())) {
            // 思考：按 user_id 查企业档案（一个企业账号一份档案）
            Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                    .eq(Company::getUserId, user.getId()));
            // 思考：档案不存在或还在待审核 → 拒绝登录，提示等审核
            if (company == null || company.getApproved() == null || company.getApproved() == 0) {
                throw new RuntimeException("企业入驻审核中，请等待管理员审核通过后再登录");
            }
            // 思考：被驳回的也拒绝，提示联系管理员
            if (company.getApproved() == 2) {
                throw new RuntimeException("企业入驻申请未通过，请联系管理员");
            }
        }

        // 思考：安全红线：返回给前端的用户对象必须抹掉密码，绝不能把密码泄漏到响应 JSON 里
        user.setPassword(null);
        // 思考：生成 token 存 Redis（24 小时过期），返回给前端
        String token = tokenStore.create(user);
        // 思考：Map.of 是不可变 Map 的快捷创建方式，装两个返回值：token 给前端存，user 给前端展示
        return Map.of("token", token, "user", user);
    }

    /**
     * 思考：注册：student 直接可用；company 额外创建企业档案（approved=0 待审核）
     * 思考：req 里学生传 username/password/name/phone；企业额外传 companyName/industry/scale
     */
    public void register(Map<String, String> req) {
        // 思考：先取关键字段
        String username = req.get("username");
        String password = req.get("password");
        String role = req.get("role");
        // 思考：基础校验：用户名密码必填（isBlank 连纯空格也算空）
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("用户名和密码不能为空");
        }
        // 思考：角色白名单校验——注册接口永远开不出管理员账号，管理员只能初始化进库
        if (!"student".equals(role) && !"company".equals(role)) {
            throw new RuntimeException("注册角色只能是学生或企业");
        }
        // 思考：用户名唯一性校验——selectCount 只查数量，比查整个对象快
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (count > 0) {
            throw new RuntimeException("用户名已被注册");
        }

        // 思考：组装用户对象并落库
        User user = new User();
        user.setUsername(username);
        // 思考：安全红线：密码绝不能明文落库——BCrypt 加密后再存，就算数据库泄露也拿不到明文
        user.setPassword(PasswordUtil.encode(password));
        user.setRole(role);
        // 思考：getOrDefault——没填姓名就用用户名兜底，避免 name 为 null
        user.setName(req.getOrDefault("name", username));
        user.setPhone(req.getOrDefault("phone", ""));
        user.setStatus(1);   // 思考：默认正常状态
        userMapper.insert(user);   // 思考：插入后 user.getId() 自动回填数据库生成的主键

        // 思考：企业注册：同步创建企业档案，approved=0 待管理员审核
        // 思考：这就是"企业入驻审核"业务闭环的第一环
        if ("company".equals(role)) {
            Company company = new Company();
            company.setUserId(user.getId());   // 思考：档案关联刚创建的账号
            company.setName(req.getOrDefault("companyName", user.getName()));
            company.setIndustry(req.getOrDefault("industry", ""));
            company.setScale(req.getOrDefault("scale", ""));
            company.setContact(req.getOrDefault("name", username));
            company.setApproved(0);   // 思考：待审核
            companyMapper.insert(company);
        }
    }

    /**
     * 思考：退出登录：把服务端保存的 token 删掉，这个 token 立即作废
     * 思考：Redis 会话方案的核心优势——服务端可随时主动踢人下线
     */
    public void logout(String token) {
        tokenStore.remove(token);
    }
}
