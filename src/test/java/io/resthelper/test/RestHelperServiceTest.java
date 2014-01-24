/*
 * Copyright 2014 The RestHelper Project
 *
 * The RestHelper Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.resthelper.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import io.resthelper.RestHelperService;
import io.resthelper.model.RestApi;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author redstrato
 */
@ContextConfiguration(locations = {"classpath:api-servlet.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class RestHelperServiceTest {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private RestHelperService restHelperService;

	@Test
	public void test() throws IOException {
		Properties properties = new Properties();
		properties.load(this.getClass().getResourceAsStream("/resthelper.properties"));
		
		assertNotNull(properties);
		String strBasePackages = properties.getProperty("resthelper.base.packages");
		assertNotNull(strBasePackages);
		
		String[] arrBasePackages = strBasePackages.split(",");
		
		String[] basePackages = restHelperService.getBasePackages();
		assertNotNull(basePackages);
		
		for (int i = 0 ; i < basePackages.length; i++) {
			assertTrue(basePackages[i].equals(arrBasePackages[i].trim()));
		}
		
		for (String basePackage : basePackages) {
			List<RestApi> apiList = restHelperService.getApiList(basePackage);
			
			logger.info("basePackage:{}", basePackage);
			for (RestApi restApi : apiList) {
				logger.info("\tapi:{}", ToStringBuilder.reflectionToString(restApi, ToStringStyle.SIMPLE_STYLE));
			}
		}
	}
}
