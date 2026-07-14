package com.xiaoyang.d_game.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@MapperScan("com.xiaoyang.d_game.mapper")
/**
 * MyBatis-Plus 全局配置。
 *
 * <p>集中配置分页、乐观锁、防全表更新删除以及自动填充字段。
 * 这些插件会作用于所有 Mapper，因此新增业务表时一般不需要重复写分页和时间填充逻辑。</p>
 */
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 插件链。
     *
     * <p>分页插件负责物理分页；乐观锁插件配合 {@code @Version} 字段防止并发覆盖；
     * 防全表更新插件可以拦截没有 WHERE 条件的 update/delete，降低误操作风险。</p>
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    /**
     * 创建自动填充处理器。
     *
     * <p>插入时写入创建时间和修改时间；更新时刷新修改时间。
     * Entity 只要继承 {@code BaseEntity} 并使用对应字段名，就会自动享受该能力。</p>
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                // 同一次插入中创建时间和修改时间保持完全一致，便于前端展示和测试断言。
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "gmtCreate", LocalDateTime.class, now);
                strictInsertFill(metaObject, "gmtModified", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                // 只更新修改时间，不覆盖创建时间，保留原始创建时间的审计意义。
                strictUpdateFill(metaObject, "gmtModified", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
