package com.tech.challenge.application.user.port.out;

import com.tech.challenge.application.user.result.AccessTokenResult;
import com.tech.challenge.domain.user.model.User;

public interface TokenPort {

    AccessTokenResult issue(User user);
}
