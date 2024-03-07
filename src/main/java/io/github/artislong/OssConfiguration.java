package io.github.artislong;

import cn.hutool.core.text.StrPool;
import io.github.artislong.constant.OssConstant;
import io.github.artislong.core.StandardOssClient;
import io.github.artislong.function.ThreeConsumer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * @author 陈敏
 * @version OssAutoConfiguration.java, v 1.1 2021/11/5 11:05 chenmin Exp $
 * Created on 2021/11/5
 */
@Getter
@Setter
@Slf4j
public abstract class OssConfiguration implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private Environment environment;

    @Override
    public void postProcessBeanDefinitionRegistry(@NotNull BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        // 动态注册一个Bean
        registerBean((beanName, clazz, beanProMap) -> {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
            beanProMap.forEach(builder::addPropertyValue);
            BeanDefinition beanDefinition = builder.getBeanDefinition();
            BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, beanName);
            beanDefinitionRegistry.registerBeanDefinition(holder.getBeanName(), holder.getBeanDefinition());
        });
    }

    @Override
    public void postProcessBeanFactory(@NotNull ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
    }

    public abstract void registerBean(ThreeConsumer<String, Class<? extends StandardOssClient>, Map<String, Object>> consumer);

    protected <T> T getOssProperties(Class<T> clazz, String ossType) {
        try {
            BindResult<T> bindResult = Binder.get(getEnvironment())
                    .bind(OssConstant.OSS + StrPool.DOT + ossType, clazz);
            T ossProperties = bindResult.get();
            if (ossProperties instanceof InitializingBean) {
                ((InitializingBean) ossProperties).afterPropertiesSet();
            }
            return ossProperties;
        } catch (Exception e) {
            log.warn("{}未配置，请检查！", ossType, e);
            return null;
        }
    }
}
