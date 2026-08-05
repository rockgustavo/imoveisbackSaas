package br.com.rockgustavo.imobiliaria.contrato;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@TestConfiguration
class ContadorDeQueriesConfig {

    @Bean
    static BeanPostProcessor dataSourceContandoQueriesPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof ContadorDeQueriesDataSource)) {
                    return new ContadorDeQueriesDataSource(dataSource);
                }
                return bean;
            }
        };
    }
}
