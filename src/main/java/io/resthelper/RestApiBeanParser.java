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

import io.resthelper.annotations.ApiDescription;
import io.resthelper.annotations.ApiName;
import io.resthelper.model.CookieVal;
import io.resthelper.model.QueryParam;
import io.resthelper.model.ReqHeader;
import io.resthelper.model.RestApi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ValueConstants;

/**
 * Spring MVC REST Controller Parser
 * 
 * @author redstrato
 * @author wooroo
 */
class RestApiBeanParser {
	private static final int LANGUAGE_MODIFIERS = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE | Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED | Modifier.NATIVE;
	
	public List<RestApi> parseBeanDefinition(BeanDefinition bean) throws ClassNotFoundException {
		List<RestApi> webApiList = new ArrayList<RestApi>();

		// FIXME 적용된 annotation을 알기 위해 class load하게 되면 원래 bean class의 로드 타이밍에 맞지
		// 않을 수 있는데 상관 없을까?
		Class<?> clazz = Class.forName(bean.getBeanClassName());

		// Controller 애노테이션 적용 여부 확인? 할필요 있나? Controller 아니고 다른 것일 수 있으므로
		// FIXME Controller 만 검색하도록 filter 넣을 수 있음. 
		if (clazz.getAnnotation(Controller.class) == null) {
			return webApiList;
		}

		// class에 RequestMapping 적용 여부 확인. 
		// TODO api 설명 애노테이션은 별도로 추가해야 할까?
		String baseURIPath = null;
		RequestMapping classRequestMapping = clazz.getAnnotation(RequestMapping.class);

		if (classRequestMapping != null) {
			// XXX base uri도 하나만 정하는 것으로 하자. (제약사항)
			String[] classURIPaths = classRequestMapping.value(); // uri path
			baseURIPath = classURIPaths[0];

			// XXX 아래 세 가지 인자는 주지 않는 것으로 정하자. (제약사항)
			RequestMethod[] classMethods = classRequestMapping.method();
			String[] classHeaders = classRequestMapping.headers();
			String[] classParams = classRequestMapping.params();
		}

		Method[] methods = clazz.getMethods();

		for (Method method : methods) {
			RestApi webApi = new RestApi();
			RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);

			// XXX @ResponseBody 적용되지 않았으면 Pass
			if (method.getAnnotation(ResponseBody.class) == null || requestMapping == null) {
				continue;
			}
			
			if (method.getAnnotation(Deprecated.class) != null) {
				webApi.setDeprecated(true);
			}
			
			// uri
			String[] uriPatterns = requestMapping.value();
			
			// FIXME URI 하나만 mandatory로 지정하기? 하나 이상의 URI를 매핑하는 경우 지원 여부?
			if (uriPatterns != null && uriPatterns.length > 0) {
				if (baseURIPath != null) {
					if (uriPatterns[0].startsWith("/")) {
						webApi.setUriPattern(baseURIPath + uriPatterns[0]);
					} else {
						webApi.setUriPattern(baseURIPath + "/" + uriPatterns[0]);
					}
				} else {
					webApi.setUriPattern(uriPatterns[0]);
				}
			} else {
				webApi.setUriPattern(baseURIPath);
			}
			
			// api name, description
			ApiName apiName = method.getAnnotation(ApiName.class);
			
			if (apiName != null) {
				webApi.setApiName(apiName.value());
			}
			
			ApiDescription apiDescription = method.getAnnotation(ApiDescription.class);
			
			if (apiDescription != null && StringUtils.hasText(apiDescription.value())) {
				if (StringUtils.startsWithIgnoreCase(apiDescription.value(), "<pre>") || StringUtils.endsWithIgnoreCase(apiDescription.value(), "</pre>")) {
					webApi.setDescription(apiDescription.value());
				} else {
					webApi.setDescription("<pre>" + apiDescription.value() + "</pre>");
				}
			}

			// http method
			RequestMethod[] requestMethods = requestMapping.method();

			// XXX RequestMethod도 하나만 mandatory로 지정하기.
			if (requestMethods != null && requestMethods.length > 0) {
				webApi.setHttpMethod(requestMethods[0].toString());
			} else {
				// XXX 일단 없으면 GET으로.
				webApi.setHttpMethod(RequestMethod.GET.toString());
			}
			

			// matching params
			String[] matchingParams = requestMapping.params();
			webApi.setMatchingParams(matchingParams);

			// matching headers
			String[] matchingHeaders = requestMapping.headers();
			webApi.setMatchingHeaders(matchingHeaders);

			// parameter별 annotation 확인. PathVariable, RequestParam, CookieValue, RequestHeader, RequestBody
			Class<?>[] parameterTypes = method.getParameterTypes();
			Annotation[][] parameterAnnotations = method.getParameterAnnotations();
			int parameterLength = parameterTypes.length;

			List<String> pathVariableTypes = new ArrayList<String>();
			List<QueryParam> queryParams = new ArrayList<QueryParam>();
			List<ReqHeader> reqHeaders = new ArrayList<ReqHeader>();
			List<CookieVal> cookieValues = new ArrayList<CookieVal>();
			Class<?> requestBodyType = null;

			for (int i = 0; i < parameterLength; i++) {
				Annotation[] annotations = parameterAnnotations[i];

				for (Annotation annotation : annotations) {
					if (annotation.annotationType().equals(PathVariable.class)) {
						// path variable
						pathVariableTypes.add(parameterTypes[i].getName());
					} else if (annotation.annotationType().equals(RequestParam.class)) {
						// req parameters
						RequestParam requestParam = (RequestParam) annotation;
						QueryParam queryParam = new QueryParam();
						queryParam.setName(requestParam.value());
						queryParam.setRequired(requestParam.required());
						
						if (!ValueConstants.DEFAULT_NONE.equals(requestParam.defaultValue())) {
							queryParam.setDevaultValue(requestParam.defaultValue());
						}
						
						queryParams.add(queryParam);
					} else if (annotation.annotationType().equals(RequestHeader.class)) {
						// req headers
						RequestHeader requestHeader = (RequestHeader) annotation;
						ReqHeader reqHeader = new ReqHeader();
						reqHeader.setName(requestHeader.value());
						reqHeader.setRequired(requestHeader.required());
						
						if (!ValueConstants.DEFAULT_NONE.equals(requestHeader.defaultValue())) {
							reqHeader.setDevaultValue(requestHeader.defaultValue());
						}
						
						reqHeaders.add(reqHeader);
					} else if (annotation.annotationType().equals(RequestBody.class)) {
						// req body
						// XXX RequestBody는 하나만 있다고 가정
						requestBodyType = parameterTypes[i];
					} else if (annotation.annotationType().equals(CookieValue.class)) {
						// cookie value
						CookieValue cookieValue = (CookieValue) annotation;
						CookieVal cookieVal = new CookieVal();
						cookieVal.setName(cookieValue.value());
						cookieVal.setRequired(cookieValue.required());
						
						if (!ValueConstants.DEFAULT_NONE.equals(cookieValue.defaultValue())) {
							cookieVal.setDevaultValue(cookieValue.defaultValue());
						}
						
						cookieValues.add(cookieVal);
					}
				}
			}

			// 주수현
			// 전체 패키지 명이 다 보여 확인하기 어려워서 수정 (Class 이름만 보이도록)
			webApi.setMethodName(getMethodName(method));
			//webApi.setMethodName(method.toGenericString());
			webApi.setPathVariableTypes(pathVariableTypes.toArray(new String[pathVariableTypes.size()]));
			webApi.setQueryParams(queryParams.toArray(new QueryParam[queryParams.size()]));
			webApi.setRequestHeaders(reqHeaders.toArray(new ReqHeader[reqHeaders.size()]));
			webApi.setCookieValues(cookieValues.toArray(new CookieVal[cookieValues.size()]));
			webApi.setRequestBodyType(requestBodyType);
			webApi.setResponseBodyType(method.getReturnType());

			webApiList.add(webApi);
		}

		return webApiList;
	}
	
	private String getMethodName(Method method) {
		try {
			StringBuffer sb = new StringBuffer();
			int mod = method.getModifiers() & LANGUAGE_MODIFIERS;
			if (mod != 0) {
				sb.append(Modifier.toString(mod) + " ");
			}
			sb.append(method.getReturnType().getSimpleName() + " ");
			sb.append(method.getDeclaringClass().getSimpleName() + ".");
			sb.append(method.getName() + "(");
			Class[] params = method.getParameterTypes(); // avoid clone
			for (int j = 0; j < params.length; j++) {
				sb.append(params[j].getSimpleName());
				if (j < (params.length - 1)) {
					sb.append(", ");
				}
			}
			sb.append(")");
			Class[] exceptions = method.getExceptionTypes(); // avoid clone
			if (exceptions.length > 0) {
				sb.append(" throws ");
				for (int k = 0; k < exceptions.length; k++) {
					sb.append(exceptions[k].getSimpleName());
					if (k < (exceptions.length - 1)) {
						sb.append(", ");
					}
				}
			}
			return sb.toString();
		} catch (Exception e) {
			return "<" + e + ">";
		}
	}
}
