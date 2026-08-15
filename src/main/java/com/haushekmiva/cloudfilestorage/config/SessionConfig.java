package com.haushekmiva.cloudfilestorage.config;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.DefaultTyping;

@Configuration
public class SessionConfig implements BeanClassLoaderAware {

    private ClassLoader loader;

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        BasicPolymorphicTypeValidator.Builder typeValidatorBuilder = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.haushekmiva.cloudfilestorage.") // Разрешаем весь пакет
                .allowIfSubType("java.util.")                     // Для коллекций (List, Map и т.д.)
                .allowIfSubType("java.lang.");                    // Для стандартных типов (String, Long и т.д.)


        var mapper = JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(loader, typeValidatorBuilder))
                .build();

        return new GenericJacksonJsonRedisSerializer(mapper);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.loader = classLoader;
    }
}