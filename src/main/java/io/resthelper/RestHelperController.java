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

import io.resthelper.model.QueryParam;
import io.resthelper.model.ReqHeader;
import io.resthelper.model.RestApi;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author redstrato
 * @author bada94
 */
@Controller
public class RestHelperController {
	private static final Logger LOG = LoggerFactory.getLogger(RestHelperController.class);

	private static String RESTFUL_JS = null;

	static {
		try {
			RESTFUL_JS = IOUtils.toString(RestHelperController.class.getResourceAsStream("/restful.js"), "utf-8");
		} catch (IOException e) {
			LOG.error(e.toString(), e);
		}
	}

	@Autowired
	private RestHelperService restHelperService;

	@ExceptionHandler(NotAllowIpException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public void accessDeniedException(HttpServletRequest request, NotAllowIpException e) {
		String remoteAddr = request.getHeader("X-Real-IP");
		remoteAddr = (remoteAddr != null) ? remoteAddr : request.getRemoteAddr();
		
		LOG.warn("Access Deny Ip : {}", remoteAddr);
	}
	
	@RequestMapping(value = "/rest-helper", method = RequestMethod.GET)
	public void frame(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html; charset=utf-8");
		response.setCharacterEncoding("utf-8");
		PrintWriter out = response.getWriter();

		String requestURI = request.getRequestURI();
		String contextName = requestURI.substring(0, requestURI.indexOf("/rest-helper"));

		if (!restHelperService.isValidIp(request)) {
			throw new NotAllowIpException();
		}

		out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\">");
		out.println("<HTML>");
		out.println("<HEAD>");
		out.println("<TITLE>");
		out.println("API HELPER");
		out.println("</TITLE>");
		out.println("<SCRIPT type=\"text/javascript\">");
		out.println("    targetPage = \"\" + window.location.search;");
		out.println("    ");
		out.println("    if (targetPage != \"\" && targetPage != \"undefined\")");
		out.println("        targetPage = targetPage.substring(1);");
		out.println("    ");
		out.println("    if (targetPage.indexOf(\":\") != -1)");
		out.println("        targetPage = \"undefined\";");
		out.println("     ");
		out.println("    function loadFrames() {");
		out.println("        if (targetPage != \"\" && targetPage != \"undefined\")");
		out.println("             top.apiFrame.location = top.targetPage;");
		out.println("    }");
		out.println("</SCRIPT>");
		out.println("</HEAD>");
		out.println("<FRAMESET cols=\"25%,75%\" title=\"\" onLoad=\"top.loadFrames()\">");
		out.println("<FRAMESET rows=\"30%,70%\" title=\"\" onLoad=\"top.loadFrames()\">");
		out.println("<FRAME src=\"" + contextName
			+ "/rest-helper/packages\" name=\"packageListFrame\" title=\"all packages\">");
		out.println("<FRAME src=\"about:blank\" name=\"packageFrame\" title=\"api name list for package\">");
		out.println("</FRAMESET>");
		out.println("<FRAME src=\"about:blank\" name=\"apiFrame\" title=\"apis\" scrolling=\"yes\">");
		out.println("</FRAMESET>");
		out.println("</HTML>");
		out.flush();
	}

	@RequestMapping(value = "/rest-helper/packages", method = RequestMethod.GET)
	public void packages(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String requestURI = request.getRequestURI();
		String contextName = requestURI.substring(0, requestURI.indexOf("/rest-helper"));

		String[] packages = restHelperService.getBasePackages();

		response.setContentType("text/html; charset=utf-8");
		response.setCharacterEncoding("utf-8");
		PrintWriter out = response.getWriter();

		if (packages == null || packages.length == 0) {
			out.flush();
			return;
		}

		if (!restHelperService.isValidIp(request)) {
			throw new NotAllowIpException();
		}
		
		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("<meta charset=\"UTF-8\">");
		out.println("<title>Packages</title>");
		out.println("<style type='text/css'>");
		out.println("body {");
		out.println("font-family: monospace;");
		out.println("}");
		out.println("</style>");
		out.println("<script type=\"text/javascript\">");
		out.println("function bodyonload() {");
		out.println("	top.packageFrame.location = \"" + contextName + "/rest-helper/apis?package=" + packages[0] + "\";");
		out.println("}");
		out.println("</script>");
		out.println("</head>");
		out.println("<body onload=\"bodyonload()\">");
		out.println("<h3>Packages</h3>");
		out.println("<ul>");

		for (String basePackage : packages) {
			out.println("<li><a href=\"" + contextName + "/rest-helper/apis?package=" + basePackage
				+ "\" target=\"packageFrame\">" + basePackage + "</a></li>");
		}

		out.println("</ul>");
		out.println("</body></html>");

		out.flush();
	}

	@RequestMapping(value = "/rest-helper/apis", method = RequestMethod.GET)
	public void apis(HttpServletRequest request, HttpServletResponse response,
		@RequestParam("package") String packageName) throws IOException {
		String requestURI = request.getRequestURI();
		String contextName = requestURI.substring(0, requestURI.indexOf("/rest-helper"));

		response.setContentType("text/html; charset=utf-8");
		response.setCharacterEncoding("utf-8");
		PrintWriter out = response.getWriter();

		if (packageName == null || packageName.length() == 0) {
			out.flush();
			return;
		}
		
		if (!restHelperService.isValidIp(request)) {
			throw new NotAllowIpException();
		}

		List<RestApi> apiList = restHelperService.getApiList(packageName);

		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("<meta charset=\"UTF-8\">");
		out.println("<title>API LIST</title>");
		out.println("<style type='text/css'>");
		out.println("body {");
		out.println("font-family: monospace;");
		out.println("}");
		out.println("</style>");
		out.println("<script type=\"text/javascript\">");
		out.println("function bodyonload() {");
		out.println("	top.apiFrame.location = \"" + contextName + "/rest-helper/detail?package=" + packageName + "\";");
		out.println("}");
		out.println("</script>");
		out.println("</head>");
		out.println("<body onload=\"bodyonload()\">");
		out.println("<h3>API List</h3>");
		out.println("<ul>");

		for (RestApi api : apiList) {
			out.println("<li><a href=\"" + contextName + "/rest-helper/detail?package=" + packageName + "#"
				+ api.getApiKey() + "\" target=\"apiFrame\">" + api.getUriPattern() + " (" + api.getHttpMethod()
				+ ")</a></li>");
		}

		out.println("</ul>");
		out.println("</body></html>");

		out.flush();
	}

	@RequestMapping(value = "/rest-helper/detail", method = RequestMethod.GET)
	public void listApis(HttpServletRequest request, HttpServletResponse response,
		@RequestParam("package") String packageName) throws IOException {
		response.setContentType("text/html; charset=utf-8");
		response.setCharacterEncoding("utf-8");
		PrintWriter out = response.getWriter();

		if (packageName == null || packageName.length() == 0) {
			out.flush();
			return;
		}
		if (!restHelperService.isValidIp(request)) {
			throw new NotAllowIpException();
		}

		List<RestApi> apiList = restHelperService.getApiList(packageName);

		String requestURI = request.getRequestURI();
		String contextName = requestURI.substring(0, requestURI.indexOf("/rest-helper"));

		out.println("<!DOCTYPE html>");
		out.println("<html>");
		out.println("<head>");
		out.println("<meta charset=\"UTF-8\">");
		out.println("<title>API LIST</title>");
		out.println("<style type='text/css'>");
		out.println("body {");
		out.println("font-family: monospace;");
		out.println("}");
		out.println("ul.httpreq {");
		out.println("border: 1px solid #AAAAAA;");
		out.println("}");
		out.println("ul.httpreq li {");
		out.println("list-style-type: none;");
		out.println("}");
		out.println("ul.comment {");
		out.println("}");
		out.println("ul.comment li {");
		out.println("list-style-type: none;");
		out.println("}");
		out.println("span.success {");
		out.println("background: #55ff55; white-space: pre;");
		out.println("}");
		out.println("span.error {");
		out.println("background: #ff5555; white-space: pre;");
		out.println("}");
		out.println("span.etc {");
		out.println("background: #ffff55; white-space: pre;");
		out.println("}");
		out.println("</style>");
		out.println("<script type='text/javascript' src='./restfuljs'></script>");
		out.println("</head>");
		out.println("<body>");
		out.println("");
		out.println("<ul class=\"apiList\">");

		int apiIndex = 0;

		for (RestApi webApi : apiList) {
			out.print("<li>" + "<a name=\"" + webApi.getApiKey() + "\"></a><b>");
			
			if (webApi.isDeprecated()) {
				out.print("<font color='red'>[Deprecated]</font> ");
			}
			
			out.println(webApi.getHttpMethod() + " "
				+ contextName + webApi.getUriPattern() + " - " + webApi.getApiName() + "</b><br/> "
				+ webApi.getMethodName());
			out.println("<ul class='httpreq'>");

			out.print("<li>");
			
			
			out.print(webApi.getHttpMethod() + " ");

			StringBuilder uriBuilder = new StringBuilder();

			String uri = contextName + webApi.getUriPattern();
			int uriParamIndex = 0;
			int openBrace = uri.indexOf('{');
			int closeBrace = -1;

			if (openBrace < 0) {
				out.print(uri);
				uriBuilder.append(uri);
			}

			while (openBrace > 0) {
				out.print(uri.substring(closeBrace + 1, openBrace));
				uriBuilder.append(uri.substring(closeBrace + 1, openBrace));

				out.print("<input type='text' id='r" + apiIndex + uriParamIndex + "' />");
				uriBuilder.append("\" + $(\"r" + apiIndex + uriParamIndex + "\").value + \"");

				closeBrace = uri.indexOf('}', openBrace);

				assert closeBrace > openBrace;

				openBrace = uri.indexOf('{', closeBrace);
				uriParamIndex++;

				if (openBrace < 0 && uri.length() > (closeBrace + 1)) {
					out.print(uri.substring(closeBrace + 1));
					uriBuilder.append(uri.substring(closeBrace + 1));
					break;
				}
			}

			QueryParam[] queryParams = webApi.getQueryParams();

			if (queryParams != null && queryParams.length > 0) {
				out.print("?");
				uriBuilder.append("\" + \"?");

				int queryIndex = 0;

				for (QueryParam queryParam : queryParams) {
					if (queryIndex > 0) {
						out.print("&amp;");
						uriBuilder.append("+ \"&");
					}

					out.print(queryParam.getName());
					uriBuilder.append(queryParam.getName());

					if (queryParam.isRequired()) {
						out.print("(*)");
					}

					out.print("=<input type='text' id='q" + apiIndex + queryIndex + "' value='"
						+ queryParam.getDevaultValue() + "'/>");

					uriBuilder.append("=\" + encodeURIComponent($(\"q" + apiIndex + queryIndex + "\").value) ");

					queryIndex++;
				}
			} else {
				uriBuilder.append("\"");
			}

			out.println("</li>");

			String headers = null;
			StringBuilder headersBuilder = null;
			String[] matchingHeaders = webApi.getMatchingHeaders();
			
			// TODO 추가적인 헤더 직접 입력할 수 있는 textarea 추가 및 요청에 반영.

			if (matchingHeaders != null) {
				headersBuilder = new StringBuilder();
				headersBuilder.append("{");
				
				for (String matchingHeader : matchingHeaders) {
					// header분리 출력 및 요청 헤더에 추가하기
					int firstEqualIndex = matchingHeader.indexOf('=');
					
					if (firstEqualIndex > 0) {
						String key = matchingHeader.substring(0, firstEqualIndex);
						String value = matchingHeader.substring(firstEqualIndex + 1);
						
						out.println("<li>" + key +": " + value + "</li>");
						
						headersBuilder.append("\"" + key + "\" : \"" + value + "\", ");
					}
				}
			}

			ReqHeader[] reqHeaders = webApi.getRequestHeaders();

			if (reqHeaders != null) {
				if (headersBuilder == null) {
					headersBuilder = new StringBuilder();
					headersBuilder.append("{");
				}

				int headerIndex = 0;
				for (ReqHeader reqHeader : reqHeaders) {
					out.println("<li>" + reqHeader.getName() + ": <input type='text' id='h" + apiIndex + headerIndex
						+ "' value='" + reqHeader.getDevaultValue() + "'/>");
					headersBuilder.append("\"" + reqHeader.getName() + "\" : $(\"h" + apiIndex + headerIndex
						+ "\").value, ");
					
					headerIndex++;
				}

				headersBuilder.append("}");
			}

			if (headersBuilder != null) {
				headers = headersBuilder.toString();
			}
			
			if ("POST".equals(webApi.getHttpMethod()) || "PUT".equals(webApi.getHttpMethod())) {
				out.println("<li><textarea id='c" + apiIndex + "' cols='80' rows='3' ></textarea></li>");
			}

			out.print("<li><input type='button' value='submit' ");
			out.print(" onclick='restful_" + webApi.getHttpMethod().toLowerCase());

			if ("POST".equals(webApi.getHttpMethod()) || "PUT".equals(webApi.getHttpMethod())) {
				out.println("(\"" + uriBuilder.toString() + ", " + headers + ", $(\"c" + apiIndex + "\").value, cb_"
					+ apiIndex + ")' />");
			} else {
				out.println("(\"" + uriBuilder.toString() + ", " + headers + ", cb_" + apiIndex + ")' />");
			}

			out.println("<div style='display: inline' id='s" + apiIndex + "'></div></li>");

			out.println("<li><textarea id='t" + apiIndex
				+ "' rows='5' cols='80' style='display:none' readonly></textarea>");
			out.println("<script type='text/javascript'>");
			out.println("function cb_" + apiIndex + "(result) {");
			out.println("	if (result.status >= 200 && result.status < 300) {");
			out.println("		$('s"
				+ apiIndex
				+ "').innerHTML = '<span class=\"success\">' + result.status + ' - ' + result.statusText + '</span>';");
			out.println("	} else if (result.status >= 400) {");
			out.println("		$('s" + apiIndex
				+ "').innerHTML = '<span class=\"error\">' + result.status + ' - ' + result.statusText + '</span>';");
			out.println("	} else {");
			out.println("		$('s" + apiIndex
				+ "').innerHTML = '<span class=\"etc\">' + result.status + ' - ' + result.statusText + '</span>';");
			out.println("	}");
			out.println("	if (result.responseText) {");
			out.println("		$('t" + apiIndex + "').style.display = 'inline';");
			out.println("		var json = JSON.stringify(JSON.parse(result.responseText), null, 3);");
			out.println("		if (navigator.userAgent.match(/Chrome/i))");
			out.println("			$('t" + apiIndex + "').innerHTML = json;");		// innerText로 할 경우 chrome에서 \n을 먹어 버림 
			out.println("		else");
			out.println("			$('t" + apiIndex + "').innerText = json;");		// Chrome 외에는 innerText 로
			out.println("	}");
			out.println("}");
			out.println("</script></li>");

			out.println("<li>");
			out.println("<ul class='comment'>");
			out.println("<li>" + webApi.getDescription() + "</li>");
			out.println("</ul>");
			out.println("</li>");

			out.println("</ul>");
			out.println("</li><br/>");

			apiIndex++;
		}

		out.println("</ul>");
		out.println("</body></html>");
		out.flush();
	}
	
	@RequestMapping(value = "/rest-helper/restfuljs", method = RequestMethod.GET)
	public void restfuljs(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!restHelperService.isValidIp(request)) {
			throw new NotAllowIpException();
		}
		
		response.setContentType("text/javascript; charset=utf-8");
		response.setCharacterEncoding("utf-8");
		PrintWriter out = response.getWriter();
		out.print(RESTFUL_JS);
		out.flush();
	}
}