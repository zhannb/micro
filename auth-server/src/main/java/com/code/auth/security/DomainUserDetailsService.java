package com.code.auth.security;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.code.auth.entity.MicroUser;
import com.code.auth.service.MicroService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
/**
 * Created by liuliang
 * on 2019/6/18
 */
@Component
@Slf4j
public class DomainUserDetailsService implements UserDetailsService {

    @Autowired
    private MicroService userService ;



    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        MicroUser microUser = userService.getOne(new QueryWrapper<MicroUser>().eq("name", s));
        log.info("user:"+microUser.getName()+" login success!");
//        String encode = passwordEncoder.encode(microUser.getPassword());
//        microUser.setPassword(encode);
        microUser.setAuthorities(AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER"));
        return microUser;
    }


}
