package PDV.PDV.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrailingSlashRedirectFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String contextPath = request.getContextPath();
            String uri = request.getRequestURI();
            if (uri.length() > contextPath.length() + 1) {
                String path = uri.substring(contextPath.length());
                if (path.endsWith("/")) {
                    String target = path.substring(0, path.length() - 1);
                    String query = request.getQueryString();
                    if (query != null && !query.isEmpty()) {
                        target = target + "?" + query;
                    }
                    response.sendRedirect(response.encodeRedirectURL(target));
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
