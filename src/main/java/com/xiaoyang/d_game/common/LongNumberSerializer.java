package com.xiaoyang.d_game.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * 将明确标注的 Long/long 数量或页码输出为 JSON 整数。
 *
 * <p>仅供字段级注解使用，覆盖项目全局 Long 转字符串规则；ID 继续使用字符串。
 * 无参构造器允许 Jackson 和 Spring 创建序列化器，无需注入类型参数。</p>
 */
public final class LongNumberSerializer extends StdScalarSerializer<Long> {
    private static final long serialVersionUID = 1L;

    public LongNumberSerializer() {
        super(Long.class);
    }

    @Override
    public void serialize(Long value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        generator.writeNumber(value.longValue());
    }
}
