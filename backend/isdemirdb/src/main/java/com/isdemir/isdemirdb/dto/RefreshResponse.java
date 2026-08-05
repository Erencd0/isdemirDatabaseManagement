package com.isdemir.isdemirdb.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// POST /auth/refresh response: the newly produced access token.
@Getter
@AllArgsConstructor
public class RefreshResponse {

    private String accessToken;
}
