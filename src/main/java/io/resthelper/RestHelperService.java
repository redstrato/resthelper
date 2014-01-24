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
package io.resthelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;

/**
 * API 검색하여 정보와 목록을 관리
 * 
 * @author redstrato
 */
public class RestHelperService implements RestHelper {
	// slf4j-log4j
	private final Logger logger = LoggerFactory.getLogger(getClass());

	// <package, apilist>
	private Map<String, List<RestApi>> apiMap = new HashMap<String, List<RestApi>>();
	private RestApiBeanParser webApiBeanParser = new RestApiBeanParser();

	@Value("${resthelper.acl.use:true}")
	private boolean useIpAcl;

	@Value("${resthelper.acl.ip:127.0.0.1,10,0:0:0:0:0:0:0:1}")
	private String[] aclIpArray;

	private String[] basePackages;

	public void setBasePackages(String[] basePackages) {
		this.basePackages = basePackages;
	}

	public void init() throws ClassNotFoundException {
		logger.info("initializing...");
		// @Controller 만 검색
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

		if (basePackages == null) {
			logger.info("basePackages is null. finishing scanning apis");
			return;
		}

		for (String basePackage : basePackages) {
			logger.debug("scanning package; {}", basePackage);
			Set<BeanDefinition> beans = scanner.findCandidateComponents(basePackage);
			Map<String, RestApi> tempApiMap = new HashMap<String, RestApi>();

			for (BeanDefinition bean : beans) {
				logger.debug("\tparsing bean definition; {}", bean);
				List<RestApi> webApiList = webApiBeanParser.parseBeanDefinition(bean);

				for (RestApi webApi : webApiList) {
					StringBuilder apiKeyBuilder = new StringBuilder();
					apiKeyBuilder.append(webApi.getUriPattern() + "-");
					apiKeyBuilder.append(webApi.getHttpMethod());

					//					String[] matchingParams = webApi.getMatchingParams();
					//					for (String matchingParam : matchingParams) {
					//						apiKeyBuilder.append(matchingParam + "&");
					//					}
					//					
					//					String[] matchingHeaders = webApi.getMatchingHeaders();
					//					for (String matchingHeader : matchingHeaders) {
					//						apiKeyBuilder.append("::" + matchingHeader);
					//					}
					//					
					String apiKey = apiKeyBuilder.toString();
					webApi.setApiKey(apiKey);
					tempApiMap.put(apiKey, webApi);

					logger.debug("\t\tadded api; {}", apiKey);
				}
			}

			List<String> keyList = new ArrayList<String>(tempApiMap.keySet());
			Collections.sort(keyList);

			List<RestApi> apiList = new ArrayList<RestApi>();

			for (String key : keyList) {
				apiList.add(tempApiMap.get(key));
			}

			// 각 package별로 sort된 상태로 list 추가
			apiMap.put(basePackage, Collections.unmodifiableList(apiList));
		}
	}

	@Override
	public boolean isValidIp(HttpServletRequest request) {
		if (!useIpAcl) {
			return true;
		}
		
		List<String> aclIpList = Arrays.asList(aclIpArray);

		String remoteAddr = request.getHeader("X-Real-IP");
		remoteAddr = (remoteAddr != null) ? remoteAddr : request.getRemoteAddr();

		logger.debug("acl check : {}", useIpAcl);
		if ((useIpAcl) && !CollectionUtils.isEmpty(aclIpList)) {
			if (remoteAddr == null || remoteAddr.length() == 0) {
				return false;
			}

			logger.debug("aclIpList : {}", aclIpList);
			String checkIp = "";
			String[] ipBands = remoteAddr.split("\\.");
			for (String ipBand : ipBands) {
				checkIp += ipBand;
				logger.debug("checkIp : {}", checkIp);
				if (aclIpList.contains(checkIp)) {
					return true;
				}
				checkIp += ".";
			}

		} else {
			return true;
		}

		return false;
	}

	public void destroy() {
		logger.info("destroying...");
	}

	/**
	 * 전체 api 목록
	 * 
	 * @return
	 * @see io.resthelper.RestHelper#getApiList()
	 */
	public List<RestApi> getApiList() {
		List<RestApi> apiList = new ArrayList<RestApi>();

		for (List<RestApi> apis : apiMap.values()) {
			apiList.addAll(apis);
		}

		// XXX sort 필요한가?

		return apiList;
	}

	/**
	 * package별 api 목록
	 * 
	 * @param packageName
	 * @return
	 * @see io.resthelper.RestHelper#getApiList(java.lang.String)
	 */
	@Override
	public List<RestApi> getApiList(String packageName) {
		return apiMap.get(packageName);
	}

	/**
	 * @return
	 * @see io.resthelper.RestHelper#getBasePackages()
	 */
	@Override
	public String[] getBasePackages() {
		return basePackages;
	}
}
