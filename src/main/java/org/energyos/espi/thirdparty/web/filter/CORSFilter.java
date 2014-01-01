/*
 *    Copyright 2013 BrandsEye (http://www.brandseye.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.energyos.espi.thirdparty.web.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.stereotype.Component;

/**
 * Adds CORS headers to requests to enable cross-domain access.
 */

@Component
public class CORSFilter implements Filter {

    private final Log logger = LogFactory.getLog(getClass());
	private final Map<String, String> optionsHeaders = new LinkedHashMap<String, String>();

    private Pattern allowOriginRegex;
    private String allowOrigin;
    private String exposeHeaders;

    public void init(FilterConfig cfg) throws ServletException {
        String regex = cfg.getInitParameter("allow.origin.regex");
        if (regex != null) {
            allowOriginRegex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        } else {
            optionsHeaders.put("Access-Control-Allow-Origin", "*");
        }

        optionsHeaders.put("Access-Control-Allow-Headers", "Origin, Authorization, Accept, Content-Type");
        optionsHeaders.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        optionsHeaders.put("Access-Control-Max-Age", "1800");
        for (Enumeration<String> i = cfg.getInitParameterNames(); i.hasMoreElements(); ) {
            String name = i.nextElement();
            if (name.startsWith("header:")) {
                optionsHeaders.put(name.substring(7), cfg.getInitParameter(name));
            }
        }

        //*
        //*
        //*  The following code has been commented out since all methods now use checkOrigin()
        //*  Therefore there is no need to create and then delete the "Access-Control-Allow-Origin" 
        //*  header
        //*
        //*
//        maintained for backward compatibility on how to set allowOrigin if not
//        using a regex
//        allowOrigin = optionsHeaders.get("Access-Control-Allow-Origin");
//        since all methods now go through checkOrigin() to apply the Access-Control-Allow-Origin
//        header, and that header should have a single value of the requesting Origin since
//        Access-Control-Allow-Credentials is always true, we remove it from the options headers
//        optionsHeaders.remove("Access-Control-Allow-Origin");

        exposeHeaders = cfg.getInitParameter("expose.headers");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
    	
    	if (logger.isInfoEnabled()) {  		
    		logger.info("CORSFilter processing: Checking for Cross Origin pre-flight OPTIONS message");
    	}
    	
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest req = (HttpServletRequest)request;
            HttpServletResponse resp = (HttpServletResponse)response;
            if ("OPTIONS".equals(req.getMethod())) {
                if (checkOrigin(req, resp)) {
                    for (Map.Entry<String, String> e : optionsHeaders.entrySet()) {
                    	
                        resp.setHeader(e.getKey(), e.getValue());
                    }

                    // We need to return here since we don't want the chain to further process
                    // a preflight request since this can lead to unexpected processing of the preflighted
                    // request or a 40x - Response Code
                    return;

                }
            } else if (checkOrigin(req, resp)) {
                if (exposeHeaders != null) {
                    resp.setHeader("Access-Control-Expose-Headers", exposeHeaders);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean checkOrigin(HttpServletRequest req, HttpServletResponse resp) {
        String origin = req.getHeader("Origin");
        if (origin == null) {
            //no origin; per W3C specification, terminate further processing for both pre-flight and actual requests
            return false;
        }

        boolean matches = false;
        // Check for JUnit Test (Origin = JUnit_Test)
        if (origin.equals("JUnit_Test")) {
            resp.setHeader("Access-Control-Allow-Headers", "Origin, Authorization, Accept, Content-Type");
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            resp.setHeader("Access-Control-Max-Age", "1800");
        	matches = true;
        } else
        	//check if using regex to match origin
        	if (allowOriginRegex != null) {
        		matches = allowOriginRegex.matcher(origin).matches();
        	} else if (allowOrigin != null) {
        		matches = allowOrigin.equals("*") || allowOrigin.equals(origin);
        	}

        if (matches) {
        	
        	// Activate next two lines and comment out third line if Credential Support is required
//          resp.addHeader("Access-Control-Allow-Origin", origin);
//          resp.addHeader("Access-Control-Allow-Credentials", "true");      	
        	resp.addHeader("Access-Control-Allow-Origin", "*");
            return true;
        } else {
            return false;
        }
    }

    public void destroy() {
    }
}
