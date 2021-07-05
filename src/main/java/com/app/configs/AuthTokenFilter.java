package com.app.configs;

import com.app.common.JwtUtils;
import com.app.services.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
//lọc đầu vào khi gửi request kèm token để hỗ trợ xác thực
public class AuthTokenFilter extends OncePerRequestFilter {
    private static  final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            //lấy mã token từ header của request
            String token = getJwt(request);
            //kiểm tra token khác null và thoả mãn validate không
            if(token !=null &&jwtUtils.validateJwtToken(token)){
                //lấy username từ mã token
                String username = jwtUtils.getUserNameFormJwtToken(token);
                //khởi tại userdetail từ tên đăng nhập
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                //tạo 1 token cơ bản từ userDetails và danh sách quyền
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                //buid 1 thông tin chi tiết từ request
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // trả về cho security một token đã qua các bước xác thực
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception e){
            logger.error("Can't set user authentication -> Message: {}",e);
        }
        //được lọc lại lần cuối, chưa hiểu nhiều
        filterChain.doFilter(request,response);
    }

    //lấy một jwt từ request
    private String getJwt(HttpServletRequest request){
        String authHeader = request.getHeader("Authorization");
        //kiểm tra chuỗi trả về khác null bắt đầu từ Bearer;
        if(authHeader !=null && authHeader.startsWith("Bearer")){
            //sau đó đổi chuỗi lại thay Bearer thành khoảng trắng và trả về header thực
            return authHeader.replace("Bearer", "");
        }
        return null;
    }


}
