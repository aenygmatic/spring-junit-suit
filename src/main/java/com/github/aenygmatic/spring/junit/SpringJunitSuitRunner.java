/*
 * Copyright 2014 Balazs Berkes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.aenygmatic.spring.junit;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;
import org.junit.runners.model.Statement;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Runs a set of JUnit test and initializes one Spring context for all test cases.
 * <p>
 * @author Balazs Berkes
 */
public class SpringJunitSuitRunner extends Suite {

    public SpringJunitSuitRunner(Class<?> clazz, RunnerBuilder builder) throws InitializationError {
        super(null, new SpringAutowiringRunnerBuilder(clazz).build());
    }

    private static class SpringAutowiringRunnerBuilder {

        private ApplicationContext applicationContext;
        private Class<?> suitClass;

        private SpringAutowiringRunnerBuilder(Class<?> suitClass) {
            this.suitClass = suitClass;
        }

        private List<Runner> build() throws InitializationError {
            Class<?>[] testClasses = getSuitClasses();
            initializeSpringContext();
            return buildRunners(testClasses);
        }

        private Class<?>[] getSuitClasses() throws InitializationError {
            Suite.SuiteClasses annotation = getPresentedAnnotation(Suite.SuiteClasses.class);
            return annotation.value();
        }

        private void initializeSpringContext() throws InitializationError {
            ContextConfiguration annotation = getPresentedAnnotation(ContextConfiguration.class);
            applicationContext = new ClassPathXmlApplicationContext(annotation.value());
        }

        private <A extends Annotation> A getPresentedAnnotation(Class<A> clazz) throws InitializationError {
            A annotation = suitClass.getAnnotation(clazz);
            if (annotation == null) {
                throw new InitializationError(String.format("Class '%s' must have a '%s' annotation", clazz, suitClass.getName()));
            }
            return annotation;
        }

        private List<Runner> buildRunners(Class<?>[] testClasses) throws InitializationError {
            List<Runner> runners = new ArrayList<>(testClasses.length);

            for (Class<?> testClass : testClasses) {
                runners.add(new SpringAutowiringRunner(testClass, applicationContext));
            }

            return runners;
        }
    }

    public static class SpringAutowiringRunner extends BlockJUnit4ClassRunner {

        private ApplicationContext context;

        private SpringAutowiringRunner(Class<?> clazz, ApplicationContext context) throws InitializationError {
            super(clazz);
            this.context = context;
        }

        @Override
        protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
            context.getAutowireCapableBeanFactory().autowireBean(target);
            return super.withBefores(method, target, statement);
        }
    }
}
