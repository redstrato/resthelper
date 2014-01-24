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
package io.resthelper.model;

/**
 * uri, method, header, parameter 조합의 하나의 web api
 * RequestMapping에서 uri와 method가 복수개로 세팅된 경우 그 경우 수 만큼 WebApi로 쪼갬
 * 
 * @author redstrato
 */
public class RestApi {
	private String apiKey = "";
	
	private String methodName = "";
	private String apiName = "";
	private String description = ""; // api description and return value description

	// Request Line and Header Matching
	private String uriPattern = "";
	private String httpMethod = "";
	private String[] matchingHeaders = new String[] {}; // header=value or header!=value pairs
	private String[] matchingParams = new String[] {}; // key=value or key!=value pairs

	// Request Headers
	private ReqHeader[] requestHeaders = new ReqHeader[] {};

	// Cookies
	private CookieVal[] cookieValues = new CookieVal[] {};

	// Request Parameters
	private String[] pathVariableTypes = new String[] {};
	private QueryParam[] queryParams = new QueryParam[] {};
	// XXX RequestBody는 일단 POST, PUT인 경우 XML, JSON 입력창을 넣을 수 있도록.
	private Class<?> requestBodyType;

	// Response Body (Type)
	// POJO, Map, List가 아닌 String 또는 primitive type인 경우는 JSON이 아닌 타입 값의 String형태가 그대로 body로 내려감.
	private Class<?> responseBodyType;
	
	private boolean deprecated;
	

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getApiName() {
		return apiName;
	}

	public void setApiName(String apiName) {
		this.apiName = apiName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUriPattern() {
		return uriPattern;
	}

	public void setUriPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public String[] getMatchingHeaders() {
		return matchingHeaders;
	}

	public void setMatchingHeaders(String[] matchingHeaders) {
		this.matchingHeaders = matchingHeaders;
	}

	public String[] getMatchingParams() {
		return matchingParams;
	}

	public void setMatchingParams(String[] matchingParams) {
		this.matchingParams = matchingParams;
	}

	public ReqHeader[] getRequestHeaders() {
		return requestHeaders;
	}

	public void setRequestHeaders(ReqHeader[] requestHeaders) {
		this.requestHeaders = requestHeaders;
	}

	public CookieVal[] getCookieValues() {
		return cookieValues;
	}

	public void setCookieValues(CookieVal[] cookieValues) {
		this.cookieValues = cookieValues;
	}

	public String[] getPathVariableTypes() {
		return pathVariableTypes;
	}

	public void setPathVariableTypes(String[] pathVariableTypes) {
		this.pathVariableTypes = pathVariableTypes;
	}

	public QueryParam[] getQueryParams() {
		return queryParams;
	}

	public void setQueryParams(QueryParam[] queryParams) {
		this.queryParams = queryParams;
	}

	public Class<?> getRequestBodyType() {
		return requestBodyType;
	}

	public void setRequestBodyType(Class<?> requestBodyType) {
		this.requestBodyType = requestBodyType;
	}

	public Class<?> getResponseBodyType() {
		return responseBodyType;
	}

	public void setResponseBodyType(Class<?> responseBodyType) {
		this.responseBodyType = responseBodyType;
	}
	
	public boolean isDeprecated() {
		return deprecated;
	}
	
	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}

}
