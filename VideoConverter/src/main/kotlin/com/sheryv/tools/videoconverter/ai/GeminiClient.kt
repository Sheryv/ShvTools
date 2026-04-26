package com.sheryv.tools.videoconverter.ai

import com.google.common.util.concurrent.RateLimiter
import com.google.genai.Client
import com.google.genai.types.*
import com.sheryv.util.logging.log
import kotlinx.coroutines.delay

class GeminiClient(
  private val apiKey: String,
//  val model: String = "gemma-4-26b-a4b-it",
  val model: String = "gemini-3.1-flash-lite-preview",
  private val thinkingLevel: ThinkingLevel.Known = ThinkingLevel.Known.HIGH,
) {
  val client: Client = Client.builder()
    .httpOptions(HttpOptions.builder().timeout(120_000).retryOptions(HttpRetryOptions.builder().attempts(0).build()).build())
    .apiKey(apiKey).build()
  
  suspend fun generate(prompt: String): String {
    rateLimitWait()
    val response = client.models.generateContent(model, prompt, config().build())
    
    return streamParts(response).joinToString("")
  }
  
  suspend fun generateStream(prompt: String): Sequence<String> {
    
    rateLimitWait()
    //todo dodać ponawianie w razie błędu lub gdy zwróci niepoprawne dane, dodać timeout na 120s
    val responseStream = client.models.generateContentStream(model, prompt, config().build())
    
    return sequence {
      responseStream.use { stream ->
        for (res in stream) {
          streamParts(res).forEach { yield(it) }
        }
      }
    }
  }
  
  private fun streamParts(res: GenerateContentResponse?): Sequence<String> {
    if (res == null
      || res.candidates().isEmpty
      || res.candidates().get()[0].content().isEmpty()
      || res.candidates().get()[0].content().get().parts().isEmpty
    ) {
      return emptySequence()
    }
    return sequence {
      
      val parts = res.candidates().get()[0].content().get().parts().get()
      for (part in parts) {
        val text = part.text()
        if (text.isPresent) {
          yield(text.get())
        }
      }
    }
  }
  
  private suspend fun rateLimitWait() {
    while (!rateLimiter.tryAcquire()) {
      delay(100)
    }
  }
  
  private fun config() = GenerateContentConfig
    .builder()
    .thinkingConfig(
      ThinkingConfig
        .builder()
        .thinkingLevel(thinkingLevel)
        .build()
    )
  
  //    List<Tool> tools = new ArrayList<>();
//    tools.add(
//        Tool.builder()
//            .googleSearch(
//                GoogleSearch.builder()
//            )
//            .build());
  
  
  companion object {
    private val rateLimiter: RateLimiter = RateLimiter.create(0.25)
  }
}
