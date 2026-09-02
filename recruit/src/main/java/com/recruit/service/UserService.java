package com.recruit.service;

import java.util.Map;

// 思考：用户服务接口——定义"用户模块对外提供的全部能力"（能力清单/合同）
// 思考：Controller 只依赖本接口，不关心具体实现（实现类 UserServiceImpl 里的细节对调用方透明）
// 思考：本项目每个接口目前只有一个实现类；若以后出现多种实现（如不同登录方式），调用方代码无需改动
public interface UserService {

    // 思考：登录——成功返回 {token, user}，失败抛异常由全局异常处理器兜底
    Map<String, Object> login(String username, String password);

    // 思考：注册——student 直接可用；company 额外创建企业档案（待审核）
    void register(Map<String, String> req);

    // 思考：退出登录——作废服务端保存的 token
    void logout(String token);
}