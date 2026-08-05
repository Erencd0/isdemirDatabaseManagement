package com.isdemir.isdemirdb.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// POST /auth/refresh request body: { "refreshToken": "..." }
@Getter
@Setter
@NoArgsConstructor
public class RefreshRequest {

    private String refreshToken;
}
