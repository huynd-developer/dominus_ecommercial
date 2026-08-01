package org.example.datn_sd69.modules.review.dto.response;

public record ReviewMediaResponse(
        Integer mediaId,
        String url,
        String mediaUrl,
        String mediaType,
        Boolean isVideo
) {
}