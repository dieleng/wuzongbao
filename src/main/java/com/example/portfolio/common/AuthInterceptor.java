package com.example.portfolio.common;

import com.example.portfolio.config.WebMvcConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台鉴权:未登录时,页面请求跳转登录页,API 请求返回 401 JSON
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object admin = request.getSession().getAttribute(WebMvcConfig.SESSION_KEY);
        if (admin != null) {
            return true;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin/api/")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"ok\":false,\"message\":\"未登录或登录已过期\"}");
        } else {
            response.sendRedirect("/admin/login");
        }
        return false;
    }
}
