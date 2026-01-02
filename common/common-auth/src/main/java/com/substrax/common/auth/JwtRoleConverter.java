package com.substrax.common.auth;


import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

import static com.substrax.common.auth.SecurityConstants.*;

public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        Set<String> roles = new HashSet<>();

        // 1. Extract realm roles
        Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get(RESOURCE_ACCESS);

        if(realmAccess != null && realmAccess.containsKey(ROLES))
        {
            roles.addAll((Collection<String>) realmAccess.get(ROLES));
        }

        // 2. Extract resource (client) roles
        Map<String, Object> resourceAccess = ( (Map<String, Object>) jwt.getClaims().get(RESOURCE_ACCESS));

        if(resourceAccess != null)
        {
            for(Object value: resourceAccess.values()){
                if(value instanceof Map<?,?> clientRoles)
                {
                    Object rolesObj = clientRoles.get(ROLES);

                    if(rolesObj instanceof Collection<?>){
                        for(Object role: (Collection<?>) rolesObj){
                            roles.add(role.toString());
                        }
                    }
                }
            }
        }

        // 3. Convert to Spring Security authorities
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .collect(Collectors.toSet());
    }
}

