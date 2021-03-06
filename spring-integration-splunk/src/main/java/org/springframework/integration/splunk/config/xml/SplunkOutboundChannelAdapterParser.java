/*
   * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.splunk.config.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.splunk.outbound.SplunkOutboundChannelAdapter;
import org.springframework.integration.splunk.support.ConnectionFactoryFactoryBean;
import org.springframework.integration.splunk.support.SplunkDataWriter;
import org.springframework.integration.splunk.support.SplunkConnectionFactory;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * The parser for the Splunk Outbound Channel Adapter.
 *
 * @author Jarred Li
 * @since 1.0
 *
 */
public class SplunkOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder splunkOutboundChannelAdapterBuilder = BeanDefinitionBuilder.genericBeanDefinition(SplunkOutboundChannelAdapter.class);
		BeanDefinitionBuilder splunkExecutorBuilder = SplunkParserUtils.getSplunkExecutorBuilder(element, parserContext);

		BeanDefinitionBuilder splunkDataWriterBuilder = BeanDefinitionBuilder.genericBeanDefinition(SplunkDataWriter.class);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(splunkDataWriterBuilder, element, "sourceType");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(splunkDataWriterBuilder, element, "source");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(splunkDataWriterBuilder, element, "index");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(splunkDataWriterBuilder, element, "ingest");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(splunkDataWriterBuilder, element, "tcpPort");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(splunkDataWriterBuilder, element, "host");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(splunkDataWriterBuilder, element, "hostRegex");
		BeanDefinitionBuilder connectionFactoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(SplunkConnectionFactory.class);

		String splunkServerBeanName = element.getAttribute("splunk-server-ref");
		if (StringUtils.hasText(splunkServerBeanName)) {
			connectionFactoryBuilder.addConstructorArgReference(splunkServerBeanName);
		}

		BeanDefinitionBuilder connectionFactoryFactoryBeanBuilder = BeanDefinitionBuilder.genericBeanDefinition(ConnectionFactoryFactoryBean.class);
		connectionFactoryFactoryBeanBuilder.addConstructorArgValue(connectionFactoryBuilder.getBeanDefinition());
		connectionFactoryFactoryBeanBuilder.addConstructorArgValue(element.getAttribute("pool-server-connection"));
		splunkDataWriterBuilder.addConstructorArgValue(connectionFactoryFactoryBeanBuilder.getBeanDefinition());

		String channelAdapterId = this.resolveId(element, splunkOutboundChannelAdapterBuilder.getRawBeanDefinition(),
				parserContext);
		String splunkExecutorBeanName = channelAdapterId + ".splunkExecutor";
		String splunkDataWriterBeanName = splunkExecutorBeanName + ".writer";

		parserContext.registerBeanComponent(new BeanComponentDefinition(splunkDataWriterBuilder.getBeanDefinition(),
				splunkDataWriterBeanName));
		splunkExecutorBuilder.addPropertyReference("writer", splunkDataWriterBeanName);

		BeanDefinition splunkExecutorBuilderBeanDefinition = splunkExecutorBuilder.getBeanDefinition();
		parserContext.registerBeanComponent(new BeanComponentDefinition(splunkExecutorBuilderBeanDefinition,
				splunkExecutorBeanName));

		splunkOutboundChannelAdapterBuilder.addConstructorArgReference(splunkExecutorBeanName);
		splunkOutboundChannelAdapterBuilder.addPropertyValue("producesReply", Boolean.FALSE);

		return splunkOutboundChannelAdapterBuilder.getBeanDefinition();

	}

}
