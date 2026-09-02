package com.recruit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.recruit.entity.Company;
import org.apache.ibatis.annotations.Mapper;

// 思考：Company 表的数据访问层（DAO）——继承 BaseMapper<Company> 即自动拥有增删改查能力，一行 SQL 不用写
// 思考：BaseMapper 提供的常用方法：insert / deleteById / updateById / selectById / selectList / selectOne / selectCount
// 思考：条件查询用 LambdaQueryWrapper 拼条件（比手拼 SQL 字符串安全，防注入）
// 思考：@Mapper 让 MyBatis 启动时为接口生成实现类并交给 Spring 管理，Service 里才能注入它
@Mapper
public interface CompanyMapper extends BaseMapper<Company> {
    // 思考：方法体是空的！所有通用方法都从 BaseMapper 继承
    // 思考：只有通用方法搞不定的复杂 SQL（如多表 JOIN 统计）才需要在这里写自定义方法
    // 思考：项目里实际用到的调用示例（都在 Service 层）：
    // 思考：  selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "student"))  登录按用户名查
    // 思考：  selectList(new LambdaQueryWrapper<User>().eq(User::getRole, "student"))     管理员查学生列表
    // 思考：  selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, name))      注册时查重

}
