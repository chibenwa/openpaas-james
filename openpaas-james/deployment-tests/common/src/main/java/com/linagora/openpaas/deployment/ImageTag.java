package com.linagora.openpaas.deployment;

import java.util.Optional;

public class ImageTag {
    public static String retrieve() {
        return Optional.ofNullable(System.getenv("IMAGE_TAG"))
            .orElse("latest");
    }
}
