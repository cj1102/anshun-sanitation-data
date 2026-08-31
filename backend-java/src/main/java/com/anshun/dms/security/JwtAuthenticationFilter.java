package com.anshun.dms.security;

import com.anshun.dms.common.BusinessException;
import com.anshun.dms.service.UserTokenStateService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final ObjectProvider<TokenBlacklistService> tokenBlacklistService;
    private final UserTokenStateService userTokenStateService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   ObjectProvider<TokenBlacklistService> tokenBlacklistService,
                                   UserTokenStateService userTokenStateService) {
        this.jwtTokenService = jwtTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userTokenStateService = userTokenStateService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String token = header.substring(7);
                TokenBlacklistService blacklist = tokenBlacklistService.getIfAvailable();
                if (blacklist != null && blacklist.isRevoked(token)) {
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }
                Claims claims = jwtTokenService.parse(token);
                String username = claims.get("username", String.class);
                Object userIdValue = claims.get("userId");
                Object versionValue = claims.get("tokenVersion");
                Number userIdClaim = userIdValue instanceof Number number ? number : null;
                Number versionClaim = versionValue instanceof Number number ? number : null;
                if (username == null || userIdClaim == null || versionClaim == null
                        || !userTokenStateService.isCurrent(userIdClaim.longValue(), username, versionClaim.intValue())) {
                    SecurityContextHolder.clearContext();
                    chain.doFilter(request, response);
                    return;
                }
                List<String> roles = claimList(claims, "roles");
                if (roles.isEmpty() && claims.get("role", String.class) != null) roles = List.of(claims.get("role", String.class));
                List<String> permissions = claimList(claims, "permissions");
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (BusinessException exception) {
                SecurityContextHolder.clearContext();
                response.setStatus(exception.status().value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":" + exception.status().value()
                        + ",\"message\":\"认证服务暂时不可用，请稍后重试\",\"data\":null}");
                return;
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private List<String> claimList(Claims claims, String name) {
        Object claim = claims.get(name);
        if (!(claim instanceof List<?> values)) return List.of();
        return values.stream().map(String::valueOf).toList();
    }
}
