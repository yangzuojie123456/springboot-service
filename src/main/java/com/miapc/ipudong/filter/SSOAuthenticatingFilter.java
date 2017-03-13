package com.miapc.ipudong.filter;

import com.miapc.ipudong.cache.MemcacheKey;
import com.miapc.ipudong.shiro.ShiroMemCacheManager;
import com.miapc.ipudong.utils.SecurityUtil;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.miapc.ipudong.constant.Constant.USER_CENTER_TOKEN;

/**
 * Created by wangwei on 2016/11/7.
 */
@Component
public class SSOAuthenticatingFilter extends AuthenticatingFilter {
    private Map<String, Long> tokenCache = new ConcurrentHashMap<String, Long>();
    @Autowired
    private ShiroMemCacheManager memCacheManager;
    private static final Logger log = LoggerFactory.getLogger(SSOAuthenticatingFilter.class);

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        return getUsernamePasswordToken(request);
    }

    private UsernamePasswordToken getUsernamePasswordToken(ServletRequest request) {
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies)
                if (cookie.getName().equals(USER_CENTER_TOKEN)) {
                    String tokenHash = cookie.getValue();
                    UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) memCacheManager.getCache(MemcacheKey.MEMCACHE_KEY_USER_TOKEN).get(tokenHash);
                    if (usernamePasswordToken != null) {
                        //检查是否超时
                        if (tokenCache.containsKey(tokenHash)) {
                            long lastAccessTime = tokenCache.get(tokenHash);
                            //当前cache 2小时有效
                            if (System.currentTimeMillis() - lastAccessTime < 7200000) {
                                return usernamePasswordToken;
                            }
                        }
                        String token = (String) memCacheManager.getCache(MemcacheKey.MEMCACHE_KEY_USER_HASH_TOKEN).get(tokenHash);
                        if (token != null && validateToken(tokenHash, token)) {
                            return usernamePasswordToken;
                        }
                    }

                }
        }
        return null;
    }

    /**
     * 验证token
     */
    private boolean validateToken(String tokenHash, String token) {
        try {
            String tokenValue = SecurityUtil.decrypt(token);
            Pattern p = Pattern.compile("DETATIME=(.*);HOST");
            Matcher m = p.matcher(tokenValue);
            Long loginTime = 0L;
            while (m.find()) {
                loginTime = (m.group(1) != null ? Long.parseLong(m.group(1)) : loginTime);
            }
            // 3个小时内登陆过则返回true
            if (System.currentTimeMillis() - loginTime < 10800000) {
                tokenCache.put(tokenHash, System.currentTimeMillis());
                return true;
            }
        } catch (NumberFormatException e) {
            log.debug("check token failed.");
        }
        return false;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        Subject subject = getSubject(request, response);
        UsernamePasswordToken token = getUsernamePasswordToken(request);
        if (token != null) {
            subject.login(token);
        } else {
            try {
                WebUtils.toHttp(response).sendError(401);
            } catch (IOException e) {
                log.debug("return 401");
            }
        }
        return subject.isAuthenticated();
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {

        if (getUsernamePasswordToken(request) != null) {
            return true;
        }
        return false;
    }
}
