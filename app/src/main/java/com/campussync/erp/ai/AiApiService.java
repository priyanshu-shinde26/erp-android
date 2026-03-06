package com.campussync.erp.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AiApiService {

    @POST("api/ai/chat")
    Call<AiResponseDto> chat(@Body AiRequestDto request);

    @POST("api/ai/analyze")
    Call<AiResponseDto> analyzeFile(@Body AiMultimodalDto request);

    @POST("api/ai/summarize")
    Call<AiResponseDto> summarizeNote(@Body AiSummaryDto request);
}