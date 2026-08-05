package com.isdemir.isdemirdb.security;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Which converters (kv1/kv2/kv3) the token holder may touch. The roles sit in the JWT as
// authorities (see JwtFilter).
//   - READ paths filter with allowed(), so another converter's heat is simply not listed.
//   - WRITE paths call require(), so a kv3 user cannot post to a kv1 heat (-> 403).
// Both live here so a new endpoint has one obvious place to ask.
@Component
public class ConverterAccess {

    // No authentication or no kv role -> empty list, so nothing is allowed instead of everything.
    public List<Integer> allowed() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }
        return convertersOf(authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
    }

    // Guard for every write. AccessDeniedException -> 403 (see GlobalExceptionHandler).
    public void require(Integer converterNo) {
        if (converterNo == null || !allowed().contains(converterNo)) {
            throw new AccessDeniedException(
                    "Bu konverter için yetkiniz yok (KV" + converterNo + ").");
        }
    }

    // dokum_no -> converter: the 2nd digit (6[3]00001 -> 3)
    public static int converterOfHeatNo(int heatNo) {
        return (heatNo / 100_000) % 10;
    }

    // "kv1", "kv3" -> 1, 3. Roles that are not converter roles are dropped.
    public static List<Integer> convertersOf(List<String> roles) {
        return roles.stream()
                .filter(role -> role.matches("kv\\d+"))
                .map(role -> Integer.valueOf(role.substring(2)))
                .toList();
    }
}
