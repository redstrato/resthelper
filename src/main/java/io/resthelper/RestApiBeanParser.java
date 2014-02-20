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

		Class<?> clazz = Class.forName(bean.getBeanClassName());

		if (clazz.getAnnotation(Controller.class) == null) {
			return webApiList;
		}

		String baseURIPath = null;
		RequestMapping classRequestMapping = clazz.getAnnotation(RequestMapping.class);

		if (classRequestMapping != null) {
			// XXX Restriction: only one base uri allowed;
			String[] classURIPaths = classRequestMapping.value(); // uri path
			baseURIPath = classURIPaths[0];

			// XXX Restriction: three fields are not used;
			RequestMethod[] classMethods = classRequestMapping.method();
			String[] classHeaders = classRequestMapping.headers();
			String[] classParams = classRequestMapping.params();
		}

		Method[] methods = clazz.getMethods();

		for (Method method : methods) {
			RestApi webApi = new RestApi();
			RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);

			// XXX @ResponseBody
			if (method.getAnnotation(ResponseBody.class) == null || requestMapping == null) {
				continue;
			}
			
			if (method.getAnnotation(Deprecated.class) != null) {
				webApi.setDeprecated(true);
			}
			
			// uri
			String[] uriPatterns = requestMapping.value();
			
			// XXX Restriction: for now only first uri will be used
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

			// XXX Restriction: for now only first method will be used
			if (requestMethods != null && requestMethods.length > 0) {
				webApi.setHttpMethod(requestMethods[0].toString());
			} else {
				// XXX default; GET
				webApi.setHttpMethod(RequestMethod.GET.toString());
			}

			// matching params
			String[] matchingParams = requestMapping.params();
			webApi.setMatchingParams(matchingParams);

			// matching headers
			String[] matchingHeaders = requestMapping.headers();
			webApi.setMatchingHeaders(matchingHeaders);

			// parameter
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

			// @wooroo
			// shorten full class path
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
